package ar.com.mercadodigital.backend.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import ar.com.mercadodigital.backend.entity.ConsultaDisponibilidad;
import ar.com.mercadodigital.backend.entity.ProductoPregunta;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompra;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompraResolver;
import ar.com.mercadodigital.backend.service.postcompra.EmailSender;
import ar.com.mercadodigital.backend.service.postcompra.MensajeEmail;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionConsultaEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionConsultaEmailService.class);

    private final EmailSender emailSender;
    private final BrandingPostCompraResolver brandingResolver;
    private final RevendedorStorefrontRepository storefrontRepository;

    public void notificarNuevaPregunta(ProductoPregunta pregunta) {
        enviarSinInterrumpir(() -> {
            UUID revendedorId = pregunta.getRevendedor().getId();
            BrandingPostCompra branding = brandingResolver.resolver(revendedorId);
            String destinatario = resolverEmailRevendedor(pregunta.getRevendedor());
            String producto = pregunta.getRevendedorProducto().getNombre();
            String asunto = "Nueva consulta por " + producto;
            String texto = "Recibiste una nueva consulta en " + branding.nombreComercial() + ".\n\n"
                    + "Producto: " + producto + "\n"
                    + "Cliente: " + valor(pregunta.getNombreContacto(), "Sin nombre") + "\n"
                    + "Email: " + valor(pregunta.getEmailContacto(), "No informado") + "\n"
                    + "Telefono: " + valor(pregunta.getTelefonoContacto(), "No informado") + "\n\n"
                    + "Consulta:\n" + pregunta.getPregunta() + "\n\n"
                    + "Responde desde: " + branding.baseUrlPublica() + "/mi-tienda";
            String html = plantilla(branding, "Nueva consulta de producto",
                    "<p><strong>Producto:</strong> " + esc(producto) + "</p>"
                            + datosContacto(pregunta.getNombreContacto(), pregunta.getEmailContacto(), pregunta.getTelefonoContacto())
                            + "<div style=\"margin:20px 0;padding:16px;background:#f8fafc;border-radius:12px\">"
                            + esc(pregunta.getPregunta()) + "</div>"
                            + boton(branding.baseUrlPublica() + "/mi-tienda", "Ver y responder consulta"));
            enviar(branding, destinatario, asunto, html, texto, pregunta.getEmailContacto(),
                    Map.of("tipo", "CONSULTA_PRODUCTO", "referenciaId", pregunta.getId().toString()));
        }, "consulta-producto", pregunta.getId());
    }

    public void notificarNuevaDisponibilidad(ConsultaDisponibilidad consulta) {
        enviarSinInterrumpir(() -> {
            UUID revendedorId = consulta.getRevendedor().getId();
            BrandingPostCompra branding = brandingResolver.resolver(revendedorId);
            String destinatario = resolverEmailRevendedor(consulta.getRevendedor());
            String producto = consulta.getRevendedorProducto().getNombre();
            String asunto = "Nueva solicitud de disponibilidad: " + producto;
            String texto = "Recibiste una solicitud de disponibilidad en " + branding.nombreComercial() + ".\n\n"
                    + "Producto: " + producto + "\n"
                    + "Cliente: " + valor(consulta.getNombreContacto(), "Sin nombre") + "\n"
                    + "Localidad: " + consulta.getLocalidad() + "\n"
                    + "Email: " + valor(consulta.getEmailContacto(), "No informado") + "\n"
                    + "Telefono: " + valor(consulta.getTelefonoContacto(), "No informado") + "\n\n"
                    + "Responde desde: " + branding.baseUrlPublica() + "/mi-tienda";
            String html = plantilla(branding, "Solicitud de disponibilidad",
                    "<p><strong>Producto:</strong> " + esc(producto) + "</p>"
                            + "<p><strong>Localidad:</strong> " + esc(consulta.getLocalidad()) + "</p>"
                            + datosContacto(consulta.getNombreContacto(), consulta.getEmailContacto(), consulta.getTelefonoContacto())
                            + (StringUtils.hasText(consulta.getObservaciones())
                                    ? "<div style=\"margin:20px 0;padding:16px;background:#f8fafc;border-radius:12px\">"
                                            + esc(consulta.getObservaciones()) + "</div>"
                                    : "")
                            + boton(branding.baseUrlPublica() + "/mi-tienda", "Responder disponibilidad"));
            enviar(branding, destinatario, asunto, html, texto, consulta.getEmailContacto(),
                    Map.of("tipo", "CONSULTA_DISPONIBILIDAD", "referenciaId", consulta.getId().toString()));
        }, "consulta-disponibilidad", consulta.getId());
    }

    public void notificarRespuestaPregunta(ProductoPregunta pregunta) {
        if (!StringUtils.hasText(pregunta.getEmailContacto())) {
            return;
        }
        this.enviarSinInterrumpir(() -> {
            BrandingPostCompra branding = this.brandingResolver.resolver(pregunta.getRevendedor().getId());
            String producto = pregunta.getRevendedorProducto().getNombre();
            String asunto = branding.nombreComercial() + " respondio tu consulta";
            String texto = "Consulta sobre " + producto + ":\n" + pregunta.getPregunta() + "\n\nRespuesta:\n" + pregunta.getRespuesta();
            String html = this.plantilla(branding, "Respondimos tu consulta", "<p><strong>Producto:</strong> " + this.esc(producto) + "</p><p style=\"color:#64748b\">" + this.esc(pregunta.getPregunta()) + "</p><div style=\"margin:20px 0;padding:16px;background:#ecfdf5;border-radius:12px\">" + this.esc(pregunta.getRespuesta()) + "</div>");
            this.enviar(branding, pregunta.getEmailContacto(), asunto, html, texto, branding.emailRemitente(), Map.of("tipo", "RESPUESTA_CONSULTA_PRODUCTO", "referenciaId", pregunta.getId().toString()));
        }, "respuesta-consulta-producto", pregunta.getId());
    }

    public void notificarRespuestaDisponibilidad(ConsultaDisponibilidad consulta) {
        if (!StringUtils.hasText(consulta.getEmailContacto())) {
            return;
        }
        this.enviarSinInterrumpir(() -> {
            BrandingPostCompra branding = this.brandingResolver.resolver(consulta.getRevendedor().getId());
            String producto = consulta.getRevendedorProducto().getNombre();
            String asunto = branding.nombreComercial() + " respondio tu solicitud";
            Object respuesta = StringUtils.hasText(consulta.getRespuestaProveedor()) ? consulta.getRespuestaProveedor() : "Estado: " + consulta.getEstado();
            String texto = "Solicitud de disponibilidad sobre " + producto + ".\n\nRespuesta:\n" + (String)respuesta;
            String html = this.plantilla(branding, "Respondimos tu solicitud de disponibilidad", "<p><strong>Producto:</strong> " + this.esc(producto) + "</p><div style=\"margin:20px 0;padding:16px;background:#ecfdf5;border-radius:12px\">" + this.esc((String)respuesta) + "</div>");
            this.enviar(branding, consulta.getEmailContacto(), asunto, html, texto, branding.emailRemitente(), Map.of("tipo", "RESPUESTA_DISPONIBILIDAD", "referenciaId", consulta.getId().toString()));
        }, "respuesta-disponibilidad", consulta.getId());
    }

    public void enviarPrueba(UUID revendedorId) {
        RevendedorStorefront storefront = this.storefrontRepository.findByRevendedor_Id(revendedorId).orElseThrow(() -> new BadRequestException("La tienda no tiene configuracion de correo"));
        BrandingPostCompra branding = this.brandingResolver.resolver(revendedorId);
        if (!(StringUtils.hasText(branding.smtpHost()) && StringUtils.hasText(branding.smtpUsername()) && StringUtils.hasText(branding.smtpPassword()))) {
            throw new BadRequestException("Completa servidor, usuario y clave SMTP antes de enviar la prueba");
        }
        String destinatario = branding.smtpUsername().trim();
        String html = this.plantilla(branding, "El correo de tu tienda funciona", "<p>Esta prueba confirma que las notificaciones de consultas pueden enviarse correctamente.</p>");
        this.enviar(branding, destinatario, "Prueba de correo de " + branding.nombreComercial(), html, "El correo de tu tienda funciona correctamente.", destinatario, Map.of("tipo", "PRUEBA_CORREO_STOREFRONT", "referenciaId", revendedorId.toString()));
    }

    private void enviar(BrandingPostCompra branding, String to, String subject, String html, String text, String replyTo, Map<String, String> metadata) {
        this.emailSender.send(MensajeEmail.builder().to(to.trim()).subject(subject).htmlBody(html).textBody(text).fromName(branding.nombreRemitente()).fromEmail(branding.emailRemitente()).replyTo(StringUtils.hasText(replyTo) ? replyTo.trim() : branding.emailRemitente()).smtpHost(branding.smtpHost()).smtpPort(branding.smtpPort()).smtpUsername(branding.smtpUsername()).smtpPassword(branding.smtpPassword()).smtpAuth(branding.smtpAuth()).smtpStartTls(branding.smtpStartTls()).metadata(metadata).build());
    }

    private String resolverEmailRevendedor(Revendedor revendedor) {
        RevendedorStorefront storefront = this.storefrontRepository.findByRevendedor_Id(revendedor.getId()).orElse(null);
        if (storefront != null && StringUtils.hasText(storefront.getSmtpUsername())) {
            return storefront.getSmtpUsername().trim();
        }
        if (storefront != null && StringUtils.hasText(storefront.getEmailComercial())) {
            return storefront.getEmailComercial().trim();
        }
        if (revendedor.getUsuario() != null && StringUtils.hasText(revendedor.getUsuario().getEmail())) {
            return revendedor.getUsuario().getEmail().trim();
        }
        throw new BadRequestException("Configura un email comercial para recibir notificaciones");
    }

    private void enviarSinInterrumpir(Runnable envio, String tipo, UUID referenciaId) {
        try {
            envio.run();
            log.info("Email de consulta enviado. tipo={} referenciaId={}", tipo, referenciaId);
        } catch (Exception exception) {
            log.warn("No se pudo enviar email de consulta. tipo={} referenciaId={}", tipo, referenciaId, exception);
        }
    }

    private String plantilla(BrandingPostCompra branding, String titulo, String contenido) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:640px;margin:auto;color:#0f172a\">"
                + "<div style=\"height:5px;background:" + esc(branding.colorPrimario()) + ";border-radius:8px\"></div>"
                + "<h1 style=\"font-size:24px;margin:24px 0 8px\">" + esc(titulo) + "</h1>"
                + "<p style=\"color:#64748b;margin-top:0\">" + esc(branding.nombreComercial()) + "</p>"
                + contenido + "</div>";
    }

    private String datosContacto(String nombre, String email, String telefono) {
        return "<p><strong>Cliente:</strong> " + esc(valor(nombre, "Sin nombre")) + "<br>"
                + "<strong>Email:</strong> " + esc(valor(email, "No informado")) + "<br>"
                + "<strong>Telefono:</strong> " + esc(valor(telefono, "No informado")) + "</p>";
    }

    private String boton(String url, String texto) {
        return "<p style=\"margin-top:24px\"><a href=\"" + esc(url)
                + "\" style=\"display:inline-block;padding:12px 18px;border-radius:10px;background:#0f172a;color:white;text-decoration:none\">"
                + esc(texto) + "</a></p>";
    }

    private String valor(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
