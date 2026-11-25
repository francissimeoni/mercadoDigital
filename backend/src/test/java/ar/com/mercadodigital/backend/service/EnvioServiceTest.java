package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import ar.com.mercadodigital.backend.dto.request.DespacharEnvioRequest;
import ar.com.mercadodigital.backend.dto.response.EnvioResponse;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.Transportista;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.EnvioRepository;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioDespachado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioEntregado;
import ar.com.mercadodigital.backend.service.postcompra.NotificacionInternaEmailService;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock
    private EnvioRepository envioRepository;
    @Mock
    private AuditoriaEventoRepository auditoriaEventoRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LiquidacionProveedorService liquidacionProveedorService;
    @Mock
    private NotificacionInternaEmailService notificacionInternaEmailService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TransportistaService transportistaService;

    @InjectMocks
    private EnvioService envioService;

    private UUID pedidoId;
    private Envio envio;

    @BeforeEach
    void setUp() {
        pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        pedido.setCliente(new ar.com.mercadodigital.backend.entity.Usuario());
        pedido.getCliente().setId(UUID.randomUUID());
        pedido.getCliente().setEmail("cliente@test.com");
        pedido.getCliente().setNombre("Cliente Demo");
        pedido.setTrackingToken(UUID.randomUUID().toString());
        ar.com.mercadodigital.backend.entity.Revendedor revendedor = new ar.com.mercadodigital.backend.entity.Revendedor();
        revendedor.setId(UUID.randomUUID());
        pedido.setRevendedor(revendedor);

        envio = new Envio();
        envio.setPedido(pedido);
        envio.setEstadoEnvio(EnvioEstado.PENDIENTE_PREPARACION);
        envio.setCostoEnvio(new BigDecimal("100.00"));
    }

    @Test
    void flujoValidoCompletoHastaEntregado() {
        UUID transportistaId = UUID.randomUUID();
        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setNombre("Andreani");

        when(envioRepository.findByPedido_Id(pedidoId)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transportistaService.resolveOrCreate(transportistaId, null)).thenReturn(transportista);
        when(auditoriaEventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EnvioResponse listo = envioService.marcarListoParaRetiro(pedidoId);
        assertEquals("LISTO_PARA_RETIRO", listo.getEstadoEnvio());

        EnvioResponse retirado = envioService.marcarRetirado(pedidoId);
        assertEquals("RETIRADO", retirado.getEstadoEnvio());
        verify(liquidacionProveedorService).liberarAutomaticamentePorRetiro(pedidoId);

        DespacharEnvioRequest req = new DespacharEnvioRequest();
        req.setTransportistaId(transportistaId);
        req.setTracking("TRK-123");
        EnvioResponse despachado = envioService.despachar(pedidoId, req);
        assertEquals("DESPACHADO", despachado.getEstadoEnvio());
        assertEquals("TRK-123", despachado.getTracking());
        assertEquals("Andreani", despachado.getTransportistaNombre());
        verify(applicationEventPublisher).publishEvent(any(EventoEnvioDespachado.class));

        EnvioResponse entregado = envioService.marcarEntregado(pedidoId);
        assertEquals("ENTREGADO", entregado.getEstadoEnvio());
        verify(applicationEventPublisher).publishEvent(any(EventoEnvioEntregado.class));
    }

    @Test
    void transicionInvalidaDevuelve409() {
        when(envioRepository.findByPedido_Id(pedidoId)).thenReturn(Optional.of(envio));
        assertThrows(OperacionEnConflictoException.class, () -> envioService.marcarEntregado(pedidoId));
    }

    @Test
    void despacharSinTrackingDevuelve400() {
        DespacharEnvioRequest req = new DespacharEnvioRequest();
        req.setTransportistaId(UUID.randomUUID());
        req.setTracking(" ");
        assertThrows(BadRequestException.class, () -> envioService.despachar(pedidoId, req));
    }

    @Test
    void despacharAceptaTransportistaManualCuandoNoHayId() {
        Transportista transportista = new Transportista();
        transportista.setId(UUID.randomUUID());
        transportista.setNombre("Correo Demo");

        when(envioRepository.findByPedido_Id(pedidoId)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transportistaService.resolveOrCreate(null, "Correo Demo")).thenReturn(transportista);

        envioService.marcarListoParaRetiro(pedidoId);
        envioService.marcarRetirado(pedidoId);

        DespacharEnvioRequest req = new DespacharEnvioRequest();
        req.setTransportistaNombre("Correo Demo");
        req.setTracking("TRK-MANUAL-1");

        EnvioResponse despachado = envioService.despachar(pedidoId, req);
        assertEquals("DESPACHADO", despachado.getEstadoEnvio());
        assertEquals("Correo Demo", despachado.getTransportistaNombre());
    }

    @Test
    void revendedorAdministraSoloElEnvioDeSuPedido() {
        UUID revendedorId = envio.getPedido().getRevendedor().getId();
        when(envioRepository.findByPedido_IdAndPedido_Revendedor_Id(pedidoId, revendedorId))
                .thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EnvioResponse despachado = envioService.despacharPorRevendedor(
                revendedorId, pedidoId, null, null);

        assertEquals("DESPACHADO", despachado.getEstadoEnvio());
        assertEquals(PedidoEstado.ENVIADO, envio.getPedido().getEstadoPedido());

        EnvioResponse entregado = envioService.marcarEntregadoPorRevendedor(revendedorId, pedidoId);
        assertEquals("ENTREGADO", entregado.getEstadoEnvio());
        assertEquals(PedidoEstado.ENTREGADO, envio.getPedido().getEstadoPedido());
    }

    @Test
    void revendedorNoPuedeAdministrarPedidoDeOtraTienda() {
        UUID otroRevendedorId = UUID.randomUUID();
        when(envioRepository.findByPedido_IdAndPedido_Revendedor_Id(pedidoId, otroRevendedorId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.despacharPorRevendedor(otroRevendedorId, pedidoId, null, null));
    }
}
