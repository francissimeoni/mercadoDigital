package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.dto.request.ActualizarReposicionManualRequest;
import ar.com.mercadodigital.backend.dto.request.ResolverRmaRequest;
import ar.com.mercadodigital.backend.dto.response.RmaResponse;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.RMA;
import ar.com.mercadodigital.backend.entity.RmaEstado;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Transportista;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.PedidoItemRepository;
import ar.com.mercadodigital.backend.repository.RMARepository;
import ar.com.mercadodigital.backend.repository.RmaAdjuntoRepository;
import ar.com.mercadodigital.backend.repository.RmaMensajeRepository;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaCerrado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionEntregada;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionReenviada;

@ExtendWith(MockitoExtension.class)
class RmaServiceTest {

    @Mock
    private RMARepository rmaRepository;
    @Mock
    private PedidoItemRepository pedidoItemRepository;
    @Mock
    private AuditoriaEventoRepository auditoriaEventoRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RmaMensajeRepository rmaMensajeRepository;
    @Mock
    private RmaAdjuntoRepository rmaAdjuntoRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TransportistaService transportistaService;

    private RmaService rmaService;
    private RMA rma;
    private UUID rmaId;
    private final Map<String, List<AuditoriaEvento>> auditoriaEventos = new HashMap<>();

    @BeforeEach
    void setUp() {
        rmaService = new RmaService(
                rmaRepository,
                pedidoItemRepository,
                auditoriaEventoRepository,
                notificationService,
                rmaMensajeRepository,
                rmaAdjuntoRepository,
                applicationEventPublisher,
                transportistaService,
                new ObjectMapper());

        rma = buildRmaBase();
        rmaId = rma.getId();

        when(rmaRepository.findById(rmaId)).thenAnswer(invocation -> Optional.of(rma));
        when(rmaRepository.save(any(RMA.class))).thenAnswer(invocation -> {
            RMA saved = invocation.getArgument(0);
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });
        when(rmaMensajeRepository.findByRma_IdOrderByCreatedAtAsc(rmaId)).thenReturn(List.of());

        when(auditoriaEventoRepository.save(any(AuditoriaEvento.class))).thenAnswer(invocation -> {
            AuditoriaEvento evento = invocation.getArgument(0);
            if (evento.getId() == null) {
                evento.setId(UUID.randomUUID());
            }
            if (evento.getFechaEvento() == null) {
                evento.setFechaEvento(LocalDateTime.now());
            }
            auditoriaEventos.computeIfAbsent(key(evento.getTipoEvento(), evento.getReferenciaId()), ignored -> new ArrayList<>())
                    .add(evento);
            return evento;
        });
        when(auditoriaEventoRepository.existsByTipoEventoAndReferenciaId(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> !auditoriaEventos
                        .getOrDefault(key(invocation.getArgument(0), invocation.getArgument(1)), List.of())
                        .isEmpty());
        when(auditoriaEventoRepository.findTopByTipoEventoAndReferenciaIdOrderByFechaEventoAsc(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> auditoriaEventos
                        .getOrDefault(key(invocation.getArgument(0), invocation.getArgument(1)), List.of())
                        .stream()
                        .min(Comparator.comparing(AuditoriaEvento::getFechaEvento)));
        when(auditoriaEventoRepository.findTopByTipoEventoAndReferenciaIdOrderByFechaEventoDesc(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> auditoriaEventos
                        .getOrDefault(key(invocation.getArgument(0), invocation.getArgument(1)), List.of())
                        .stream()
                        .max(Comparator.comparing(AuditoriaEvento::getFechaEvento)));
    }

    @Test
    void circuitoReposicionManualQuedaTrazadoConTrackingReal() {
        ResolverRmaRequest resolverRequest = new ResolverRmaRequest();
        resolverRequest.setResolucionFinal("RESUELTO");
        resolverRequest.setObservacionesInspeccion("Se aprueba cambio de producto");
        resolverRequest.setResponsableProbable("PROVEEDOR");

        RmaResponse cerrado = rmaService.resolver(rmaId, resolverRequest, List.of());

        assertEquals("RESUELTO", cerrado.getEstadoRma());
        assertEquals("PENDIENTE_SOLICITUD_PROVEEDOR", cerrado.getEstadoReposicion());
        assertTrue(cerrado.getResumenReposicion().contains("todavia falta pedir"));
        assertTrue(cerrado.getInstruccionesSiguientePaso().contains("solicitar la reposicion al proveedor"));
        assertTrue(cerrado.isPermiteSolicitarReposicionProveedor());
        assertFalse(cerrado.isPermiteRegistrarRecepcionReposicionMd());
        verify(applicationEventPublisher).publishEvent(any(EventoRmaCerrado.class));

        RmaResponse solicitadoProveedor = rmaService.solicitarReposicionProveedor(rmaId, payload("Proveedor confirma reemplazo", null, null));
        assertNotNull(solicitadoProveedor.getFechaReposicionSolicitadaProveedor());
        assertEquals("ESPERANDO_INGRESO_MD", solicitadoProveedor.getEstadoReposicion());
        assertTrue(solicitadoProveedor.getInstruccionesSiguientePaso().contains("ingrese a MD"));
        assertTrue(solicitadoProveedor.isPermiteRegistrarRecepcionReposicionMd());
        assertEquals(1, solicitadoProveedor.getReposicionBitacora().size());
        assertEquals("Proveedor confirma reemplazo", solicitadoProveedor.getReposicionBitacora().get(0).getObservacion());

        RmaResponse recibidoMd = rmaService.recibirReposicionMd(rmaId, payload("Ingreso de reemplazo a deposito MD", null, null));
        assertNotNull(recibidoMd.getFechaReposicionRecibidaMd());
        assertEquals("LISTA_PARA_REENVIO", recibidoMd.getEstadoReposicion());
        assertTrue(recibidoMd.getResumenReposicion().contains("Falta registrar el reenvio"));
        assertTrue(recibidoMd.isPermiteReenviarReposicionCliente());
        assertEquals(2, recibidoMd.getReposicionBitacora().size());

        UUID transportistaId = UUID.randomUUID();
        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setNombre("Andreani");
        when(transportistaService.resolveOrCreate(transportistaId, null)).thenReturn(transportista);

        RmaResponse reenviado = rmaService.reenviarReposicionCliente(
                rmaId,
                payload("Reenvio desde deposito central", transportistaId, "RMA-TRACK-001"));

        assertEquals("RMA-TRACK-001", reenviado.getReposicionTracking());
        assertEquals("Andreani", reenviado.getReposicionTransportistaNombre());
        assertNotNull(reenviado.getFechaReposicionReenviadaCliente());
        assertEquals("EN_CAMINO_AL_CLIENTE", reenviado.getEstadoReposicion());
        assertTrue(reenviado.getResumenReposicion().contains("RMA-TRACK-001"));
        assertTrue(reenviado.isPermiteEntregarReposicionCliente());
        assertEquals(3, reenviado.getReposicionBitacora().size());
        assertEquals("RMA-TRACK-001", reenviado.getReposicionBitacora().get(2).getTracking());
        verify(applicationEventPublisher).publishEvent(any(EventoRmaReposicionReenviada.class));

        RmaResponse entregado = rmaService.entregarReposicionCliente(rmaId, payload("Entrega confirmada", null, null));

        assertNotNull(entregado.getFechaReposicionEntregadaCliente());
        assertEquals("RMA-TRACK-001", entregado.getReposicionTracking());
        assertEquals("Andreani", entregado.getReposicionTransportistaNombre());
        assertEquals("ENTREGADA_CLIENTE", entregado.getEstadoReposicion());
        assertTrue(entregado.getInstruccionesSiguientePaso().contains("ya fue entregada al cliente"));
        assertFalse(entregado.isPermiteEntregarReposicionCliente());
        assertEquals(4, entregado.getReposicionBitacora().size());
        assertEquals("Entrega confirmada", entregado.getReposicionBitacora().get(3).getObservacion());
        verify(applicationEventPublisher).publishEvent(any(EventoRmaReposicionEntregada.class));
    }

    @Test
    void reposicionManualNoAplicaACasosReembolsados() {
        ResolverRmaRequest resolverRequest = new ResolverRmaRequest();
        resolverRequest.setResolucionFinal("REEMBOLSADO");
        resolverRequest.setMontoCompensacion(new BigDecimal("45000"));

        RmaResponse cerrado = rmaService.resolver(rmaId, resolverRequest, List.of());

        assertEquals("REEMBOLSADO", cerrado.getEstadoRma());
        assertEquals("NO_APLICA", cerrado.getEstadoReposicion());
        assertTrue(cerrado.getResumenReposicion().contains("devolucion de dinero"));
        assertThrows(OperacionEnConflictoException.class,
                () -> rmaService.solicitarReposicionProveedor(rmaId, payload("Intento invalido", null, null)));
    }

    @Test
    void reenviarReposicionExigeTransportistaYTracking() {
        ResolverRmaRequest resolverRequest = new ResolverRmaRequest();
        resolverRequest.setResolucionFinal("RESUELTO");
        rmaService.resolver(rmaId, resolverRequest, List.of());
        rmaService.solicitarReposicionProveedor(rmaId, payload("Proveedor confirmado", null, null));
        rmaService.recibirReposicionMd(rmaId, payload("Producto recibido", null, null));

        ActualizarReposicionManualRequest request = new ActualizarReposicionManualRequest();
        request.setObservacion("Falta informacion logistica");

        assertThrows(BadRequestException.class,
                () -> rmaService.reenviarReposicionCliente(rmaId, request));
    }

    @Test
    void reenviarReposicionAceptaTransportistaManualSinId() {
        ResolverRmaRequest resolverRequest = new ResolverRmaRequest();
        resolverRequest.setResolucionFinal("RESUELTO");
        rmaService.resolver(rmaId, resolverRequest, List.of());
        rmaService.solicitarReposicionProveedor(rmaId, payload("Proveedor confirmado", null, null));
        rmaService.recibirReposicionMd(rmaId, payload("Producto recibido", null, null));

        Transportista transportista = new Transportista();
        transportista.setId(UUID.randomUUID());
        transportista.setNombre("Correo Demo");
        when(transportistaService.resolveOrCreate(null, "Correo Demo")).thenReturn(transportista);

        ActualizarReposicionManualRequest request = new ActualizarReposicionManualRequest();
        request.setTransportistaNombre("Correo Demo");
        request.setTracking("RMA-MANUAL-01");
        request.setObservacion("Reenvio con transportista cargado en el momento");

        RmaResponse reenviado = rmaService.reenviarReposicionCliente(rmaId, request);

        assertEquals("Correo Demo", reenviado.getReposicionTransportistaNombre());
        assertEquals("RMA-MANUAL-01", reenviado.getReposicionTracking());
    }

    private ActualizarReposicionManualRequest payload(String observacion, UUID transportistaId, String tracking) {
        ActualizarReposicionManualRequest request = new ActualizarReposicionManualRequest();
        request.setObservacion(observacion);
        request.setTransportistaId(transportistaId);
        request.setTracking(tracking);
        return request;
    }

    private String key(String tipoEvento, UUID referenciaId) {
        return tipoEvento + "|" + referenciaId;
    }

    private RMA buildRmaBase() {
        Usuario cliente = new Usuario();
        cliente.setId(UUID.randomUUID());
        cliente.setNombre("Cliente Demo");
        cliente.setEmail("cliente@demo.local");
        cliente.setTipoUsuario(TipoUsuario.CLIENTE);

        Usuario usuarioProveedor = new Usuario();
        usuarioProveedor.setId(UUID.randomUUID());
        usuarioProveedor.setNombre("Proveedor Demo");
        usuarioProveedor.setEmail("proveedor@demo.local");

        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        proveedor.setNombre("Proveedor Garantias");
        proveedor.setUsuario(usuarioProveedor);

        ProductoCanonico canonico = new ProductoCanonico();
        canonico.setId(UUID.randomUUID());
        canonico.setMarca("Marca");
        canonico.setModelo("Modelo");
        canonico.setNombrePublico("Producto de prueba");

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setProveedor(proveedor);
        producto.setProductoCanonico(canonico);
        producto.setSku("SKU-RMA-1");
        producto.setNombre("Producto interno");
        producto.setPrecioCosto(new BigDecimal("50000"));
        producto.setCategoriaPaquete("SMALL");

        Revendedor revendedor = new Revendedor();
        revendedor.setId(UUID.randomUUID());
        revendedor.setNombre("Revendedor Demo");
        revendedor.setDominio("www.demo.com");

        RevendedorStorefront storefront = new RevendedorStorefront();
        storefront.setId(UUID.randomUUID());
        storefront.setRevendedor(revendedor);
        storefront.setNombreComercial("Tienda Demo");
        revendedor.setStorefront(storefront);

        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID());
        pedido.setCliente(cliente);
        pedido.setRevendedor(revendedor);
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        pedido.setTrackingToken(UUID.randomUUID().toString());
        pedido.setTotalEnvio(new BigDecimal("3500"));

        Envio envio = new Envio();
        envio.setId(UUID.randomUUID());
        envio.setPedido(pedido);
        envio.setEstadoEnvio(EnvioEstado.ENTREGADO);
        envio.setDireccionEnvio("Calle Falsa 123");
        pedido.setEnvio(envio);

        PedidoItem pedidoItem = new PedidoItem();
        pedidoItem.setId(UUID.randomUUID());
        pedidoItem.setPedido(pedido);
        pedidoItem.setProducto(producto);
        pedidoItem.setProveedor(proveedor);
        pedidoItem.setCantidad(1);
        pedidoItem.setPrecioCosto(new BigDecimal("50000"));
        pedidoItem.setPrecioItemFinal(new BigDecimal("75000"));
        pedidoItem.setMarkupAplicado(new BigDecimal("25000"));
        pedidoItem.setTieneSeguro(true);
        pedidoItem.setMontoSeguro(new BigDecimal("5000"));

        RMA entity = new RMA();
        entity.setId(UUID.randomUUID());
        entity.setPedidoItem(pedidoItem);
        entity.setMotivo("No funciona");
        entity.setDescripcion("El producto llego con falla");
        entity.setEstadoRma(RmaEstado.RECIBIDO_EN_DEPOSITO);
        entity.setTieneSeguro(true);
        entity.setCreadoPor(cliente);
        entity.setCreatedAt(LocalDateTime.now().minusDays(2));
        entity.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return entity;
    }
}
