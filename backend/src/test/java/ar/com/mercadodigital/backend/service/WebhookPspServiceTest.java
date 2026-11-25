package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.dto.request.WebhookPspRequest;
import ar.com.mercadodigital.backend.entity.CategoriaPaquete;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.PspWebhook;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.EnvioRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.PspWebhookRepository;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoInternoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.NotificacionInternaEmailService;

@ExtendWith(MockitoExtension.class)
class WebhookPspServiceTest {

    @Mock
    private PspWebhookRepository pspWebhookRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private EnvioRepository envioRepository;
    @Mock
    private AuditoriaEventoRepository auditoriaEventoRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificacionInternaEmailService notificacionInternaEmailService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private RevendedorMercadoPagoService revendedorMercadoPagoService;

    @InjectMocks
    private WebhookPspService webhookPspService;

    private UUID pedidoId;
    private WebhookPspRequest requestBase;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        pedidoId = UUID.randomUUID();
        requestBase = new WebhookPspRequest();
        requestBase.setIdEventoPsp("evt_123");
        requestBase.setTipoEvento("PAGO_CONFIRMADO");
        requestBase.setReferenciaPedido(pedidoId.toString());
        requestBase.setReferenciaPago("pago_psp_1");
        requestBase.setMonto(new BigDecimal("1000.00"));
        requestBase.setEstado("CONFIRMADO");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"REJECTED,RECHAZADO", "CANCELLED,RECHAZADO", "REFUNDED,REEMBOLSADO", "CHARGED_BACK,REEMBOLSADO"})
    void estadosPspNoAprobadosSeTraducenSegunBytecode(String external, PagoEstado expected) throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var order = new Pedido(); order.setId(pedidoId); order.setEstadoPedido(PedidoEstado.CREADO); order.setTotalFinal(BigDecimal.TEN);
        Pago payment = new Pago(); payment.setMetodo("PSP"); payment.setEstadoPago(PagoEstado.PENDIENTE);
        payment.setMonto(BigDecimal.TEN); order.setPago(payment);
        requestBase.setMonto(BigDecimal.TEN);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(order));
        requestBase.setEstado(external);
        requestBase.setTipoEvento("PAGO_ACTUALIZADO");
        webhookPspService.procesarWebhook(requestBase);
        assertEquals(expected, order.getPago().getEstadoPago());
        assertEquals(PedidoEstado.CREADO, order.getEstadoPedido());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reembolsoDeOtroPagoNoAlteraCobroConfirmado() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        pedido.setTotalFinal(BigDecimal.TEN);
        Pago pago = new Pago();
        pago.setMetodo("PSP");
        pago.setEstadoPago(PagoEstado.CONFIRMADO);
        pago.setPspReferencia("pago-confirmado");
        pago.setMonto(BigDecimal.TEN);
        pedido.setPago(pago);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        requestBase.setTipoEvento("PAGO_ACTUALIZADO");
        requestBase.setEstado("REFUNDED");
        requestBase.setMonto(BigDecimal.TEN);
        requestBase.setReferenciaPago("otro-pago");

        webhookPspService.procesarWebhook(requestBase);

        assertEquals(PagoEstado.CONFIRMADO, pago.getEstadoPago());
        assertEquals("pago-confirmado", pago.getPspReferencia());
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(auditoriaEventoRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void reembolsoDelPagoConfirmadoActualizaSoloElPago() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        pedido.setTotalFinal(BigDecimal.TEN);
        Pago pago = new Pago();
        pago.setMetodo("PSP");
        pago.setEstadoPago(PagoEstado.CONFIRMADO);
        pago.setPspReferencia("pago_psp_1");
        pago.setMonto(BigDecimal.TEN);
        pedido.setPago(pago);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        requestBase.setTipoEvento("PAGO_ACTUALIZADO");
        requestBase.setEstado("REFUNDED");
        requestBase.setMonto(BigDecimal.TEN);

        webhookPspService.procesarWebhook(requestBase);

        assertEquals(PagoEstado.REEMBOLSADO, pago.getEstadoPago());
        assertEquals(PedidoEstado.PAGADO, pedido.getEstadoPedido());
        verify(pedidoRepository).save(pedido);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void cobroTardioDePedidoCanceladoQuedaParaConciliacion() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.CANCELADO);
        Pago pago = new Pago();
        pago.setMetodo("PSP");
        pago.setEstadoPago(PagoEstado.CANCELADO);
        pedido.setPago(pago);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        webhookPspService.procesarWebhook(requestBase);

        assertEquals(PedidoEstado.CANCELADO, pedido.getEstadoPedido());
        assertEquals(PagoEstado.CANCELADO, pago.getEstadoPago());
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(auditoriaEventoRepository, org.mockito.Mockito.times(2)).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void webhookNuevoConfirmaPedidoYPago() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pspWebhookRepository.findByIdEventoPsp("evt_123")).thenReturn(Optional.empty());

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setTotalFinal(new BigDecimal("1000.00"));
        pedido.setTotalEnvio(new BigDecimal("150.00"));
        pedido.setItems(java.util.List.of(item("CHICO")));
        ar.com.mercadodigital.backend.entity.Revendedor revendedor = new ar.com.mercadodigital.backend.entity.Revendedor();
        revendedor.setId(UUID.randomUUID());
        pedido.setRevendedor(revendedor);
        ar.com.mercadodigital.backend.entity.Usuario cliente = new ar.com.mercadodigital.backend.entity.Usuario();
        cliente.setId(UUID.randomUUID());
        cliente.setNombre("Cliente Demo");
        cliente.setEmail("cliente@test.com");
        pedido.setCliente(cliente);

        Pago pago = new Pago();
        pago.setId(UUID.randomUUID());
        pago.setPedido(pedido);
        pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMetodo("PSP");
        pago.setMonto(new BigDecimal("1000.00"));
        pedido.setPago(pago);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        webhookPspService.procesarWebhook(requestBase);

        assertEquals(PedidoEstado.PAGADO, pedido.getEstadoPedido());
        assertEquals(PagoEstado.CONFIRMADO, pago.getEstadoPago());
        assertEquals("pago_psp_1", pago.getPspReferencia());
        assertEquals(EnvioEstado.PENDIENTE_PREPARACION, pedido.getEnvio().getEstadoEnvio());
        assertEquals("CHICO", pedido.getEnvio().getCategoriaPaquete());
        assertEquals(0, new BigDecimal("150.00").compareTo(pedido.getEnvio().getCostoEnvio()));
        verify(auditoriaEventoRepository, org.mockito.Mockito.times(3)).save(any());
        verify(applicationEventPublisher).publishEvent(any(EventoPagoConfirmado.class));
        verify(applicationEventPublisher).publishEvent(any(EventoPagoInternoConfirmado.class));
        verify(notificacionInternaEmailService, never()).notificarVentaInterna(any());
    }

    @Test
    void webhookRepetidoNoReprocesa() {
        PspWebhook existente = new PspWebhook();
        existente.setId(UUID.randomUUID());
        existente.setIdEventoPsp("evt_123");
        existente.setProcesadoOk(true);
        when(pspWebhookRepository.findByIdEventoPsp("evt_123")).thenReturn(Optional.of(existente));

        webhookPspService.procesarWebhook(requestBase);

        verify(pspWebhookRepository, never()).save(any(PspWebhook.class));
        verify(pedidoRepository, never()).findById(any());
        verify(auditoriaEventoRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void pedidoInexistenteDevuelve404() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pspWebhookRepository.findByIdEventoPsp("evt_123")).thenReturn(Optional.empty());
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> webhookPspService.procesarWebhook(requestBase));
        verify(pspWebhookRepository).saveAndFlush(any(PspWebhook.class));
        verify(pspWebhookRepository).save(any(PspWebhook.class));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void montoInconsistenteNoConfirma() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pspWebhookRepository.findByIdEventoPsp("evt_123")).thenReturn(Optional.empty());

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setTotalFinal(new BigDecimal("1000.00"));
        ar.com.mercadodigital.backend.entity.Revendedor revendedor = new ar.com.mercadodigital.backend.entity.Revendedor();
        revendedor.setId(UUID.randomUUID());
        pedido.setRevendedor(revendedor);
        ar.com.mercadodigital.backend.entity.Usuario cliente = new ar.com.mercadodigital.backend.entity.Usuario();
        cliente.setId(UUID.randomUUID());
        cliente.setNombre("Cliente Demo");
        cliente.setEmail("cliente@test.com");
        pedido.setCliente(cliente);

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMetodo("PSP");
        pago.setMonto(new BigDecimal("1000.00"));
        pedido.setPago(pago);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        requestBase.setMonto(new BigDecimal("999.99"));

        assertThrows(OperacionEnConflictoException.class, () -> webhookPspService.procesarWebhook(requestBase));
        assertEquals(PedidoEstado.CREADO, pedido.getEstadoPedido());
        assertEquals(PagoEstado.PENDIENTE, pago.getEstadoPago());
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void webhookConfirmaPedidoYActivaEnvioPendientePagoExistente() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pspWebhookRepository.findByIdEventoPsp("evt_123")).thenReturn(Optional.empty());

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setTotalFinal(new BigDecimal("1000.00"));
        pedido.setTotalEnvio(new BigDecimal("150.00"));
        pedido.setItems(java.util.List.of(item("CHICO")));
        ar.com.mercadodigital.backend.entity.Revendedor revendedor = new ar.com.mercadodigital.backend.entity.Revendedor();
        revendedor.setId(UUID.randomUUID());
        pedido.setRevendedor(revendedor);
        ar.com.mercadodigital.backend.entity.Usuario cliente = new ar.com.mercadodigital.backend.entity.Usuario();
        cliente.setId(UUID.randomUUID());
        cliente.setNombre("Cliente Demo");
        cliente.setEmail("cliente@test.com");
        pedido.setCliente(cliente);

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMetodo("PSP");
        pago.setMonto(new BigDecimal("1000.00"));
        pedido.setPago(pago);

        Envio envio = new Envio();
        envio.setPedido(pedido);
        envio.setEstadoEnvio(EnvioEstado.PENDIENTE_PAGO);
        envio.setCategoriaPaquete("CHICO");
        envio.setDireccionEnvio("Calle 123");
        envio.setCostoEnvio(new BigDecimal("150.00"));
        pedido.setEnvio(envio);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        webhookPspService.procesarWebhook(requestBase);

        assertEquals(PedidoEstado.PAGADO, pedido.getEstadoPedido());
        assertEquals(PagoEstado.CONFIRMADO, pago.getEstadoPago());
        assertEquals(EnvioEstado.PENDIENTE_PREPARACION, pedido.getEnvio().getEstadoEnvio());
        verify(envioRepository, never()).save(any(Envio.class));
    }

    @Test
    void cuentaPspDeOtraTiendaNoPuedeConfirmarPedido() {
        Pedido pedido = new Pedido(); pedido.setId(pedidoId);
        var revendedor = new ar.com.mercadodigital.backend.entity.Revendedor();
        revendedor.setId(UUID.randomUUID()); pedido.setRevendedor(revendedor);
        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));
        when(revendedorMercadoPagoService.resolveForRevendedor(revendedor.getId()))
                .thenReturn(new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("token-propio", "collector-propio", false, true));

        assertThrows(OperacionEnConflictoException.class, () -> webhookPspService.procesarWebhookMercadoPago(
                requestBase,
                new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("token-ajeno", "collector-ajeno", false, true),
                "collector-ajeno"));
        verify(pspWebhookRepository, never()).saveAndFlush(any());
        verify(pedidoRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazoAtrasadoNoRevierteUnPagoConfirmado() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(pspWebhookRepository.saveAndFlush(any(PspWebhook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Pedido pedido = new Pedido(); pedido.setId(pedidoId); pedido.setEstadoPedido(PedidoEstado.PAGADO);
        pedido.setTotalFinal(new BigDecimal("1000.00"));
        Pago pago = new Pago(); pago.setMetodo("PSP"); pago.setEstadoPago(PagoEstado.CONFIRMADO);
        pago.setMonto(new BigDecimal("1000.00")); pago.setPspReferencia("pago_psp_1"); pedido.setPago(pago);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        requestBase.setTipoEvento("PAGO_ACTUALIZADO"); requestBase.setEstado("REJECTED");

        webhookPspService.procesarWebhook(requestBase);
        assertEquals(PagoEstado.CONFIRMADO, pago.getEstadoPago());
        verify(pedidoRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void eventoRegistradoPeroNoProcesadoSeReintenta() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        PspWebhook previo = new PspWebhook(); previo.setIdEventoPsp("evt_123"); previo.setProcesadoOk(false);
        when(pspWebhookRepository.findByIdEventoPsp("evt_123")).thenReturn(Optional.of(previo));
        Pedido pedido = new Pedido(); pedido.setId(pedidoId); pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setTotalFinal(new BigDecimal("1000.00"));
        Pago pago = new Pago(); pago.setMetodo("PSP"); pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMonto(new BigDecimal("1000.00")); pedido.setPago(pago);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        requestBase.setTipoEvento("PAGO_ACTUALIZADO"); requestBase.setEstado("REJECTED");

        webhookPspService.procesarWebhook(requestBase);
        assertEquals(PagoEstado.RECHAZADO, pago.getEstadoPago());
        org.junit.jupiter.api.Assertions.assertTrue(previo.isProcesadoOk());
        verify(pspWebhookRepository, never()).saveAndFlush(any());
    }

    private PedidoItem item(String categoriaPaquete) {
        Producto producto = new Producto();
        producto.setCategoriaPaquete(categoriaPaquete);
        producto.setProveedor(new Proveedor());
        PedidoItem item = new PedidoItem();
        item.setProducto(producto);
        item.setCantidad(1);
        return item;
    }
}
