package ar.com.mercadodigital.backend.service.postcompra;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlantillaEmailPostCompraServiceTest {

    private final PlantillaEmailPostCompraService plantillaService = new PlantillaEmailPostCompraService();

    @Test
    void plantillaPedidoCreadoIncluyeTrackingYBranding() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                "https://cdn/logo.png",
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoPedidoCreado evento = new EventoPedidoCreado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-123",
                "CREADO",
                new BigDecimal("1200.00"),
                "ARS",
                LocalDateTime.of(2026, 3, 17, 12, 0));

        ContenidoEmail contenido = plantillaService.renderizarPedidoCreado(evento, branding);

        assertTrue(contenido.subject().contains("Mi Tienda"));
        assertTrue(contenido.htmlBody().contains("tracking-123"));
        assertTrue(contenido.htmlBody().contains("https://mi-tienda.com/pedido/tracking-123"));
        assertTrue(contenido.textBody().contains("Pedido"));
    }

    @Test
    void plantillaPagoConfirmadoIncluyeTrackingYMonto() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoPagoConfirmado evento = new EventoPagoConfirmado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-999",
                "PAGADO",
                new BigDecimal("1500.00"),
                "ARS",
                LocalDateTime.of(2026, 3, 17, 14, 30));

        ContenidoEmail contenido = plantillaService.renderizarPagoConfirmado(evento, branding);

        assertTrue(contenido.subject().contains("Pago confirmado"));
        assertTrue(contenido.htmlBody().contains("tracking-999"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-999"));
    }

    @Test
    void plantillaEnvioDespachadoIncluyeTrackingLogistico() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoEnvioDespachado evento = new EventoEnvioDespachado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-ship",
                "PAGADO",
                "DESPACHADO",
                "TRK-123",
                "Andreani",
                LocalDateTime.of(2026, 4, 7, 10, 15));

        ContenidoEmail contenido = plantillaService.renderizarEnvioDespachado(evento, branding);

        assertTrue(contenido.subject().contains("en camino"));
        assertTrue(contenido.htmlBody().contains("TRK-123"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-ship"));
    }

    @Test
    void plantillaEnvioEntregadoReutilizaTrackingParaPostventa() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoEnvioEntregado evento = new EventoEnvioEntregado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-done",
                "PAGADO",
                "ENTREGADO",
                LocalDateTime.of(2026, 4, 7, 11, 45));

        ContenidoEmail contenido = plantillaService.renderizarEnvioEntregado(evento, branding);

        assertTrue(contenido.subject().contains("entregado"));
        assertTrue(contenido.htmlBody().contains("garantia"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-done"));
    }

    @Test
    void plantillaRmaSolicitadoConfirmaRecepcionDelCaso() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoRmaSolicitado evento = new EventoRmaSolicitado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-1",
                "SOLICITADO",
                "Robot Smart",
                "No enciende",
                LocalDateTime.of(2026, 4, 7, 12, 0));

        ContenidoEmail contenido = plantillaService.renderizarRmaSolicitado(evento, branding);

        assertTrue(contenido.subject().contains("Recibimos tu reclamo"));
        assertTrue(contenido.htmlBody().contains("Robot Smart"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-rma-1"));
    }

    @Test
    void plantillaRmaEnvioSolicitadoIncluyeRotuloYSiguientePaso() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoRmaEnvioSolicitado evento = new EventoRmaEnvioSolicitado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-2",
                "EN_TRANSITO_DEVOLUCION",
                "Robot Smart",
                LocalDateTime.of(2026, 4, 7, 13, 30));

        ContenidoEmail contenido = plantillaService.renderizarRmaEnvioSolicitado(evento, branding);

        assertTrue(contenido.subject().contains("Ya puedes enviar el producto"));
        assertTrue(contenido.htmlBody().contains("rotulo de devolucion"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-rma-2"));
    }

    @Test
    void plantillaRmaCerradoIncluyeResolucionYCompensacion() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoRmaCerrado evento = new EventoRmaCerrado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-3",
                "REEMBOLSADO",
                "Robot Smart",
                "REEMBOLSADO",
                new BigDecimal("2500.00"),
                "TRX-123",
                LocalDateTime.of(2026, 4, 7, 16, 45));

        ContenidoEmail contenido = plantillaService.renderizarRmaCerrado(evento, branding);

        assertTrue(contenido.subject().contains("resuelto"));
        assertTrue(contenido.htmlBody().contains("TRX-123"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-rma-3"));
    }

    @Test
    void plantillaRmaReposicionReenviadaIncluyeTrackingRealDelReemplazo() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoRmaReposicionReenviada evento = new EventoRmaReposicionReenviada(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-4",
                "RESUELTO",
                "Robot Smart",
                "REP-123",
                "Andreani",
                LocalDateTime.of(2026, 4, 7, 18, 10));

        ContenidoEmail contenido = plantillaService.renderizarRmaReposicionReenviada(evento, branding);

        assertTrue(contenido.subject().contains("reemplazo"));
        assertTrue(contenido.htmlBody().contains("REP-123"));
        assertTrue(contenido.htmlBody().contains("Andreani"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-rma-4"));
    }

    @Test
    void plantillaRmaReposicionEntregadaConfirmaCierreFinalDelReemplazo() {
        BrandingPostCompra branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                null,
                null,
                null,
                null,
                true,
                true);

        EventoRmaReposicionEntregada evento = new EventoRmaReposicionEntregada(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-5",
                "RESUELTO",
                "Robot Smart",
                "REP-999",
                "Andreani",
                LocalDateTime.of(2026, 4, 7, 19, 30));

        ContenidoEmail contenido = plantillaService.renderizarRmaReposicionEntregada(evento, branding);

        assertTrue(contenido.subject().contains("entrega"));
        assertTrue(contenido.htmlBody().contains("REP-999"));
        assertTrue(contenido.textBody().contains("https://mi-tienda.com/pedido/tracking-rma-5"));
    }
}
