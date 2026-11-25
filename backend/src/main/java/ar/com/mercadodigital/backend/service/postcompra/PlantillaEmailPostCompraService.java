package ar.com.mercadodigital.backend.service.postcompra;

import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompra;
import ar.com.mercadodigital.backend.service.postcompra.ContenidoEmail;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Currency;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class PlantillaEmailPostCompraService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ContenidoEmail renderizarPedidoCreado(EventoPedidoCreado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Pedido recibido " + String.valueOf(evento.pedidoId());
        String titulo = "Recibimos tu pedido";
        String mensaje = "Tu pedido fue recibido correctamente. Te avisaremos cuando se confirme el pago y con cada novedad del envio.";
        String detalleMonto = this.formatCurrency(evento.total(), evento.moneda());
        return new ContenidoEmail(asunto, this.renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoPedido(), detalleMonto, evento.trackingToken(), trackingUrl, evento.fechaEvento()), this.renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoPedido(), detalleMonto, evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarPagoConfirmado(EventoPagoConfirmado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String pedidoCorto = "#" + evento.pedidoId().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String asunto = branding.nombreComercial() + " - \u00a1Gracias por tu compra! Pago confirmado " + pedidoCorto;
        String titulo = "\u00a1Pago confirmado!";
        String mensaje = "Gracias por elegirnos. Recibimos correctamente tu pago y ya estamos preparando tu compra.";
        String detalleMonto = this.formatCurrency(evento.montoConfirmado(), evento.moneda());
        return new ContenidoEmail(asunto, this.renderHtml(branding, titulo, mensaje, pedidoCorto, "En preparacion", detalleMonto, evento.trackingToken(), trackingUrl, evento.fechaConfirmacion()), this.renderText(branding, titulo, mensaje, pedidoCorto, "En preparacion", detalleMonto, evento.trackingToken(), trackingUrl, evento.fechaConfirmacion()));
    }

    @Transactional(readOnly=true)
    public ContenidoEmail renderizarEnvioDespachado(EventoEnvioDespachado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Tu pedido esta en camino";
        ProductoEmail producto = this.resolverProductoEmail(evento.pedidoId());
        String logoHtml = this.buildLogoHtml(branding);
        String imageHtml = producto.imagenUrl() != null ? "<img src=\"" + this.escapeHtml(producto.imagenUrl()) + "\" alt=\"" + this.escapeHtml(producto.nombre()) + "\" style=\"width:92px;height:92px;object-fit:contain;border-radius:14px;background:#fff;border:1px solid #e2e8f0;padding:6px;\" />" : "";
        String transportista = this.hasText(evento.transportistaNombre()) ? evento.transportistaNombre() : "Entrega coordinada por la tienda";
        String tracking = this.hasText(evento.trackingLogistica()) ? evento.trackingLogistica() : "A confirmar";
        String html = "<div style=\"font-family:Arial,sans-serif;background:#f1f5f9;padding:24px;color:#0f172a;\">\n  <div style=\"max-width:640px;margin:0 auto;background:#fff;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,.08);\">\n    <div style=\"padding:24px;background:%s;color:#fff;\">%s<p style=\"margin:0;font-size:13px;opacity:.85;\">%s</p><h1 style=\"margin:8px 0 0;font-size:25px;\">Tu pedido est\u00e1 en camino</h1></div>\n    <div style=\"padding:28px;\">\n      <p style=\"margin:0 0 18px;line-height:1.6;color:#475569;\">Despachamos tu compra. Estos son los datos \u00fatiles para seguir la entrega.</p>\n      <div style=\"display:flex;gap:16px;align-items:center;padding:16px;border:1px solid #e2e8f0;border-radius:16px;background:#f8fafc;\">%s<div><p style=\"margin:0;font-size:16px;font-weight:700;\">%s</p><p style=\"margin:8px 0 0;color:#475569;\"><strong>Transportista:</strong> %s</p><p style=\"margin:5px 0 0;color:#475569;\"><strong>C\u00f3digo de seguimiento:</strong> %s</p></div></div>\n      <a href=\"%s\" style=\"display:inline-block;background:%s;color:#fff;text-decoration:none;padding:13px 20px;border-radius:999px;font-weight:700;margin-top:22px;\">Seguir mi pedido</a>\n    </div>%s\n  </div>\n</div>\n".formatted(this.escapeHtml(branding.colorPrimario()), logoHtml, this.escapeHtml(branding.nombreComercial()), imageHtml, this.escapeHtml(producto.nombre()), this.escapeHtml(transportista), this.escapeHtml(tracking), this.escapeHtml(trackingUrl), this.escapeHtml(branding.colorPrimario()), this.buildContactoHtml(branding));
        String text = "%s\n\nTu pedido est\u00e1 en camino\nProducto: %s\nTransportista: %s\nC\u00f3digo de seguimiento: %s\n\nSeguir mi pedido: %s\n\n%s\n".formatted(branding.nombreComercial(), producto.nombre(), transportista, tracking, trackingUrl, this.buildFirmaTienda(branding));
        return new ContenidoEmail(asunto, html, text);
    }

    public ContenidoEmail renderizarEnvioEntregado(EventoEnvioEntregado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - \u00a1Gracias por tu compra! Pedido entregado";
        String titulo = "\u00a1Gracias por tu compra!";
        String mensaje = "La entrega fue confirmada. Esperamos que disfrutes mucho tu producto.";
        String detalleMonto = "Si necesitas ayuda o gestionar la garantia, la tienda sigue disponible desde el enlace de tu pedido.";
        return new ContenidoEmail(asunto, this.renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoEnvio(), detalleMonto, evento.trackingToken(), trackingUrl, evento.fechaEvento()), this.renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoEnvio(), detalleMonto, evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarEnvioListoParaRetiro(EventoEnvioListoParaRetiro evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Estamos preparando tu pedido";
        String titulo = "Estamos preparando tu pedido";
        String mensaje = "Tu pedido ya entro en la etapa final de preparacion para despacho.";
        String detalle = "Seguiremos avisandote desde este mismo enlace de tracking.";

        return new ContenidoEmail(
                asunto,
                renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoEnvio(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()),
                renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoEnvio(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarEnvioRetirado(EventoEnvioRetirado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Tu pedido sigue avanzando";
        String titulo = "Tu pedido sigue avanzando";
        String mensaje = "Tu pedido ya fue retirado para continuar su circuito logistico.";
        String detalle = "Puedes seguir su estado en tiempo real desde el mismo enlace de seguimiento.";

        return new ContenidoEmail(
                asunto,
                renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoEnvio(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()),
                renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoEnvio(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarRmaSolicitado(EventoRmaSolicitado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Recibimos tu reclamo";
        String titulo = "Recibimos tu reclamo";
        String mensaje = "Tu caso de postventa ya fue registrado. Nuestro equipo lo revisara y cualquier novedad quedara en el mismo enlace del pedido.";
        String detalle = "Caso: " + evento.rmaId() + " | Producto: " + evento.productoNombre();

        return new ContenidoEmail(
                asunto,
                renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()),
                renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarRmaEnvioSolicitado(EventoRmaEnvioSolicitado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Ya puedes enviar el producto";
        String titulo = "Ya puedes enviar el producto";
        String mensaje = "Aprobamos tu caso y ya tienes disponible el rotulo de devolucion desde el seguimiento del pedido.";
        String detalle = "Producto: " + evento.productoNombre() + ". Ingresa al enlace y descarga el rotulo para pegarlo en el paquete.";

        return new ContenidoEmail(
                asunto,
                renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()),
                renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarRmaCerrado(EventoRmaCerrado evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Tu caso fue resuelto";
        String titulo = "Tu caso fue resuelto";
        String mensaje = "Cerramos tu reclamo. Puedes volver al mismo enlace para revisar el resumen final del caso.";
        String detalle = buildDetalleCierreRma(evento);

        return new ContenidoEmail(
                asunto,
                renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()),
                renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarRmaReposicionReenviada(EventoRmaReposicionReenviada evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Tu reemplazo ya esta en camino";
        String titulo = "Tu reemplazo ya esta en camino";
        String mensaje = evento.transportistaNombre() != null && !evento.transportistaNombre().isBlank()
                ? "Ya reenviamos tu producto de reemplazo. Transportista: " + evento.transportistaNombre() + "."
                : "Ya reenviamos tu producto de reemplazo.";
        String detalle = evento.trackingReposicion() != null && !evento.trackingReposicion().isBlank()
                ? "Tracking del reemplazo: " + evento.trackingReposicion()
                : "El seguimiento del reemplazo ya esta disponible desde el enlace del pedido.";

        return new ContenidoEmail(
                asunto,
                renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()),
                renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    public ContenidoEmail renderizarRmaReposicionEntregada(EventoRmaReposicionEntregada evento, BrandingPostCompra branding) {
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String asunto = branding.nombreComercial() + " - Confirmamos la entrega de tu reemplazo";
        String titulo = "Tu reemplazo fue entregado";
        String mensaje = "Confirmamos la entrega final del producto de reemplazo. Puedes conservar este mismo enlace por cualquier seguimiento posterior.";
        String detalle = evento.trackingReposicion() != null && !evento.trackingReposicion().isBlank()
                ? "Tracking final del reemplazo: " + evento.trackingReposicion()
                : "El reemplazo ya figura como entregado en el seguimiento del pedido.";

        return new ContenidoEmail(
                asunto,
                renderHtml(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()),
                renderText(branding, titulo, mensaje, evento.pedidoId().toString(), evento.estadoRma(), detalle,
                        evento.trackingToken(), trackingUrl, evento.fechaEvento()));
    }

    private String renderHtml(BrandingPostCompra branding, String titulo, String mensaje, String pedidoId, String estadoPedido, String monto, String trackingToken, String trackingUrl, LocalDateTime fechaEvento) {
        String logoHtml = this.buildLogoHtml(branding);
        String problemaUrl = this.buildWhatsAppUrl(branding, pedidoId);
        String problemaButton = problemaUrl != null ? "<a href=\"" + this.escapeHtml(problemaUrl) + "\" style=\"display:inline-block;border:1px solid #cbd5e1;color:#334155;text-decoration:none;padding:12px 18px;border-radius:999px;font-weight:700;margin:6px 8px 0 0;\">Tengo un problema</a>" : "";
        return "<div style=\"font-family:Arial,sans-serif;background:#f1f5f9;padding:24px;color:#0f172a;\">\n  <div style=\"max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,.08);\">\n    <div style=\"padding:24px;background:%s;color:#ffffff;\">\n      %s\n      <p style=\"margin:0;font-size:13px;opacity:.85;\">%s</p>\n      <h1 style=\"margin:8px 0 0;font-size:25px;\">%s</h1>\n    </div>\n    <div style=\"padding:28px;\">\n      <p style=\"margin-top:0;font-size:17px;font-weight:700;\">\u00a1Gracias por tu compra!</p>\n      <p style=\"line-height:1.6;color:#475569;\">%s</p>\n      <div style=\"margin:22px 0;padding:18px;border:1px solid #e2e8f0;border-radius:16px;background:#f8fafc;\">\n        <p style=\"margin:0 0 8px 0;\"><strong>Pedido:</strong> %s</p>\n        <p style=\"margin:0 0 8px 0;\"><strong>Estado:</strong> %s</p>\n        <p style=\"margin:0 0 8px 0;\"><strong>Total:</strong> %s</p>\n        <p style=\"margin:0;\"><strong>Fecha:</strong> %s</p>\n      </div>\n      <a href=\"%s\" style=\"display:inline-block;background:%s;color:#ffffff;text-decoration:none;padding:13px 20px;border-radius:999px;font-weight:700;margin:6px 8px 0 0;\">Seguir mi pedido</a>\n      %s\n    </div>\n    %s\n  </div>\n</div>\n".formatted(this.escapeHtml(branding.colorPrimario()), logoHtml, this.escapeHtml(branding.nombreComercial()), this.escapeHtml(titulo), this.escapeHtml(mensaje), this.escapeHtml(pedidoId), this.escapeHtml(estadoPedido), this.escapeHtml(monto), this.escapeHtml(this.formatDate(fechaEvento)), this.escapeHtml(trackingUrl), this.escapeHtml(branding.colorPrimario()), problemaButton, this.buildContactoHtml(branding));
    }

    private String renderText(BrandingPostCompra branding, String titulo, String mensaje, String pedidoId, String estadoPedido, String monto, String trackingToken, String trackingUrl, LocalDateTime fechaEvento) {
        return "%s\n\n%s\n\n%s\nPedido: %s\nEstado: %s\nTotal: %s\nFecha: %s\n\nSeguimiento:\n%s\n\n%s\n\n%s\n".formatted(branding.nombreComercial(), titulo, mensaje, pedidoId, estadoPedido, monto, this.formatDate(fechaEvento), trackingUrl, this.buildWhatsAppUrl(branding, pedidoId) != null ? "Tengo un problema: " + this.buildWhatsAppUrl(branding, pedidoId) : "", this.buildFirmaTienda(branding));
    }

    private String buildDetalleCierreRma(EventoRmaCerrado evento) {
        String resolucion = evento.resolucionFinal() != null ? evento.resolucionFinal() : evento.estadoRma();
        if ("REEMBOLSADO".equalsIgnoreCase(resolucion)) {
            String base = "Resolucion: devolucion de dinero.";
            if (evento.montoCompensacion() != null) {
                base += " Importe: " + formatCurrency(evento.montoCompensacion(), "ARS") + ".";
            }
            if (evento.referenciaCompensacion() != null && !evento.referenciaCompensacion().isBlank()) {
                base += " Referencia: " + evento.referenciaCompensacion() + ".";
            }
            return base;
        }
        if ("RECHAZADO".equalsIgnoreCase(resolucion)) {
            return "Resolucion: reclamo rechazado.";
        }
        return "Resolucion: caso cerrado con solucion operativa.";
    }

    private String formatCurrency(BigDecimal amount, String currencyCode) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        try {
            formatter.setCurrency(java.util.Currency.getInstance(currencyCode != null ? currencyCode : "ARS"));
        } catch (IllegalArgumentException ignored) {
            formatter.setCurrency(java.util.Currency.getInstance("ARS"));
        }
        return formatter.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private String formatDate(LocalDateTime value) {
        return (value != null ? value : LocalDateTime.now()).format(DATE_TIME_FORMATTER);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private final PedidoRepository pedidoRepository;
    private final ProductoMediaRepository productoMediaRepository;

    public PlantillaEmailPostCompraService() {
        this(null, null);
    }

    @Autowired
    public PlantillaEmailPostCompraService(PedidoRepository pedidoRepository, ProductoMediaRepository productoMediaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoMediaRepository = productoMediaRepository;
    }

    public ContenidoEmail renderizarTransferenciaSolicitada(EventoTransferenciaSolicitada evento, BrandingPostCompra branding) {
        String uploadUrl = branding.baseUrlPublica() + "/transferencia/" + evento.transferenciaToken();
        String trackingUrl = branding.trackingUrl(evento.trackingToken());
        String total = this.formatCurrency(evento.total(), "ARS");
        String titular = evento.titular() != null && !evento.titular().isBlank() ? "<p style=\"margin:6px 0 0;color:#475569;\">Titular: " + this.escapeHtml(evento.titular()) + "</p>" : "";
        String problemaUrl = this.buildWhatsAppUrl(branding, evento.pedidoId().toString());
        String logoHtml = this.buildLogoHtml(branding);
        String contactoHtml = this.buildContactoHtml(branding);
        String problemaButton = problemaUrl != null ? "<a href=\"" + this.escapeHtml(problemaUrl) + "\" style=\"display:inline-block;border:1px solid #cbd5e1;color:#334155;text-decoration:none;padding:12px 18px;border-radius:999px;font-weight:700;margin:6px 8px 0 0;\">Tengo un problema</a>" : "";
        String html = "<div style=\"font-family:Arial,sans-serif;background:#f1f5f9;padding:24px;color:#0f172a;\">\n  <div style=\"max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,.08);\">\n    <div style=\"padding:24px;background:%s;color:#ffffff;\">%s<p style=\"margin:0;font-size:13px;opacity:.85;\">%s</p><h1 style=\"margin:8px 0 0;font-size:25px;\">Datos para tu transferencia</h1></div>\n    <div style=\"padding:28px;\">\n      <p style=\"margin-top:0;font-size:16px;\">Hola %s, gracias por comprar en <strong>%s</strong>.</p>\n      <p style=\"line-height:1.6;color:#475569;\">Transfer\u00ed el importe indicado y luego adjunt\u00e1 el comprobante para que la tienda pueda verificarlo.</p>\n      <div style=\"margin:22px 0;padding:20px;border:1px solid #dbeafe;border-radius:16px;background:#eff6ff;\">\n        <p style=\"margin:0 0 6px;color:#64748b;font-size:12px;text-transform:uppercase;letter-spacing:.08em;\">Total a transferir</p>\n        <p style=\"margin:0;font-size:26px;font-weight:800;\">%s</p>\n        <p style=\"margin:18px 0 6px;color:#64748b;font-size:12px;text-transform:uppercase;letter-spacing:.08em;\">Alias</p>\n        <p style=\"margin:0;font-size:22px;font-weight:800;\">%s</p>%s\n      </div>\n      <a href=\"%s\" style=\"display:inline-block;background:%s;color:#ffffff;text-decoration:none;padding:13px 20px;border-radius:999px;font-weight:700;margin:6px 8px 0 0;\">Cargar comprobante</a>\n      <a href=\"%s\" style=\"display:inline-block;border:1px solid #cbd5e1;color:#334155;text-decoration:none;padding:12px 18px;border-radius:999px;font-weight:700;margin:6px 8px 0 0;\">Seguir mi pedido</a>\n      %s\n    </div>\n    %s\n  </div>\n</div>\n".formatted(this.escapeHtml(branding.colorPrimario()), logoHtml, this.escapeHtml(branding.nombreComercial()), this.escapeHtml(evento.clienteNombre()), this.escapeHtml(branding.nombreComercial()), this.escapeHtml(total), this.escapeHtml(evento.alias()), titular, this.escapeHtml(uploadUrl), this.escapeHtml(branding.colorPrimario()), this.escapeHtml(trackingUrl), problemaButton, contactoHtml);
        String text = "Hola " + evento.clienteNombre() + ", gracias por comprar en " + branding.nombreComercial() + ".\n\nTotal a transferir: " + total + "\nAlias: " + evento.alias() + (String)(evento.titular() != null && !evento.titular().isBlank() ? "\nTitular: " + evento.titular() : "") + "\n\nCargar comprobante: " + uploadUrl + "\nSeguir pedido: " + trackingUrl + (String)(problemaUrl != null ? "\nTengo un problema: " + problemaUrl : "") + "\n\n" + this.buildFirmaTienda(branding);
        return new ContenidoEmail(branding.nombreComercial() + " - Datos para tu transferencia", html, text);
    }

    private ProductoEmail resolverProductoEmail(UUID pedidoId) {
        if (this.pedidoRepository == null) {
            return new ProductoEmail("Producto de tu compra", null);
        }
        PedidoItem item = this.pedidoRepository.findById(pedidoId).stream().flatMap(pedido -> pedido.getItems().stream()).findFirst().orElse(null);
        if (item == null) {
            return new ProductoEmail("Producto de tu compra", null);
        }
        if (item.getRevendedorProducto() != null) {
            String nombre = item.getRevendedorProducto().getNombre();
            String imagen = item.getRevendedorProducto().getMedia().stream().filter(media -> TipoMedia.IMAGEN.equals((Object)media.getTipoMedia())).sorted(Comparator.comparing(RevendedorProductoMedia::isEsPrincipal).reversed().thenComparing(RevendedorProductoMedia::getOrden)).map(RevendedorProductoMedia::getUrl).findFirst().orElse(null);
            return new ProductoEmail(nombre, imagen);
        }
        if (item.getProducto() != null) {
            String nombre = item.getProducto().getProductoCanonico() != null ? item.getProducto().getProductoCanonico().getNombrePublico() : item.getProducto().getNombre();
            String imagen = this.productoMediaRepository == null ? null : (String)this.productoMediaRepository.findByProducto_IdOrderByOrdenAsc(item.getProducto().getId()).stream().filter(media -> TipoMedia.IMAGEN.equals((Object)media.getTipoMedia())).sorted(Comparator.comparing(ProductoMedia::isEsPrincipal).reversed().thenComparing(ProductoMedia::getOrden)).map(ProductoMedia::getUrl).findFirst().orElse(null);
            return new ProductoEmail(nombre, imagen);
        }
        return new ProductoEmail("Producto de tu compra", null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildContactoTienda(BrandingPostCompra branding) {
        return branding.emailComercial() != null && !branding.emailComercial().isBlank() ? "Este mensaje fue enviado por " + branding.nombreComercial() + ". Para consultas, responde este correo o escribe a " + branding.emailComercial() + "." : "Este mensaje fue enviado por " + branding.nombreComercial() + ".";
    }

    private String buildFirmaTienda(BrandingPostCompra branding) {
        StringBuilder firma = new StringBuilder(branding.nombreComercial());
        if (branding.emailComercial() != null && !branding.emailComercial().isBlank()) {
            firma.append(" | ").append(branding.emailComercial());
        }
        if (branding.telefonoComercial() != null && !branding.telefonoComercial().isBlank()) {
            firma.append(" | ").append(branding.telefonoComercial());
        }
        if (branding.direccionComercial() != null && !branding.direccionComercial().isBlank()) {
            firma.append(" | ").append(branding.direccionComercial());
        }
        return firma.toString();
    }

    private String buildLogoHtml(BrandingPostCompra branding) {
        return branding.logoUrl() != null ? "<img src=\"" + this.escapeHtml(branding.logoUrl()) + "\" alt=\"" + this.escapeHtml(branding.nombreComercial()) + "\" style=\"max-height:52px;max-width:180px;display:block;margin-bottom:16px;background:#fff;border-radius:10px;padding:6px;\" />" : "";
    }

    private String buildContactoHtml(BrandingPostCompra branding) {
        StringBuilder datos = new StringBuilder();
        if (branding.emailComercial() != null && !branding.emailComercial().isBlank()) {
            datos.append("<div>").append(this.escapeHtml(branding.emailComercial())).append("</div>");
        }
        if (branding.telefonoComercial() != null && !branding.telefonoComercial().isBlank()) {
            datos.append("<div>").append(this.escapeHtml(branding.telefonoComercial())).append("</div>");
        }
        if (branding.direccionComercial() != null && !branding.direccionComercial().isBlank()) {
            datos.append("<div>").append(this.escapeHtml(branding.direccionComercial())).append("</div>");
        }
        return "<div style=\"padding:20px 28px;background:#f8fafc;border-top:1px solid #e2e8f0;color:#64748b;font-size:13px;line-height:1.6;\"><div>Este mensaje fue enviado por " + this.escapeHtml(branding.nombreComercial()) + ".</div><strong style=\"color:#334155;\">" + this.escapeHtml(branding.nombreComercial()) + "</strong>" + String.valueOf(datos) + "</div>";
    }

    private String buildWhatsAppUrl(BrandingPostCompra branding, String pedidoId) {
        if (branding.telefonoComercial() == null) {
            return null;
        }
        Object digits = branding.telefonoComercial().replaceAll("\\D", "");
        if (((String)digits).length() == 10) {
            digits = "549" + (String)digits;
        } else if (((String)digits).startsWith("0") && ((String)digits).length() == 11) {
            digits = "549" + ((String)digits).substring(1);
        }
        if (((String)digits).length() < 8) {
            return null;
        }
        String shortId = pedidoId != null && pedidoId.length() >= 8 ? pedidoId.substring(0, 8).toUpperCase(Locale.ROOT) : pedidoId;
        String message = "Hola " + branding.nombreComercial() + ", necesito ayuda con el pedido #" + shortId + ".";
        return "https://wa.me/" + (String)digits + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private record ProductoEmail(String nombre, String imagenUrl) {
    }
}
