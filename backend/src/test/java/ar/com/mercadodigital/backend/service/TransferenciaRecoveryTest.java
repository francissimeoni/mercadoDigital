package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.service.postcompra.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;

class TransferenciaRecoveryTest {
    @TempDir Path directory;
    private final PagoRepository payments = mock(PagoRepository.class);
    private final PedidoRepository orders = mock(PedidoRepository.class);
    private final RevendedorStorefrontRepository stores = mock(RevendedorStorefrontRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private TransferenciaService service;
    private Pedido order;

    @BeforeEach void setup() {
        service = new TransferenciaService(payments, orders, stores, mock(BrandingPostCompraResolver.class), mock(EmailSender.class), events, mock(NotificationService.class), directory.toString());
        order = new Pedido(); order.setId(UUID.randomUUID()); order.setEstadoPedido(PedidoEstado.CREADO); order.setTrackingToken("tracking-test");
        var reseller = new Revendedor(); reseller.setId(UUID.randomUUID()); order.setRevendedor(reseller);
        var user = new Usuario(); user.setEmail("customer@test.com"); user.setNombre("Cliente"); order.setCliente(user);
        var payment = new Pago(); payment.setId(UUID.randomUUID()); payment.setPedido(order); payment.setMetodo("TRANSFERENCIA"); payment.setEstadoPago(PagoEstado.PENDIENTE); payment.setMonto(BigDecimal.TEN); payment.setTransferenciaToken("ticket"); order.setPago(payment);
        var shipment = new Envio(); shipment.setEstadoEnvio(EnvioEstado.PENDIENTE_PAGO); order.setEnvio(shipment);
    }

    @Test void pdfOctetStreamGuardaYEmiteEventoSinConfirmarPago() throws Exception {
        when(payments.findByTransferenciaToken("ticket")).thenReturn(Optional.of(order.getPago()));
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        var result = service.cargarComprobante("ticket", new MockMultipartFile("archivo", "pago.PDF", "application/octet-stream", "%PDF-test".getBytes()));
        assertEquals("EN_REVISION", result.getEstado()); assertEquals(PagoEstado.PENDIENTE, order.getPago().getEstadoPago());
        assertTrue(Files.exists(directory.resolve(order.getPago().getComprobanteArchivo())));
        assertTrue(order.getPago().getComprobanteArchivo().endsWith(".pdf"));
        verify(events).publishEvent(any(EventoComprobanteTransferenciaCargado.class));
    }

    @Test void confirmarPagoYaConfirmadoReparaPreparacion() {
        order.setEstadoPedido(PedidoEstado.PAGADO);
        order.getPago().setEstadoPago(PagoEstado.CONFIRMADO); order.getPago().setComprobanteArchivo("receipt.pdf");
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        service.confirmar(order.getRevendedor().getId(), order.getId());
        assertEquals(EnvioEstado.PENDIENTE_PREPARACION, order.getEnvio().getEstadoEnvio());
        verify(events, never()).publishEvent(any(EventoPagoConfirmado.class));
    }

    @Test void corrigeDefectoProductivoRechazandoTransferenciaDePedidoCanceladoSinEfectos() {
        order.setEstadoPedido(PedidoEstado.CANCELADO); order.getPago().setEstadoPago(PagoEstado.CANCELADO);
        order.getEnvio().setEstadoEnvio(EnvioEstado.CANCELADO); order.getPago().setComprobanteArchivo("receipt.pdf");
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        assertThrows(OperacionEnConflictoException.class, () -> service.confirmar(order.getRevendedor().getId(), order.getId()));
        assertEquals(PedidoEstado.CANCELADO, order.getEstadoPedido());
        assertEquals(PagoEstado.CANCELADO, order.getPago().getEstadoPago());
        assertEquals(EnvioEstado.CANCELADO, order.getEnvio().getEstadoEnvio());
        verify(orders, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test void confirmaPendienteUnaVezYNoReemiteEventoAlRepetir() {
        order.getPago().setComprobanteArchivo("receipt.pdf");
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        service.confirmar(order.getRevendedor().getId(), order.getId());
        service.confirmar(order.getRevendedor().getId(), order.getId());
        assertEquals(PedidoEstado.PAGADO, order.getEstadoPedido());
        assertEquals(PagoEstado.CONFIRMADO, order.getPago().getEstadoPago());
        assertEquals(EnvioEstado.PENDIENTE_PREPARACION, order.getEnvio().getEstadoEnvio());
        verify(orders, times(1)).save(order);
        verify(events, times(1)).publishEvent(any(EventoPagoConfirmado.class));
    }

    @Test void noConfirmaPedidoAjenoNiSinComprobante() {
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        assertThrows(ResourceNotFoundException.class, () -> service.confirmar(UUID.randomUUID(), order.getId()));
        assertThrows(ar.com.mercadodigital.backend.exception.BadRequestException.class,
                () -> service.confirmar(order.getRevendedor().getId(), order.getId()));
        verify(events, never()).publishEvent(any());
        verify(orders, never()).save(any());
    }

    @Test void noAceptaComprobanteDePedidoCanceladoNiSegundaCarga() throws Exception {
        var file = new MockMultipartFile("archivo", "pago.pdf", "application/pdf", "%PDF-test".getBytes());
        when(payments.findByTransferenciaToken("ticket")).thenReturn(Optional.of(order.getPago()));
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        order.setEstadoPedido(PedidoEstado.CANCELADO);
        assertThrows(OperacionEnConflictoException.class, () -> service.cargarComprobante("ticket", file));
        order.setEstadoPedido(PedidoEstado.CREADO);
        order.getPago().setComprobanteArchivo("previo.pdf");
        assertThrows(OperacionEnConflictoException.class, () -> service.cargarComprobante("ticket", file));
        assertEquals("previo.pdf", order.getPago().getComprobanteArchivo());
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
        verify(payments, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test void rechazaArchivoQueFingeSerPdfAntesDeEscribir() throws Exception {
        when(payments.findByTransferenciaToken("ticket")).thenReturn(Optional.of(order.getPago()));
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        var falso = new MockMultipartFile("archivo", "comprobante.pdf", "application/pdf", "texto arbitrario".getBytes());
        assertThrows(ar.com.mercadodigital.backend.exception.BadRequestException.class,
                () -> service.cargarComprobante("ticket", falso));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
        verify(payments, never()).save(any());
        verify(events, never()).publishEvent(any());
    }
}
