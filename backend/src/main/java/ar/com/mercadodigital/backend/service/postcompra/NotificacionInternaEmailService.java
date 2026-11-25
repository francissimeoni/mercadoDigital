package ar.com.mercadodigital.backend.service.postcompra;

import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompra;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompraResolver;
import ar.com.mercadodigital.backend.service.postcompra.EmailSender;
import ar.com.mercadodigital.backend.service.postcompra.EventoComprobanteTransferenciaCargado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioEntregado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPedidoCreado;
import ar.com.mercadodigital.backend.service.postcompra.MensajeEmail;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.entity.ConfiguracionCorreoMd;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.service.CorreoMdConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionInternaEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionInternaEmailService.class);

    private final EmailSender emailSender;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final CorreoMdConfigService correoMdConfigService;
    private final BrandingPostCompraResolver brandingResolver;
    private final ObjectMapper objectMapper;

    public void notificarVentaInterna(Pedido pedido) {
        String emailRevendedor;
        if (pedido == null || pedido.getId() == null) {
            return;
        }
        if (OrigenPedido.PUBLICO_DIRECTO.equals((Object)pedido.getOrigenPedido())) {
            return;
        }
        String trackingToken = pedido.getTrackingToken();
        String panelUrl = this.resolverPanelPedidoUrl(pedido);
        String asunto = "Nueva venta registrada - Pedido " + String.valueOf(pedido.getId());
        String cuerpoBase = "Se registro una nueva venta.\nPedido: " + String.valueOf(pedido.getId()) + "\nEstado pedido: " + String.valueOf((Object)pedido.getEstadoPedido()) + "\nTracking cliente: " + (StringUtils.hasText(trackingToken) ? trackingToken : "No disponible") + (String)(panelUrl != null ? "\nPanel del pedido: " + panelUrl : "");
        String string = emailRevendedor = pedido.getRevendedor() != null && pedido.getRevendedor().getUsuario() != null ? pedido.getRevendedor().getUsuario().getEmail() : null;
        if (!OrigenPedido.PUBLICO_DIRECTO.equals((Object)pedido.getOrigenPedido())) {
            this.enviarInterno("EMAIL_INTERNO_VENTA_REVENDEDOR", pedido.getId(), emailRevendedor, asunto, cuerpoBase, Map.of("actorDestino", "REVENDEDOR", "trackingToken", StringUtils.hasText(trackingToken) ? trackingToken : ""));
        }
        LinkedHashSet<String> emailsProveedores = new LinkedHashSet<String>();
        for (PedidoItem item : pedido.getItems()) {
            if (item == null || item.getProveedor() == null || item.getProveedor().getUsuario() == null || !StringUtils.hasText(item.getProveedor().getUsuario().getEmail())) continue;
            emailsProveedores.add(item.getProveedor().getUsuario().getEmail().trim());
        }
        for (String emailProveedor : emailsProveedores) {
            this.enviarInterno("EMAIL_INTERNO_VENTA_PROVEEDOR", pedido.getId(), emailProveedor, "Solicitud formal de preparacion - Pedido " + String.valueOf(pedido.getId()), "Se solicita preparar el pedido " + String.valueOf(pedido.getId()) + " para su circuito logistico.", Map.of("actorDestino", "PROVEEDOR"));
        }
    }

    @Transactional
    public void notificarPagoInternoConfirmado(UUID pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || OrigenPedido.PUBLICO_DIRECTO.equals(pedido.getOrigenPedido())) {
            return;
        }
        notificarVentaInterna(pedido);
        notificarCambioLogisticoInterno(pedido, "PAGO_CONFIRMADO", null, null);
    }

    public void notificarCambioLogisticoInterno(Pedido pedido, String hito, String tracking, String transportista) {
        if (pedido == null || pedido.getId() == null || !StringUtils.hasText(hito)) {
            return;
        }
        if (OrigenPedido.PUBLICO_DIRECTO.equals((Object)pedido.getOrigenPedido())) {
            return;
        }
        String asunto = "Actualizacion operativa " + hito + " - Pedido " + String.valueOf(pedido.getId());
        String cuerpo = "Actualizacion interna de operacion.\nHito: " + hito + "\nPedido: " + String.valueOf(pedido.getId()) + "\nTracking envio: " + (StringUtils.hasText(tracking) ? tracking : "No disponible") + "\nTransportista: " + (StringUtils.hasText(transportista) ? transportista : "No informado");
        LinkedHashSet<String> destinatarios = new LinkedHashSet<String>();
        if (pedido.getRevendedor() != null && pedido.getRevendedor().getUsuario() != null && StringUtils.hasText(pedido.getRevendedor().getUsuario().getEmail())) {
            destinatarios.add(pedido.getRevendedor().getUsuario().getEmail().trim());
        }
        for (PedidoItem item : pedido.getItems()) {
            if (item == null || item.getProveedor() == null || item.getProveedor().getUsuario() == null || !StringUtils.hasText(item.getProveedor().getUsuario().getEmail())) continue;
            destinatarios.add(item.getProveedor().getUsuario().getEmail().trim());
        }
        for (Usuario usuarioLogistica : this.usuarioRepository.findActivosByRolNombre("LOGISTICA_MD")) {
            if (usuarioLogistica == null || !StringUtils.hasText(usuarioLogistica.getEmail())) continue;
            destinatarios.add(usuarioLogistica.getEmail().trim());
        }
        for (String destinatario : destinatarios) {
            this.enviarInterno("EMAIL_INTERNO_LOGISTICA_" + hito, pedido.getId(), destinatario, asunto, cuerpo, Map.of("actorDestino", "INTERNO", "hito", hito));
        }
    }

    private void enviarInterno(String tipoAuditoria, UUID pedidoId, String destinatario, String subject, String textBody, Map<String, String> metadata) {
        if (!StringUtils.hasText(destinatario)) {
            this.registrarAuditoriaSegura(tipoAuditoria + "_OMITIDO", pedidoId, Map.of("motivo", "DESTINATARIO_FALTANTE", "subject", subject));
            return;
        }
        try {
            ConfiguracionCorreoMd config = this.correoMdConfigService.getConfigRuntime();
            this.emailSender.send(MensajeEmail.builder().to(destinatario.trim()).subject(subject).textBody(textBody).htmlBody(this.renderHtmlInterno(textBody)).fromName(config.getFromName()).fromEmail(config.getFromEmail()).replyTo(StringUtils.hasText(config.getReplyTo()) ? config.getReplyTo() : config.getFromEmail()).smtpHost(config.getSmtpHost()).smtpPort(config.getSmtpPort()).smtpUsername(config.getSmtpUsername()).smtpPassword(config.getSmtpPassword()).smtpAuth(config.isSmtpAuth()).smtpStartTls(config.isSmtpStartTls()).metadata(metadata).build());
            this.registrarAuditoriaSegura(tipoAuditoria + "_ENVIADO", pedidoId, Map.of("destinatario", destinatario, "subject", subject));
        }
        catch (Exception ex) {
            log.warn("Fallo envio de email interno tipo={} pedidoId={} destinatario={}", tipoAuditoria, pedidoId, destinatario, ex);
            this.registrarAuditoriaSegura(tipoAuditoria + "_ERROR", pedidoId, Map.of("destinatario", destinatario, "subject", subject, "error", ex.getMessage()));
        }
    }

    private void registrarAuditoriaSegura(String tipoEvento, UUID referenciaId, Map<String, Object> payload) {
        try {
            AuditoriaEvento evento = new AuditoriaEvento();
            evento.setTipoEvento(tipoEvento);
            evento.setReferenciaId(referenciaId);
            evento.setPayloadJson(toJson(payload));
            auditoriaEventoRepository.save(evento);
        } catch (Exception ex) {
            log.warn("No se pudo registrar auditoria de email interno tipo={} referenciaId={}", tipoEvento, referenciaId, ex);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(payload));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }


    @Transactional(readOnly=true)
    public void notificarPedidoCreado(EventoPedidoCreado evento) {
        this.notificarVentaRevendedor(evento.pedidoId());
    }

    @Transactional(readOnly=true)
    public void notificarPagoConfirmado(EventoPagoConfirmado evento) {
        this.notificarVentaRevendedor(evento.pedidoId());
    }

    private void notificarVentaRevendedor(UUID pedidoId) {
        Pedido pedido = this.pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getRevendedor() == null || pedido.getRevendedor().getUsuario() == null) {
            return;
        }
        String panelUrl = this.resolverPanelPedidoUrl(pedido);
        String productos = pedido.getItems().stream().map(item -> item.getCantidad() + " x " + this.nombreProducto((PedidoItem)item)).collect(Collectors.joining(", "));
        String cuerpo = "\u00a1Vendiste! Se registro una nueva compra en tu tienda.\nPedido: #" + pedido.getId().toString().substring(0, 8).toUpperCase() + "\nCliente: " + this.valor(pedido.getCliente() != null ? pedido.getCliente().getNombre() : null) + "\nEmail: " + this.valor(pedido.getCliente() != null ? pedido.getCliente().getEmail() : null) + "\nProductos: " + this.valor(productos) + "\nTotal: $ " + String.valueOf(pedido.getTotalFinal()) + "\nForma de pago: " + this.valor(pedido.getPago() != null ? pedido.getPago().getMetodo() : null) + "\nEntrega: " + this.valor(pedido.getEnvio() != null ? pedido.getEnvio().getDireccionEnvio() : null) + (String)(panelUrl != null ? "\nPanel del pedido: " + panelUrl : "");
        this.enviarComoTienda("EMAIL_VENTA_REVENDEDOR", pedido, this.emailComercialRevendedor(pedido), "\u00a1Vendiste! Pedido #" + pedido.getId().toString().substring(0, 8).toUpperCase(), cuerpo, Map.of("actorDestino", "REVENDEDOR"));
    }

    @Transactional(readOnly=true)
    public void notificarEntregaConfirmada(EventoEnvioEntregado evento) {
        Pedido pedido = this.pedidoRepository.findById(evento.pedidoId()).orElse(null);
        if (pedido == null || pedido.getRevendedor() == null || pedido.getRevendedor().getUsuario() == null) {
            return;
        }
        String panelUrl = this.resolverPanelPedidoUrl(pedido);
        String cuerpo = "El cliente confirmo que recibio correctamente su compra.\nPedido: #" + pedido.getId().toString().substring(0, 8).toUpperCase() + "\nCliente: " + this.valor(pedido.getCliente() != null ? pedido.getCliente().getNombre() : null) + "\nEstado: entregado" + (String)(panelUrl != null ? "\nPanel del pedido: " + panelUrl : "");
        this.enviarComoTienda("EMAIL_ENTREGA_REVENDEDOR", pedido, this.emailComercialRevendedor(pedido), "El producto fue entregado - Pedido #" + pedido.getId().toString().substring(0, 8).toUpperCase(), cuerpo, Map.of("actorDestino", "REVENDEDOR"));
    }

    @Transactional(readOnly=true)
    public void notificarComprobanteTransferencia(EventoComprobanteTransferenciaCargado evento) {
        Pedido pedido = this.pedidoRepository.findById(evento.pedidoId()).orElse(null);
        if (pedido == null || pedido.getRevendedor() == null || evento.revendedorId() == null || !evento.revendedorId().equals(pedido.getRevendedor().getId())) {
            return;
        }
        String panelUrl = this.resolverPanelPedidoUrl(evento.revendedorDominio(), evento.pedidoId());
        String cuerpo = "El cliente cargo un comprobante de transferencia.\nPedido: " + String.valueOf(evento.pedidoId()) + "\nEstado: pendiente de revision" + (String)(panelUrl != null ? "\nPanel del pedido: " + panelUrl : "");
        this.enviarComoTienda("EMAIL_INTERNO_COMPROBANTE_TRANSFERENCIA", pedido, evento.revendedorEmail(), "Comprobante pendiente de revision - Pedido " + String.valueOf(evento.pedidoId()), cuerpo, Map.of("actorDestino", "REVENDEDOR"));
    }

    private void enviarComoTienda(String tipoAuditoria, Pedido pedido, String destinatario, String subject, String textBody, Map<String, String> metadata) {
        if (!StringUtils.hasText(destinatario)) {
            this.registrarAuditoriaSegura(tipoAuditoria + "_OMITIDO", pedido.getId(), Map.of("motivo", "DESTINATARIO_FALTANTE", "subject", subject));
            return;
        }
        try {
            BrandingPostCompra branding = this.brandingResolver.resolver(pedido.getRevendedor().getId());
            if (!this.smtpPropioConfigurado(branding)) {
                this.registrarAuditoriaSegura(tipoAuditoria + "_OMITIDO", pedido.getId(), Map.of("motivo", "SMTP_REVENDEDOR_NO_CONFIGURADO", "subject", subject));
                return;
            }
            this.emailSender.send(MensajeEmail.builder().to(destinatario.trim()).subject(subject).textBody(textBody).htmlBody(this.renderHtmlTienda(branding, textBody)).fromName(branding.nombreRemitente()).fromEmail(branding.emailRemitente()).replyTo(branding.emailRemitente()).smtpHost(branding.smtpHost()).smtpPort(branding.smtpPort()).smtpUsername(branding.smtpUsername()).smtpPassword(branding.smtpPassword()).smtpAuth(branding.smtpAuth()).smtpStartTls(branding.smtpStartTls()).metadata(metadata).build());
            this.registrarAuditoriaSegura(tipoAuditoria + "_ENVIADO", pedido.getId(), Map.of("destinatario", destinatario, "subject", subject));
        }
        catch (Exception ex) {
            log.warn("Fallo envio de email de tienda tipo={} pedidoId={} destinatario={}", tipoAuditoria, pedido.getId(), destinatario, ex);
            this.registrarAuditoriaSegura(tipoAuditoria + "_ERROR", pedido.getId(), Map.of("destinatario", destinatario, "subject", subject, "error", ex.getMessage()));
        }
    }

    private boolean smtpPropioConfigurado(BrandingPostCompra branding) {
        return branding != null && StringUtils.hasText(branding.smtpHost()) && StringUtils.hasText(branding.smtpUsername()) && StringUtils.hasText(branding.smtpPassword());
    }

    private String renderHtmlTienda(BrandingPostCompra branding, String textBody) {
        String logo = StringUtils.hasText(branding.logoUrl()) ? "<img src='" + this.escapeHtml(branding.logoUrl()) + "' alt='" + this.escapeHtml(branding.nombreComercial()) + "' style='max-height:52px;max-width:180px;background:#fff;border-radius:10px;padding:6px;margin-bottom:14px'>" : "";
        return "<div style='font-family:Arial,sans-serif;background:#f1f5f9;padding:24px;color:#0f172a'><div style='max-width:640px;margin:auto;background:#fff;border-radius:20px;overflow:hidden;border:1px solid #e2e8f0'><div style='padding:24px;background:" + this.escapeHtml(branding.colorPrimario()) + ";color:#fff'>" + logo + "<strong>" + this.escapeHtml(branding.nombreComercial()) + "</strong></div><div style='padding:26px'>" + this.renderHtmlInterno(textBody) + "</div></div></div>";
    }

    private String resolverPanelPedidoUrl(Pedido pedido) {
        if (pedido.getRevendedor() == null || !StringUtils.hasText(pedido.getRevendedor().getDominio())) {
            return null;
        }
        return this.resolverPanelPedidoUrl(pedido.getRevendedor().getDominio(), pedido.getId());
    }

    private String resolverPanelPedidoUrl(String dominio, UUID pedidoId) {
        if (!StringUtils.hasText(dominio) || pedidoId == null) {
            return null;
        }
        String host = dominio.trim().replaceFirst("^https?://", "").replaceFirst("/.*$", "");
        return "https://" + host + "/mi-tienda?tab=pedidos&pedido=" + String.valueOf(pedidoId);
    }

    private String renderHtmlInterno(String textBody) {
        StringBuilder html = new StringBuilder("<div style='font-family:Arial,sans-serif;color:#0f172a;line-height:1.6'>");
        for (String line : textBody.split("\\n")) {
            if (line.startsWith("Panel del pedido: ")) {
                String url = line.substring("Panel del pedido: ".length());
                html.append("<p><a style='display:inline-block;background:#0f172a;color:#fff;padding:12px 18px;border-radius:999px;text-decoration:none;font-weight:700' href='").append(this.escapeHtml(url)).append("'>Abrir pedido en la tienda</a></p>");
                continue;
            }
            html.append("<p style='margin:6px 0'>").append(this.escapeHtml(line)).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String nombreProducto(PedidoItem item) {
        if (item.getRevendedorProducto() != null && StringUtils.hasText(item.getRevendedorProducto().getNombre())) {
            return item.getRevendedorProducto().getNombre();
        }
        if (item.getProducto() != null && StringUtils.hasText(item.getProducto().getNombre())) {
            return item.getProducto().getNombre();
        }
        return "Producto";
    }

    private String emailComercialRevendedor(Pedido pedido) {
        if (pedido.getRevendedor() != null && pedido.getRevendedor().getStorefront() != null && StringUtils.hasText(pedido.getRevendedor().getStorefront().getSmtpUsername())) {
            return pedido.getRevendedor().getStorefront().getSmtpUsername().trim();
        }
        if (pedido.getRevendedor() != null && pedido.getRevendedor().getStorefront() != null && StringUtils.hasText(pedido.getRevendedor().getStorefront().getEmailComercial())) {
            return pedido.getRevendedor().getStorefront().getEmailComercial().trim();
        }
        return pedido.getRevendedor() != null && pedido.getRevendedor().getUsuario() != null ? pedido.getRevendedor().getUsuario().getEmail() : null;
    }

    private String valor(Object value) {
        return value != null && StringUtils.hasText(value.toString()) ? value.toString() : "No informado";
    }
}
