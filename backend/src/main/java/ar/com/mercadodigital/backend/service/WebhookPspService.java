package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.dto.request.WebhookPspRequest;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PspWebhook;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.EnvioRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.PspWebhookRepository;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoInternoConfirmado;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebhookPspService {

    private static final String TIPO_EVENTO_PAGO_CONFIRMADO = "PAGO_CONFIRMADO";
    private static final String ESTADO_CONFIRMADO = "CONFIRMADO";

    private final PspWebhookRepository pspWebhookRepository;
    private final PedidoRepository pedidoRepository;
    private final EnvioRepository envioRepository;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;

    @Transactional(noRollbackFor={ResourceNotFoundException.class, OperacionEnConflictoException.class})
    public void procesarWebhookMercadoPago(WebhookPspRequest request,
            RevendedorMercadoPagoService.RevendedorMercadoPagoResolution origen,
            String collectorIdVerificado) {
        UUID pedidoId = parseUuid(request.getReferenciaPedido(), "referenciaPedido invalida");
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        var esperado = revendedorMercadoPagoService.resolveForRevendedor(pedido.getRevendedor().getId());
        if (origen == null || esperado == null || !origen.readyForCheckout() || !esperado.readyForCheckout()
                || !StringUtils.hasText(collectorIdVerificado)
                || origen.usaCredencialesGlobalesFallback() != esperado.usaCredencialesGlobalesFallback()
                || (!origen.usaCredencialesGlobalesFallback() && (!collectorIdVerificado.equals(origen.mercadoPagoUserId())
                    || !collectorIdVerificado.equals(esperado.mercadoPagoUserId())))) {
            throw new OperacionEnConflictoException("El cobro no corresponde a la cuenta de esta tienda");
        }
        procesarWebhook(request);
    }

    @Transactional(noRollbackFor={ResourceNotFoundException.class, OperacionEnConflictoException.class})
    public void procesarWebhook(WebhookPspRequest request) {
        var existente = this.pspWebhookRepository.findByIdEventoPsp(request.getIdEventoPsp());
        if (existente.isPresent() && existente.get().isProcesadoOk()) {
            return;
        }
        PspWebhook webhook;
        if (existente.isPresent()) {
            webhook = existente.get();
        } else {
            webhook = new PspWebhook();
            webhook.setId(UUID.randomUUID());
            webhook.setIdEventoPsp(request.getIdEventoPsp());
            webhook.setTipoEvento(request.getTipoEvento());
            webhook.setPayloadJson(this.toJson(request));
            webhook.setRecibidoEn(LocalDateTime.now());
            webhook.setProcesadoOk(false);
            this.pspWebhookRepository.saveAndFlush(webhook);
            this.registrarAuditoria("WEBHOOK_PSP_RECIBIDO", null, webhook.getPayloadJson());
        }
        if (!TIPO_EVENTO_PAGO_CONFIRMADO.equalsIgnoreCase(request.getTipoEvento())) {
            this.procesarEstadoNoAprobado(request);
            webhook.setProcesadoOk(true);
            this.pspWebhookRepository.save(webhook);
            return;
        }
        if (!ESTADO_CONFIRMADO.equalsIgnoreCase(request.getEstado())) {
            webhook.setProcesadoOk(true);
            this.pspWebhookRepository.save(webhook);
            return;
        }
        UUID pedidoId = this.parseUuid(request.getReferenciaPedido(), "referenciaPedido invalida");
        Pedido pedido = this.pedidoRepository.findById(pedidoId).orElseThrow(() -> {
            webhook.setPayloadJson(this.toJson(Map.of("motivo", "PEDIDO_NO_ENCONTRADO", "referenciaPedido", request.getReferenciaPedido(), "idEventoPsp", request.getIdEventoPsp())));
            this.pspWebhookRepository.save(webhook);
            return new ResourceNotFoundException("Pedido no encontrado: " + request.getReferenciaPedido());
        });
        if (PedidoEstado.CANCELADO.equals((Object)pedido.getEstadoPedido())) {
            webhook.setProcesadoOk(true);
            this.pspWebhookRepository.save(webhook);
            this.registrarAuditoria("COBRO_TARDIO_CONCILIACION", pedido.getId(), webhook.getPayloadJson());
            return;
        }
        if (PedidoEstado.PAGADO.equals(pedido.getEstadoPedido())
                || PedidoEstado.ENVIADO.equals(pedido.getEstadoPedido())
                || PedidoEstado.ENTREGADO.equals(pedido.getEstadoPedido())) {
            webhook.setProcesadoOk(true);
            this.pspWebhookRepository.save(webhook);
            if (pedido.getPago() != null && !request.getReferenciaPago().equals(pedido.getPago().getPspReferencia())) {
                this.registrarAuditoria("COBRO_DUPLICADO_CONCILIACION", pedido.getId(), webhook.getPayloadJson());
            }
            return;
        }
        this.validarMonto(pedido.getTotalFinal(), request.getMonto());
        Pago pago = pedido.getPago();
        if (pedido.getEstadoPedido() != PedidoEstado.CREADO || pago == null
                || !"PSP".equalsIgnoreCase(pago.getMetodo())
                || (pago.getEstadoPago() != PagoEstado.PENDIENTE && pago.getEstadoPago() != PagoEstado.RECHAZADO)) {
            throw new OperacionEnConflictoException("El pedido ya no admite confirmar este pago PSP");
        }
        this.validarMonto(pago.getMonto(), request.getMonto());
        pago.setEstadoPago(PagoEstado.CONFIRMADO);
        pago.setPspReferencia(request.getReferenciaPago());
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        this.crearEnvioSiNoExiste(pedido);
        this.pedidoRepository.save(pedido);
        webhook.setProcesadoOk(true);
        this.pspWebhookRepository.save(webhook);
        this.registrarAuditoria(TIPO_EVENTO_PAGO_CONFIRMADO, pedido.getId(), this.toJson(Map.of("pedidoId", pedido.getId(), "referenciaPago", request.getReferenciaPago(), "monto", request.getMonto())));
        this.notificarActoresOperativosPagoConfirmado(pedido);
        this.applicationEventPublisher.publishEvent(new EventoPagoConfirmado(pedido.getId(), pago.getId(), pedido.getRevendedor().getId(), pedido.getCliente().getEmail(), pedido.getCliente().getNombre(), pedido.getTrackingToken(), pedido.getEstadoPedido().name(), request.getMonto(), "ARS", LocalDateTime.now()));
    }

    private void notificarActoresOperativosPagoConfirmado(Pedido pedido) {
        if (pedido.getRevendedor() != null && pedido.getRevendedor().getUsuario() != null) {
            this.notificationService.createNotificationForUser(pedido.getRevendedor().getUsuario(), "PAGO_CONFIRMADO_REVENDEDOR", "Pago confirmado", "Mercado Pago confirmo el pago del pedido " + String.valueOf(pedido.getId()) + ". Ya puede prepararse para el envio.");
        }
        this.notificationService.createNotificationsForUsers(pedido.getItems().stream().map(PedidoItem::getProveedor).filter(Objects::nonNull).map(Proveedor::getUsuario).filter(Objects::nonNull).toList(), "PAGO_CONFIRMADO_PROVEEDOR", "Pedido confirmado para preparar", "El pedido " + String.valueOf(pedido.getId()) + " quedo pago y puede entrar en preparacion.");
        this.registrarAuditoria("NOTIFICACIONES_PAGO_CONFIRMADO_ADMIN", pedido.getId(), this.toJson(Map.of("pedidoId", pedido.getId(), "revendedorId", pedido.getRevendedor() != null ? pedido.getRevendedor().getId() : null, "notificaRevendedor", pedido.getRevendedor() != null && pedido.getRevendedor().getUsuario() != null, "notificaProveedor", true, "origen", "ADMIN_MD")));
        if (!OrigenPedido.PUBLICO_DIRECTO.equals(pedido.getOrigenPedido())) {
            this.applicationEventPublisher.publishEvent(new EventoPagoInternoConfirmado(pedido.getId()));
        }
    }

    private void validarMonto(BigDecimal totalPedido, BigDecimal montoWebhook) {
        if (totalPedido == null || montoWebhook == null || totalPedido.compareTo(montoWebhook) != 0) {
            throw new OperacionEnConflictoException(
                    "Monto inconsistente para confirmacion. totalFinal=" + totalPedido + ", webhook=" + montoWebhook);
        }
    }

    private UUID parseUuid(String valor, String mensajeError) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            throw new OperacionEnConflictoException(mensajeError);
        }
    }

    private void crearEnvioSiNoExiste(Pedido pedido) {
        if (pedido.getEnvio() != null) {
            if (EnvioEstado.PENDIENTE_PAGO.equals(pedido.getEnvio().getEstadoEnvio())) {
                pedido.getEnvio().setEstadoEnvio(EnvioEstado.PENDIENTE_PREPARACION);
            }
            return;
        }
        Envio envio = new Envio();
        envio.setPedido(pedido);
        envio.setEstadoEnvio(EnvioEstado.PENDIENTE_PREPARACION);
        envio.setCategoriaPaquete(pedido.getItems().isEmpty() ? null : pedido.getItems().get(0).getProducto().getCategoriaPaquete());
        envio.setCostoEnvio(pedido.getTotalEnvio() != null ? pedido.getTotalEnvio() : BigDecimal.ZERO);
        envio.setTracking(null);
        envio.setDireccionEnvio("SIN_DIRECCION");
        pedido.setEnvio(envio);
        envioRepository.save(envio);
    }

    private void registrarAuditoria(String tipoEvento, UUID referenciaId, String payloadJson) {
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setTipoEvento(tipoEvento);
        evento.setReferenciaId(referenciaId);
        evento.setPayloadJson(payloadJson);
        auditoriaEventoRepository.save(evento);
    }

    private String toJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar payload de webhook PSP", ex);
        }
    }


    private void procesarEstadoNoAprobado(WebhookPspRequest request) {
        PagoEstado estadoPago;
        switch (request.getEstado() != null ? request.getEstado().toUpperCase() : "") {
            case "REJECTED":
            case "CANCELLED": {
                estadoPago = PagoEstado.RECHAZADO;
                break;
            }
            case "REFUNDED":
            case "CHARGED_BACK": {
                estadoPago = PagoEstado.REEMBOLSADO;
                break;
            }
            default: {
                estadoPago = null;
            }
        }
        if (estadoPago == null || request.getReferenciaPedido() == null) {
            return;
        }
        UUID pedidoId = this.parseUuid(request.getReferenciaPedido(), "referenciaPedido invalida");
        Pedido pedido = this.pedidoRepository.findById(pedidoId).orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + String.valueOf(pedidoId)));
        if (PedidoEstado.CANCELADO.equals((Object)pedido.getEstadoPedido())) {
            this.registrarAuditoria("WEBHOOK_PSP_PEDIDO_CANCELADO", pedido.getId(), this.toJson(Map.of("pedidoId", pedido.getId(), "estado", request.getEstado() != null ? request.getEstado() : "")));
            return;
        }
        Pago pago = pedido.getPago();
        if (pago == null || !"PSP".equalsIgnoreCase(pago.getMetodo())) {
            throw new OperacionEnConflictoException("El pedido no tiene un pago PSP asociado");
        }
        this.validarMonto(pedido.getTotalFinal(), request.getMonto());
        this.validarMonto(pago.getMonto(), request.getMonto());
        if ((pago.getEstadoPago() == PagoEstado.CONFIRMADO || pago.getEstadoPago() == PagoEstado.APROBADO
                || pago.getEstadoPago() == PagoEstado.REEMBOLSADO)
                && !Objects.equals(pago.getPspReferencia(), request.getReferenciaPago())) {
            this.registrarAuditoria("WEBHOOK_PSP_OTRO_PAGO", pedidoId,
                    this.toJson(Map.of("pedidoId", pedidoId,
                            "referenciaPago", request.getReferenciaPago() != null ? request.getReferenciaPago() : "",
                            "estado", request.getEstado() != null ? request.getEstado() : "")));
            return;
        }
        if (estadoPago == PagoEstado.RECHAZADO && (pago.getEstadoPago() == PagoEstado.CONFIRMADO
                || pago.getEstadoPago() == PagoEstado.APROBADO || pago.getEstadoPago() == PagoEstado.REEMBOLSADO)) {
            this.registrarAuditoria("WEBHOOK_PSP_ESTADO_ANTERIOR", pedidoId,
                    this.toJson(Map.of("pedidoId", pedidoId, "estado", request.getEstado())));
            return;
        }
        if (estadoPago == pago.getEstadoPago()) {
            return;
        }
        pago.setEstadoPago(estadoPago);
        pago.setPspReferencia(request.getReferenciaPago());
        this.pedidoRepository.save(pedido);
        this.registrarAuditoria("PAGO_" + estadoPago.name(), pedidoId, this.toJson(Map.of("pedidoId", pedidoId, "referenciaPago", request.getReferenciaPago() != null ? request.getReferenciaPago() : "", "estado", estadoPago.name())));
        this.notificarRevendedorEstadoPago(pedido, estadoPago);
    }

    private void notificarRevendedorEstadoPago(Pedido pedido, PagoEstado estadoPago) {
        if (pedido.getRevendedor() == null || pedido.getRevendedor().getUsuario() == null) {
            return;
        }
        String titulo = PagoEstado.REEMBOLSADO.equals((Object)estadoPago) ? "Pago reembolsado" : "Pago no completado";
        String mensaje = PagoEstado.REEMBOLSADO.equals((Object)estadoPago) ? "Mercado Pago informo el reembolso del pedido " + String.valueOf(pedido.getId()) + "." : "Mercado Pago informo que el pago del pedido " + String.valueOf(pedido.getId()) + " fue rechazado o cancelado.";
        this.notificationService.createNotificationForUser(pedido.getRevendedor().getUsuario(), "PAGO_" + estadoPago.name() + "_REVENDEDOR", titulo, mensaje);
    }
}
