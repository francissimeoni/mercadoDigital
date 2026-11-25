package ar.com.mercadodigital.backend.service.postcompra;

import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompra;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompraResolver;
import ar.com.mercadodigital.backend.service.postcompra.ContenidoEmail;
import ar.com.mercadodigital.backend.service.postcompra.EmailSender;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioDespachado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioEntregado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioListoParaRetiro;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioRetirado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPedidoCreado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaCerrado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaEnvioSolicitado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionEntregada;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionReenviada;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaSolicitado;
import ar.com.mercadodigital.backend.service.postcompra.EventoTransferenciaSolicitada;
import ar.com.mercadodigital.backend.service.postcompra.MensajeEmail;
import ar.com.mercadodigital.backend.service.postcompra.PlantillaEmailPostCompraService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionPostCompraService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPostCompraService.class);

    private final EmailSender emailSender;
    private final BrandingPostCompraResolver brandingResolver;
    private final PlantillaEmailPostCompraService plantillaService;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final ObjectMapper objectMapper;

    public void notificarPedidoCreado(EventoPedidoCreado evento) {
        enviarPedidoCreado(evento);
    }

    public void notificarPagoConfirmado(EventoPagoConfirmado evento) {
        enviarPagoConfirmado(evento);
    }

    public void notificarEnvioDespachado(EventoEnvioDespachado evento) {
        enviarEnvioDespachado(evento);
    }

    public void notificarEnvioEntregado(EventoEnvioEntregado evento) {
        enviarEnvioEntregado(evento);
    }

    public void notificarEnvioListoParaRetiro(EventoEnvioListoParaRetiro evento) {
        enviarEnvioListoParaRetiro(evento);
    }

    public void notificarEnvioRetirado(EventoEnvioRetirado evento) {
        enviarEnvioRetirado(evento);
    }

    public void notificarRmaSolicitado(EventoRmaSolicitado evento) {
        enviarRmaSolicitado(evento);
    }

    public void notificarRmaEnvioSolicitado(EventoRmaEnvioSolicitado evento) {
        enviarRmaEnvioSolicitado(evento);
    }

    public void notificarRmaCerrado(EventoRmaCerrado evento) {
        enviarRmaCerrado(evento);
    }

    public void notificarRmaReposicionReenviada(EventoRmaReposicionReenviada evento) {
        enviarRmaReposicionReenviada(evento);
    }

    public void notificarRmaReposicionEntregada(EventoRmaReposicionEntregada evento) {
        enviarRmaReposicionEntregada(evento);
    }

    private void enviarPedidoCreado(EventoPedidoCreado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_PEDIDO_CREADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarPedidoCreado(evento, branding);
        enviar("pedido-creado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarPagoConfirmado(EventoPagoConfirmado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_PAGO_CONFIRMADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarPagoConfirmado(evento, branding);
        enviar("pago-confirmado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarEnvioDespachado(EventoEnvioDespachado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_ENVIO_DESPACHADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarEnvioDespachado(evento, branding);
        enviar("envio-despachado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarEnvioEntregado(EventoEnvioEntregado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_ENVIO_ENTREGADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarEnvioEntregado(evento, branding);
        enviar("envio-entregado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarEnvioListoParaRetiro(EventoEnvioListoParaRetiro evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_ENVIO_LISTO_PARA_RETIRO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarEnvioListoParaRetiro(evento, branding);
        enviar("envio-listo-para-retiro", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarEnvioRetirado(EventoEnvioRetirado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_ENVIO_RETIRADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarEnvioRetirado(evento, branding);
        enviar("envio-retirado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarRmaSolicitado(EventoRmaSolicitado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_RMA_SOLICITADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "rmaId", evento.rmaId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarRmaSolicitado(evento, branding);
        enviar("rma-solicitado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarRmaEnvioSolicitado(EventoRmaEnvioSolicitado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_RMA_ENVIO_SOLICITADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "rmaId", evento.rmaId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarRmaEnvioSolicitado(evento, branding);
        enviar("rma-envio-solicitado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarRmaCerrado(EventoRmaCerrado evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_RMA_CERRADO_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "rmaId", evento.rmaId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarRmaCerrado(evento, branding);
        enviar("rma-cerrado", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarRmaReposicionReenviada(EventoRmaReposicionReenviada evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_RMA_REPOSICION_REENVIADA_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "rmaId", evento.rmaId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarRmaReposicionReenviada(evento, branding);
        enviar("rma-reposicion-reenviada", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviarRmaReposicionEntregada(EventoRmaReposicionEntregada evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.trackingToken())) {
            registrarAuditoriaSegura("EMAIL_RMA_REPOSICION_ENTREGADA_OMITIDO", evento.pedidoId(), Map.of(
                    "motivo", "EMAIL_O_TRACKING_FALTANTE",
                    "pedidoId", evento.pedidoId(),
                    "rmaId", evento.rmaId(),
                    "destinatario", evento.clienteEmail()));
            return;
        }

        BrandingPostCompra branding = brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = plantillaService.renderizarRmaReposicionEntregada(evento, branding);
        enviar("rma-reposicion-entregada", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private void enviar(String tipoNotificacion, UUID pedidoId, String destinatario, BrandingPostCompra branding, ContenidoEmail contenido, String trackingToken) {
        if (!this.smtpPropioConfigurado(branding)) {
            this.registrarAuditoriaSegura(this.tipoAuditoria(tipoNotificacion, "OMITIDO"), pedidoId, Map.of("tipoNotificacion", tipoNotificacion, "destinatario", destinatario, "trackingToken", trackingToken, "motivo", "SMTP_REVENDEDOR_NO_CONFIGURADO"));
            log.warn("Email de storefront omitido: la tienda no tiene SMTP propio. tipo={} pedidoId={}", (Object)tipoNotificacion, (Object)pedidoId);
            return;
        }
        try {
            this.emailSender.send(MensajeEmail.builder().to(destinatario).subject(contenido.subject()).htmlBody(contenido.htmlBody()).textBody(contenido.textBody()).fromName(branding.nombreRemitente()).fromEmail(branding.emailRemitente()).replyTo(branding.emailRemitente()).smtpHost(branding.smtpHost()).smtpPort(branding.smtpPort()).smtpUsername(branding.smtpUsername()).smtpPassword(branding.smtpPassword()).smtpAuth(branding.smtpAuth()).smtpStartTls(branding.smtpStartTls()).metadata(Map.of("tipoNotificacion", tipoNotificacion, "pedidoId", pedidoId.toString(), "trackingToken", trackingToken)).build());
            this.registrarAuditoriaSegura(this.tipoAuditoria(tipoNotificacion, "ENVIADO"), pedidoId, Map.of("tipoNotificacion", tipoNotificacion, "destinatario", destinatario, "trackingToken", trackingToken, "subject", contenido.subject()));
        }
        catch (Exception ex) {
            log.warn("Fallo envio de email post-compra tipo={} pedidoId={} destinatario={}", tipoNotificacion, pedidoId, destinatario, ex);
            this.registrarAuditoriaSegura(this.tipoAuditoria(tipoNotificacion, "ERROR"), pedidoId, Map.of("tipoNotificacion", tipoNotificacion, "destinatario", destinatario, "trackingToken", trackingToken, "error", ex.getMessage()));
        }
    }

    private String tipoAuditoria(String tipoNotificacion, String sufijo) {
        return "EMAIL_" + tipoNotificacion.replace('-', '_').toUpperCase() + "_" + sufijo;
    }

    private void registrarAuditoriaSegura(String tipoEvento, UUID referenciaId, Map<String, Object> payload) {
        try {
            AuditoriaEvento evento = new AuditoriaEvento();
            evento.setTipoEvento(tipoEvento);
            evento.setReferenciaId(referenciaId);
            evento.setPayloadJson(toJson(payload));
            auditoriaEventoRepository.save(evento);
        } catch (Exception ex) {
            log.warn("No se pudo registrar auditoria post-compra tipo={} referenciaId={}", tipoEvento, referenciaId, ex);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(payload));
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"NO_SE_PUDO_SERIALIZAR_PAYLOAD\"}";
        }
    }


    public void notificarTransferenciaSolicitada(EventoTransferenciaSolicitada evento) {
        if (!StringUtils.hasText(evento.clienteEmail()) || !StringUtils.hasText(evento.transferenciaToken())) {
            return;
        }
        BrandingPostCompra branding = this.brandingResolver.resolver(evento.revendedorId());
        ContenidoEmail contenido = this.plantillaService.renderizarTransferenciaSolicitada(evento, branding);
        this.enviar("transferencia-solicitada", evento.pedidoId(), evento.clienteEmail(), branding, contenido, evento.trackingToken());
    }

    private boolean smtpPropioConfigurado(BrandingPostCompra branding) {
        return branding != null && StringUtils.hasText(branding.smtpHost()) && StringUtils.hasText(branding.smtpUsername()) && StringUtils.hasText(branding.smtpPassword());
    }
}
