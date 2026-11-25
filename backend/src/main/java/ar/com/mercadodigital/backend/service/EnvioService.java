package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.service.LiquidacionProveedorService;
import ar.com.mercadodigital.backend.service.NotificationService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import ar.com.mercadodigital.backend.service.TransportistaService;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.dto.request.DespacharEnvioRequest;
import ar.com.mercadodigital.backend.dto.response.EnvioResponse;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.Transportista;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.EnvioRepository;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioDespachado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioEntregado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioListoParaRetiro;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioRetirado;
import ar.com.mercadodigital.backend.service.postcompra.NotificacionInternaEmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnvioService {

    private final EnvioRepository envioRepository;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final NotificationService notificationService;
    private final LiquidacionProveedorService liquidacionProveedorService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransportistaService transportistaService;
    private final NotificacionInternaEmailService notificacionInternaEmailService;

    @Transactional(readOnly = true)
    public EnvioResponse getByPedidoId(UUID pedidoId) {
        Envio envio = getEnvio(pedidoId);
        return toResponse(envio);
    }

    @Transactional
    public EnvioResponse marcarListoParaRetiro(UUID pedidoId) {
        Envio envio = getEnvio(pedidoId);
        if (!PedidoEstado.PAGADO.equals(envio.getPedido().getEstadoPedido())) {
            throw new OperacionEnConflictoException("El pedido debe estar PAGADO para pasar a LISTO_PARA_RETIRO");
        }
        validarTransicion(envio.getEstadoEnvio(), EnvioEstado.PENDIENTE_PREPARACION, EnvioEstado.LISTO_PARA_RETIRO);
        envio.setEstadoEnvio(EnvioEstado.LISTO_PARA_RETIRO);
        Envio saved = envioRepository.save(envio);
        registrarAuditoria("ENVIO_LISTO_PARA_RETIRO", pedidoId, "{\"pedidoId\":\"" + pedidoId + "\"}");
        notificarRevendedor(saved, "ENVIO_LISTO_PARA_RETIRO_REVENDEDOR", "Pedido listo para retiro",
                "El pedido " + pedidoId + " esta listo para retiro.");
        notificationService.createNotificationsForRole(
                "LOGISTICA_MD",
                "ENVIO_LISTO_PARA_RETIRO_LOGISTICA",
                "Retiro pendiente",
                "Hay un pedido listo para retiro. Pedido " + pedidoId + ".");
        registrarAuditoria("NOTIFICACIONES_ENVIO_LISTO_PARA_RETIRO_ADMIN", pedidoId,
                "{\"pedidoId\":\"" + pedidoId
                        + "\",\"notificaRevendedor\":"
                        + (saved.getPedido() != null && saved.getPedido().getRevendedor() != null
                                && saved.getPedido().getRevendedor().getUsuario() != null)
                        + ",\"notificaLogistica\":true,\"origen\":\"ADMIN_MD\"}");
        publicarEventoEnvioListoParaRetiro(saved);
        notificacionInternaEmailService.notificarCambioLogisticoInterno(saved.getPedido(), "LISTO_PARA_RETIRO", null, null);
        return toResponse(saved);
    }

    @Transactional
    public EnvioResponse marcarRetirado(UUID pedidoId) {
        Envio envio = getEnvio(pedidoId);
        validarTransicion(envio.getEstadoEnvio(), EnvioEstado.LISTO_PARA_RETIRO, EnvioEstado.RETIRADO);
        envio.setEstadoEnvio(EnvioEstado.RETIRADO);
        Envio saved = envioRepository.save(envio);
        liquidacionProveedorService.liberarAutomaticamentePorRetiro(pedidoId);
        registrarAuditoria("ENVIO_RETIRADO", pedidoId, "{\"pedidoId\":\"" + pedidoId + "\"}");
        notificarRevendedor(saved, "ENVIO_RETIRADO_REVENDEDOR", "Pedido retirado",
                "El pedido " + pedidoId + " fue retirado por logistica.");
        registrarAuditoria("NOTIFICACIONES_ENVIO_RETIRADO_ADMIN", pedidoId,
                "{\"pedidoId\":\"" + pedidoId
                        + "\",\"notificaRevendedor\":"
                        + (saved.getPedido() != null && saved.getPedido().getRevendedor() != null
                                && saved.getPedido().getRevendedor().getUsuario() != null)
                        + ",\"origen\":\"ADMIN_MD\"}");
        publicarEventoEnvioRetirado(saved);
        notificacionInternaEmailService.notificarCambioLogisticoInterno(saved.getPedido(), "RETIRADO", saved.getTracking(), null);
        return toResponse(saved);
    }

    @Transactional
    public EnvioResponse despachar(UUID pedidoId, DespacharEnvioRequest request) {
        if (request.getTracking() == null || request.getTracking().isBlank()) {
            throw new BadRequestException("tracking obligatorio para despachar");
        }
        if (request.getTransportistaId() == null && !StringUtils.hasText(request.getTransportistaNombre())) {
            throw new BadRequestException("transportista obligatorio para despachar");
        }
        Envio envio = getEnvio(pedidoId);
        validarTransicion(envio.getEstadoEnvio(), EnvioEstado.RETIRADO, EnvioEstado.DESPACHADO);
        Transportista transportista = transportistaService.resolveOrCreate(
                request.getTransportistaId(),
                request.getTransportistaNombre());
        envio.setTransportista(transportista);
        envio.setTracking(request.getTracking().trim());
        envio.setEstadoEnvio(EnvioEstado.DESPACHADO);
        Envio saved = envioRepository.save(envio);
        registrarAuditoria("ENVIO_DESPACHADO", pedidoId, "{\"pedidoId\":\"" + pedidoId
                + "\",\"transportistaId\":\"" + transportista.getId() + "\",\"tracking\":\"" + request.getTracking().trim() + "\"}");
        notificarRevendedor(saved, "ENVIO_DESPACHADO_REVENDEDOR", "Pedido despachado",
                "El pedido " + pedidoId + " fue despachado. Tracking: " + request.getTracking().trim());
        publicarEventoEnvioDespachado(saved);
        registrarAuditoria("NOTIFICACIONES_ENVIO_DESPACHADO_ADMIN", pedidoId,
                "{\"pedidoId\":\"" + pedidoId
                        + "\",\"notificaRevendedor\":"
                        + (saved.getPedido() != null && saved.getPedido().getRevendedor() != null
                                && saved.getPedido().getRevendedor().getUsuario() != null)
                        + ",\"origen\":\"ADMIN_MD\"}");
        notificacionInternaEmailService.notificarCambioLogisticoInterno(
                saved.getPedido(),
                "DESPACHADO",
                saved.getTracking(),
                saved.getTransportista() != null ? saved.getTransportista().getNombre() : null);
        return toResponse(saved);
    }

    @Transactional
    public EnvioResponse despacharPorRevendedor(UUID revendedorId, UUID pedidoId,
            String transportistaNombre, String tracking) {
        Envio envio = getEnvioRevendedor(revendedorId, pedidoId);
        if (!PedidoEstado.PAGADO.equals(envio.getPedido().getEstadoPedido())) {
            throw new OperacionEnConflictoException("El pedido debe estar PAGADO antes de marcarlo como enviado");
        }
        if (!(EnvioEstado.PENDIENTE_PREPARACION.equals(envio.getEstadoEnvio())
                || EnvioEstado.LISTO_PARA_RETIRO.equals(envio.getEstadoEnvio())
                || EnvioEstado.RETIRADO.equals(envio.getEstadoEnvio()))) {
            throw new OperacionEnConflictoException(
                    "El envio no se puede despachar desde el estado actual: " + envio.getEstadoEnvio());
        }

        if (StringUtils.hasText(transportistaNombre)) {
            envio.setTransportista(transportistaService.resolveOrCreate(null, transportistaNombre.trim()));
        }
        envio.setTracking(StringUtils.hasText(tracking) ? tracking.trim() : null);
        envio.setEstadoEnvio(EnvioEstado.DESPACHADO);
        envio.getPedido().setEstadoPedido(PedidoEstado.ENVIADO);
        Envio saved = envioRepository.save(envio);
        registrarAuditoria("ENVIO_DESPACHADO_REVENDEDOR", pedidoId,
                "{\"pedidoId\":\"" + pedidoId + "\",\"revendedorId\":\"" + revendedorId + "\"}");
        publicarEventoEnvioDespachado(saved);
        return toResponse(saved);
    }

    @Transactional
    public EnvioResponse marcarEntregado(UUID pedidoId) {
        Envio envio = this.getEnvio(pedidoId);
        this.validarTransicion(envio.getEstadoEnvio(), EnvioEstado.DESPACHADO, EnvioEstado.ENTREGADO);
        envio.setEstadoEnvio(EnvioEstado.ENTREGADO);
        envio.getPedido().setEstadoPedido(PedidoEstado.ENTREGADO);
        Envio saved = this.envioRepository.save(envio);
        this.registrarAuditoria("ENVIO_ENTREGADO", pedidoId, "{\"pedidoId\":\"" + String.valueOf(pedidoId) + "\"}");
        this.notificarRevendedor(saved, "ENVIO_ENTREGADO_REVENDEDOR", "Pedido entregado", "El pedido " + String.valueOf(pedidoId) + " fue marcado como entregado.");
        this.publicarEventoEnvioEntregado(saved);
        this.registrarAuditoria("NOTIFICACIONES_ENVIO_ENTREGADO_ADMIN", pedidoId, "{\"pedidoId\":\"" + String.valueOf(pedidoId) + "\",\"notificaRevendedor\":" + (saved.getPedido() != null && saved.getPedido().getRevendedor() != null && saved.getPedido().getRevendedor().getUsuario() != null) + ",\"origen\":\"ADMIN_MD\"}");
        return this.toResponse(saved);
    }

    @Transactional
    public EnvioResponse marcarEntregadoPorRevendedor(UUID revendedorId, UUID pedidoId) {
        Envio envio = getEnvioRevendedor(revendedorId, pedidoId);
        validarTransicion(envio.getEstadoEnvio(), EnvioEstado.DESPACHADO, EnvioEstado.ENTREGADO);
        envio.setEstadoEnvio(EnvioEstado.ENTREGADO);
        envio.getPedido().setEstadoPedido(PedidoEstado.ENTREGADO);
        Envio saved = envioRepository.save(envio);
        registrarAuditoria("ENVIO_ENTREGADO_REVENDEDOR", pedidoId,
                "{\"pedidoId\":\"" + pedidoId + "\",\"revendedorId\":\"" + revendedorId + "\"}");
        publicarEventoEnvioEntregado(saved);
        return toResponse(saved);
    }

    private void notificarRevendedor(Envio envio, String tipo, String titulo, String mensaje) {
        if (envio.getPedido() == null
                || envio.getPedido().getRevendedor() == null
                || envio.getPedido().getRevendedor().getUsuario() == null) {
            return;
        }

        notificationService.createNotificationForUser(envio.getPedido().getRevendedor().getUsuario(), tipo, titulo, mensaje);
    }

    private void publicarEventoEnvioDespachado(Envio envio) {
        if (envio.getPedido() == null
                || envio.getPedido().getRevendedor() == null
                || envio.getPedido().getCliente() == null) {
            return;
        }

        applicationEventPublisher.publishEvent(new EventoEnvioDespachado(
                envio.getPedido().getId(),
                envio.getPedido().getRevendedor().getId(),
                envio.getPedido().getCliente().getEmail(),
                envio.getPedido().getCliente().getNombre(),
                envio.getPedido().getTrackingToken(),
                envio.getPedido().getEstadoPedido().name(),
                envio.getEstadoEnvio().name(),
                envio.getTracking(),
                envio.getTransportista() != null ? envio.getTransportista().getNombre() : null,
                java.time.LocalDateTime.now()));
    }

    private void publicarEventoEnvioEntregado(Envio envio) {
        if (envio.getPedido() == null
                || envio.getPedido().getRevendedor() == null
                || envio.getPedido().getCliente() == null) {
            return;
        }

        applicationEventPublisher.publishEvent(new EventoEnvioEntregado(
                envio.getPedido().getId(),
                envio.getPedido().getRevendedor().getId(),
                envio.getPedido().getCliente().getEmail(),
                envio.getPedido().getCliente().getNombre(),
                envio.getPedido().getTrackingToken(),
                envio.getPedido().getEstadoPedido().name(),
                envio.getEstadoEnvio().name(),
                java.time.LocalDateTime.now()));
    }

    private void publicarEventoEnvioListoParaRetiro(Envio envio) {
        if (envio.getPedido() == null
                || envio.getPedido().getRevendedor() == null
                || envio.getPedido().getCliente() == null) {
            return;
        }

        applicationEventPublisher.publishEvent(new EventoEnvioListoParaRetiro(
                envio.getPedido().getId(),
                envio.getPedido().getRevendedor().getId(),
                envio.getPedido().getCliente().getEmail(),
                envio.getPedido().getCliente().getNombre(),
                envio.getPedido().getTrackingToken(),
                envio.getPedido().getEstadoPedido().name(),
                envio.getEstadoEnvio().name(),
                java.time.LocalDateTime.now()));
    }

    private void publicarEventoEnvioRetirado(Envio envio) {
        if (envio.getPedido() == null
                || envio.getPedido().getRevendedor() == null
                || envio.getPedido().getCliente() == null) {
            return;
        }

        applicationEventPublisher.publishEvent(new EventoEnvioRetirado(
                envio.getPedido().getId(),
                envio.getPedido().getRevendedor().getId(),
                envio.getPedido().getCliente().getEmail(),
                envio.getPedido().getCliente().getNombre(),
                envio.getPedido().getTrackingToken(),
                envio.getPedido().getEstadoPedido().name(),
                envio.getEstadoEnvio().name(),
                java.time.LocalDateTime.now()));
    }
    private Envio getEnvio(UUID pedidoId) {
        return envioRepository.findByPedido_Id(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Envio no encontrado para pedido: " + pedidoId));
    }

    private Envio getEnvioRevendedor(UUID revendedorId, UUID pedidoId) {
        return envioRepository.findByPedido_IdAndPedido_Revendedor_Id(pedidoId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Envio no encontrado para pedido: " + pedidoId));
    }

    private void validarTransicion(EnvioEstado actual, EnvioEstado esperadoActual, EnvioEstado siguiente) {
        if (!esperadoActual.equals(actual)) {
            throw new OperacionEnConflictoException(
                    "Transicion de envio invalida. Estado actual: " + actual + ", esperado: " + esperadoActual
                            + ", destino: " + siguiente);
        }
    }

    private void registrarAuditoria(String tipoEvento, UUID referenciaId, String payloadJson) {
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setTipoEvento(tipoEvento);
        evento.setReferenciaId(referenciaId);
        evento.setPayloadJson(payloadJson);
        auditoriaEventoRepository.save(evento);
    }

    private EnvioResponse toResponse(Envio envio) {
        return EnvioResponse.builder()
                .pedidoId(envio.getPedido().getId())
                .estadoEnvio(envio.getEstadoEnvio().name())
                .tracking(envio.getTracking())
                .transportistaNombre(envio.getTransportista() != null ? envio.getTransportista().getNombre() : null)
                .costoEnvio(envio.getCostoEnvio() != null ? envio.getCostoEnvio() : BigDecimal.ZERO)
                .build();
    }


    @Transactional
    public EnvioResponse marcarEntregadoPorCliente(String trackingToken, String storefrontHost) {
        Envio envio = this.envioRepository.findByPedido_TrackingToken(trackingToken).orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (!(envio.getPedido() != null && OrigenPedido.PUBLICO_DIRECTO.equals((Object)envio.getPedido().getOrigenPedido()) && envio.getPedido().getRevendedor() != null && StringUtils.hasText(envio.getPedido().getRevendedor().getDominio()) && StringUtils.hasText(storefrontHost) && StorefrontService.hostsEquivalentes(envio.getPedido().getRevendedor().getDominio(), storefrontHost))) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        if (EnvioEstado.ENTREGADO.equals((Object)envio.getEstadoEnvio())) {
            return this.toResponse(envio);
        }
        this.validarTransicion(envio.getEstadoEnvio(), EnvioEstado.DESPACHADO, EnvioEstado.ENTREGADO);
        envio.setEstadoEnvio(EnvioEstado.ENTREGADO);
        envio.getPedido().setEstadoPedido(PedidoEstado.ENTREGADO);
        Envio saved = this.envioRepository.save(envio);
        this.registrarAuditoria("ENVIO_ENTREGADO_CLIENTE_QR", saved.getPedido().getId(), "{\"pedidoId\":\"" + String.valueOf(saved.getPedido().getId()) + "\",\"origen\":\"QR_ROTULO\"}");
        this.notificarRevendedor(saved, "ENVIO_ENTREGADO_CLIENTE", "Entrega confirmada por el cliente", "El cliente confirmo la entrega del pedido " + String.valueOf(saved.getPedido().getId()) + ".");
        this.publicarEventoEnvioEntregado(saved);
        return this.toResponse(saved);
    }
}
