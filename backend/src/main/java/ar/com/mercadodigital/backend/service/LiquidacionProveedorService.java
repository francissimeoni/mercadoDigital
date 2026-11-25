package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.dto.response.LiquidacionPartesResponse;
import ar.com.mercadodigital.backend.dto.response.LiquidacionProveedorResponse;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.LiquidacionProveedorEstado;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.SplitPedidoItem;
import ar.com.mercadodigital.backend.entity.SplitPayoutEjecucion;
import ar.com.mercadodigital.backend.entity.SplitPayoutEstado;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.SplitPayoutEjecucionRepository;
import ar.com.mercadodigital.backend.repository.SplitPedidoItemRepository;
import ar.com.mercadodigital.backend.service.split.SplitPayoutGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LiquidacionProveedorService {
    private final MercadoPagoClient mercadoPagoClient;

    private final SplitPedidoItemRepository splitPedidoItemRepository;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final SplitPayoutGateway splitPayoutGateway;
    private final SplitPayoutEjecucionRepository splitPayoutEjecucionRepository;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final MercadoPagoMdConfigService mercadoPagoMdConfigService;

    @Transactional(readOnly = true)
    public LiquidacionProveedorResponse getByPedidoId(UUID pedidoId) {
        List<SplitPedidoItem> splits = getSplits(pedidoId);
        return toResponse(pedidoId, splits);
    }

    @Transactional(readOnly = true)
    public LiquidacionPartesResponse getPartesByPedidoId(UUID pedidoId) {
        List<SplitPedidoItem> splits = getSplits(pedidoId);
        return toPartesResponse(pedidoId, splits);
    }

    @Transactional
    public LiquidacionProveedorResponse liberarAutomaticamentePorRetiro(UUID pedidoId) {
        return liberarAutomaticamente(pedidoId, "RETIRO");
    }

    private LiquidacionProveedorResponse liberarAutomaticamente(UUID pedidoId, String origen) {
        List<SplitPedidoItem> splits = getSplits(pedidoId);
        SplitPedidoItem referencia = splits.get(0);

        validarPedidoPagado(referencia);
        if (estaLiberado(referencia)) {
            return toResponse(pedidoId, splits);
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (SplitPedidoItem split : splits) {
            split.setEstadoLiquidacionProveedor(LiquidacionProveedorEstado.LIBERADO_AUTOMATICO);
            split.setLiberadoProveedorAt(ahora);
            split.setMotivoLiberacionManual(null);
        }
        splitPedidoItemRepository.saveAll(splits);
        registrarAuditoria(
                "PROVEEDOR_LIBERADO_AUTOMATICO",
                pedidoId,
                "{\"pedidoId\":\"" + pedidoId + "\",\"origen\":\"" + escapeJson(origen) + "\"}");
        SplitPayoutGateway.TotalesSplit totalesSplit = buildTotalesSplit(splits);
        registrarAuditoria("SPLIT_LIBERADO_PARTES_RETIRADO", pedidoId, buildPayloadLiquidacionPartes(pedidoId, totalesSplit, origen));
        intentarPayoutReal(pedidoId, splits, referencia, totalesSplit);
        return toResponse(pedidoId, splits);
    }

    @Transactional
    public LiquidacionProveedorResponse liberarManual(UUID pedidoId, String motivo) {
        if (!StringUtils.hasText(motivo)) {
            throw new BadRequestException("motivo obligatorio para liberar proveedor manualmente");
        }

        List<SplitPedidoItem> splits = getSplits(pedidoId);
        SplitPedidoItem referencia = splits.get(0);

        validarPedidoPagado(referencia);
        if (estaLiberado(referencia)) {
            throw new OperacionEnConflictoException("El proveedor ya fue liberado para este pedido.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        String motivoNormalizado = motivo.trim();
        for (SplitPedidoItem split : splits) {
            split.setEstadoLiquidacionProveedor(LiquidacionProveedorEstado.LIBERADO_MANUAL);
            split.setLiberadoProveedorAt(ahora);
            split.setMotivoLiberacionManual(motivoNormalizado);
        }
        splitPedidoItemRepository.saveAll(splits);
        registrarAuditoria(
                "PROVEEDOR_LIBERADO_MANUAL",
                pedidoId,
                "{\"pedidoId\":\"" + pedidoId + "\",\"motivo\":\"" + escapeJson(motivoNormalizado) + "\"}");
        return toResponse(pedidoId, splits);
    }

    private List<SplitPedidoItem> getSplits(UUID pedidoId) {
        List<SplitPedidoItem> splits = splitPedidoItemRepository.findByPedidoItem_Pedido_Id(pedidoId);
        if (splits.isEmpty()) {
            throw new ResourceNotFoundException("Split no encontrado para pedido: " + pedidoId);
        }
        return splits;
    }

    private void validarPedidoPagado(SplitPedidoItem split) {
        if (!PedidoEstado.PAGADO.equals(split.getPedidoItem().getPedido().getEstadoPedido())) {
            throw new OperacionEnConflictoException("El pedido debe estar PAGADO para liberar proveedor.");
        }
    }

    private boolean estaLiberado(SplitPedidoItem split) {
        return LiquidacionProveedorEstado.LIBERADO_AUTOMATICO.equals(split.getEstadoLiquidacionProveedor())
                || LiquidacionProveedorEstado.LIBERADO_MANUAL.equals(split.getEstadoLiquidacionProveedor());
    }

    private LiquidacionProveedorResponse toResponse(UUID pedidoId, List<SplitPedidoItem> splits) {
        SplitPedidoItem referencia = splits.get(0);
        BigDecimal totalProveedor = buildTotalesSplit(splits).totalProveedor();

        return LiquidacionProveedorResponse.builder()
                .pedidoId(pedidoId)
                .estadoLiquidacionProveedor(referencia.getEstadoLiquidacionProveedor().name())
                .liberadoProveedorAt(referencia.getLiberadoProveedorAt())
                .motivoLiberacionManual(referencia.getMotivoLiberacionManual())
                .totalProveedor(totalProveedor)
                .build();
    }

    private LiquidacionPartesResponse toPartesResponse(UUID pedidoId, List<SplitPedidoItem> splits) {
        SplitPedidoItem referencia = splits.get(0);
        SplitPayoutGateway.TotalesSplit totales = buildTotalesSplit(splits);
        SplitPayoutEjecucion payout = splitPayoutEjecucionRepository.findByPedidoId(pedidoId).orElse(null);
        BigDecimal totalComponentes = totales.totalProveedor()
                .add(totales.totalRevendedor())
                .add(totales.totalMd())
                .add(totales.totalSeguro())
                .add(totales.totalPsp());

        return LiquidacionPartesResponse.builder()
                .pedidoId(pedidoId)
                .estadoLiquidacionProveedor(referencia.getEstadoLiquidacionProveedor().name())
                .liberadoProveedorAt(referencia.getLiberadoProveedorAt())
                .origenLiberacion(referencia.getLiberadoProveedorAt() != null ? "RETIRO" : null)
                .totalProveedor(totales.totalProveedor())
                .totalRevendedor(totales.totalRevendedor())
                .totalMd(totales.totalMd())
                .totalEnvio(totales.totalEnvio())
                .totalSeguro(totales.totalSeguro())
                .totalPsp(totales.totalPsp())
                .totalComponentes(totalComponentes)
                .payoutEstado(payout != null && payout.getEstado() != null ? payout.getEstado().name() : null)
                .payoutId(payout != null ? payout.getPayoutId() : null)
                .payoutErrorUltimo(payout != null ? payout.getErrorUltimo() : null)
                .payoutUltimoIntentoAt(payout != null ? payout.getUltimoIntentoAt() : null)
                .payoutCompletadoAt(payout != null ? payout.getCompletadoAt() : null)
                .build();
    }

    private void registrarAuditoria(String tipoEvento, UUID referenciaId, String payloadJson) {
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setTipoEvento(tipoEvento);
        evento.setReferenciaId(referenciaId);
        evento.setPayloadJson(payloadJson);
        auditoriaEventoRepository.save(evento);
    }

    private String buildPayloadLiquidacionPartes(UUID pedidoId, SplitPayoutGateway.TotalesSplit totales, String origen) {
        return "{\"pedidoId\":\"" + pedidoId
                + "\",\"origen\":\"" + escapeJson(origen)
                + "\",\"totalProveedor\":" + totales.totalProveedor()
                + ",\"totalRevendedor\":" + totales.totalRevendedor()
                + ",\"totalMd\":" + totales.totalMd()
                + ",\"totalEnvio\":" + totales.totalEnvio()
                + ",\"totalSeguro\":" + totales.totalSeguro()
                + ",\"totalPsp\":" + totales.totalPsp()
                + "}";
    }

    private SplitPayoutGateway.TotalesSplit buildTotalesSplit(List<SplitPedidoItem> splits) {
        BigDecimal totalProveedor = splits.stream()
                .map(SplitPedidoItem::getMontoProveedor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevendedor = splits.stream()
                .map(SplitPedidoItem::getMontoRevendedor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMd = splits.stream()
                .map(SplitPedidoItem::getMontoMd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEnvio = splits.stream()
                .map(SplitPedidoItem::getMontoEnvio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSeguro = splits.stream()
                .map(SplitPedidoItem::getMontoSeguro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPsp = splits.stream()
                .map(SplitPedidoItem::getMontoPsp)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SplitPayoutGateway.TotalesSplit(
                totalProveedor,
                totalRevendedor,
                totalMd,
                totalEnvio,
                totalSeguro,
                totalPsp);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void intentarPayoutReal(
            UUID pedidoId,
            List<SplitPedidoItem> splits,
            SplitPedidoItem referencia,
            SplitPayoutGateway.TotalesSplit totalesSplit) {
        if (!mercadoPagoClient.isEnabled()) {
            registrarAuditoria("SPLIT_PAYOUT_RETIRADO", pedidoId, "{\"motivo\":\"Mercado Pago retirado; no se enviaron fondos\"}");
            return;
        }
        SplitPayoutEjecucion ejecucion = splitPayoutEjecucionRepository.findByPedidoId(pedidoId)
                .orElseGet(() -> {
                    SplitPayoutEjecucion nueva = new SplitPayoutEjecucion();
                    nueva.setPedidoId(pedidoId);
                    nueva.setEstado(SplitPayoutEstado.PENDIENTE);
                    return nueva;
                });
        if (SplitPayoutEstado.ENVIADO.equals(ejecucion.getEstado())) {
            return;
        }

        RevendedorMercadoPagoService.RevendedorMercadoPagoResolution resolution =
                revendedorMercadoPagoService.resolveForRevendedor(referencia.getPedidoItem().getPedido().getRevendedor().getId());
        if (!resolution.readyForCheckout() || resolution.usaCredencialesGlobalesFallback()) {
            throw new OperacionEnConflictoException(
                    "El revendedor debe tener OAuth de Mercado Pago propio para ejecutar payout real al retirar.");
        }

        MercadoPagoMdConfigService.MercadoPagoMdPayoutConfig mdPayoutConfig = mercadoPagoMdConfigService.resolveForPayout();
        if (!mdPayoutConfig.readyForPayout()) {
            throw new OperacionEnConflictoException(
                    "Falta configurar Mercado Pago MD (OAuth + payout email) para liberar costo operativo MD.");
        }

        List<SplitPayoutGateway.DestinoPayout> destinos = buildDestinosPayout(splits, mdPayoutConfig.payoutEmail());
        if (destinos.isEmpty()) {
            throw new OperacionEnConflictoException("No hay destinos de payout con montos positivos para este pedido.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        ejecucion.setEstado(SplitPayoutEstado.EN_PROCESO);
        ejecucion.setIntentos(ejecucion.getIntentos() + 1);
        ejecucion.setUltimoIntentoAt(ahora);
        splitPayoutEjecucionRepository.save(ejecucion);

        try {
            SplitPayoutGateway.SplitPayoutResult result = splitPayoutGateway.intentarPayoutRetirado(
                    new SplitPayoutGateway.SplitPayoutRequest(
                            pedidoId,
                            resolution.accessToken(),
                            totalesSplit,
                            destinos));
            ejecucion.setEstado(SplitPayoutEstado.ENVIADO);
            ejecucion.setPayoutId(result.payoutId());
            ejecucion.setIdempotencyKey(result.idempotencyKey());
            ejecucion.setPayloadJson(result.payloadRespuestaJson());
            ejecucion.setErrorUltimo(null);
            ejecucion.setCompletadoAt(LocalDateTime.now());
            splitPayoutEjecucionRepository.save(ejecucion);
            registrarAuditoria("SPLIT_PAYOUT_REAL_ENVIADO", pedidoId,
                    "{\"pedidoId\":\"" + pedidoId + "\",\"payoutId\":\"" + escapeJson(result.payoutId()) + "\",\"estado\":\""
                            + escapeJson(result.estado()) + "\"}");
        } catch (Exception ex) {
            ejecucion.setEstado(SplitPayoutEstado.FALLIDO);
            ejecucion.setErrorUltimo(truncate(ex.getMessage(), 500));
            splitPayoutEjecucionRepository.save(ejecucion);
            registrarAuditoria("SPLIT_PAYOUT_REAL_ERROR", pedidoId,
                    "{\"pedidoId\":\"" + pedidoId + "\",\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        }
    }

    private List<SplitPayoutGateway.DestinoPayout> buildDestinosPayout(
            List<SplitPedidoItem> splits,
            String payoutEmailMd) {
        Map<String, BigDecimal> montoPorProveedorEmail = new LinkedHashMap<>();
        BigDecimal montoMdTotal = BigDecimal.ZERO;

        for (SplitPedidoItem split : splits) {
            if (split.getPedidoItem() == null || split.getPedidoItem().getProveedor() == null
                    || split.getPedidoItem().getProveedor().getUsuario() == null
                    || !StringUtils.hasText(split.getPedidoItem().getProveedor().getUsuario().getEmail())) {
                throw new OperacionEnConflictoException(
                        "No se puede liberar payout: hay proveedor sin email para recibir su parte.");
            }
            String proveedorEmail = split.getPedidoItem().getProveedor().getUsuario().getEmail().trim();

            BigDecimal montoProveedor = nvl(split.getMontoProveedor());
            if (montoProveedor.signum() > 0) {
                montoPorProveedorEmail.merge(proveedorEmail, montoProveedor, BigDecimal::add);
            }

            BigDecimal mdItem = nvl(split.getMontoMd()).add(nvl(split.getMontoSeguro()));
            boolean proveedorDespacha = split.getPedidoItem().getProveedor().isDespachaMercaderia();
            if (!proveedorDespacha) {
                mdItem = mdItem.add(nvl(split.getMontoEnvio()));
            }
            montoMdTotal = montoMdTotal.add(mdItem);
        }

        List<SplitPayoutGateway.DestinoPayout> destinos = montoPorProveedorEmail.entrySet().stream()
                .filter(entry -> entry.getValue().signum() > 0)
                .map(entry -> new SplitPayoutGateway.DestinoPayout("PROVEEDOR", entry.getKey(), entry.getValue()))
                .collect(java.util.stream.Collectors.toList());

        if (montoMdTotal.signum() > 0) {
            destinos.add(new SplitPayoutGateway.DestinoPayout("MERCADO_DIGITAL", payoutEmailMd, montoMdTotal));
        }
        return destinos;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
