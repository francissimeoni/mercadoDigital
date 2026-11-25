package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.request.PublicPedidoDirectoRequest;
import ar.com.mercadodigital.backend.dto.response.PedidoResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoDirectoResponse;
import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.entity.ClienteRevendedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AsesorVentaRepository;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.service.postcompra.EventoPedidoCreado;

@ExtendWith(MockitoExtension.class)
class PublicPedidoDirectoServiceTest {
    @org.mockito.Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private CatalogoPublicoService catalogoIndependiente;
    @Mock private CatalogoModoService modoCatalogo;

    @Mock
    private StorefrontService storefrontService;
    @Mock
    private PublicCatalogoService publicCatalogoService;
    @Mock
    private PedidoService pedidoService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private ClienteRevendedorRepository clienteRevendedorRepository;
    @Mock
    private AsesorVentaRepository asesorVentaRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @Mock
    private PspConfigService pspConfigService;

    @InjectMocks
    private PublicPedidoDirectoService publicPedidoDirectoService;

    @Test
    void compraDirectaExitosaCreaUsuarioNuevoYAseguraRelacion() {
        PublicPedidoDirectoRequest request = requestBase();
        Revendedor revendedor = revendedor(request.getRevendedorId());
        Usuario clienteGuardado = cliente(UUID.randomUUID(), "cliente@test.com");

        doNothing().when(storefrontService).validarRevendedorPublico(request.getRevendedorId());
        when(publicCatalogoService.getProductoDetalle(request.getProductoCanonicoId(), request.getRevendedorId()))
                .thenReturn(PublicProductoDetalleResponse.builder()
                        .productoCanonicoId(request.getProductoCanonicoId())
                        .demoraEstimadaDias(5)
                        .build());
        when(revendedorRepository.findById(request.getRevendedorId())).thenReturn(Optional.of(revendedor));
        when(usuarioRepository.findByEmailIgnoreCase("cliente@test.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(clienteGuardado);
        when(clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(clienteGuardado.getId(), revendedor.getId()))
                .thenReturn(Optional.empty());
        when(clienteRevendedorRepository.save(any(ClienteRevendedor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoService.crearPedidoCanonicoPublico(any(), any())).thenReturn(PedidoResponse.builder()
                .id(UUID.randomUUID())
                .estadoPedido("CREADO")
                .estadoPago("PENDIENTE")
                .totalEnvio(BigDecimal.ZERO)
                .totalSeguro(new BigDecimal("20.00"))
                .totalFinal(new BigDecimal("120.00"))
                .trackingToken("tracking-publico-1")
                .build());

        PublicPedidoDirectoResponse response = publicPedidoDirectoService.crearPedidoDirecto(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals("Juan Perez", usuarioCaptor.getValue().getNombre());
        assertEquals("cliente@test.com", usuarioCaptor.getValue().getEmail());
        assertEquals(TipoUsuario.CLIENTE, usuarioCaptor.getValue().getTipoUsuario());

        verify(clienteRevendedorRepository).save(any(ClienteRevendedor.class));
        // Production emits this event at transfer creation or payment confirmation, not at unpaid PSP checkout.
        verify(applicationEventPublisher, never()).publishEvent(any(EventoPedidoCreado.class));
        assertEquals(new BigDecimal("100.00"), response.getTotalProducto());
        assertEquals(new BigDecimal("20.00"), response.getTotalSeguro());
        assertEquals(Integer.valueOf(5), response.getDemoraEstimadaDias());
        assertEquals("tracking-publico-1", response.getTrackingToken());
    }

    @Test
    void reutilizaUsuarioExistenteYNoDuplicaRelacion() {
        PublicPedidoDirectoRequest request = requestBase();
        Revendedor revendedor = revendedor(request.getRevendedorId());
        Usuario clienteExistente = cliente(UUID.randomUUID(), "cliente@test.com");
        ClienteRevendedor relacionExistente = new ClienteRevendedor();
        relacionExistente.setUsuario(clienteExistente);
        relacionExistente.setRevendedor(revendedor);
        relacionExistente.setActivo(true);
        relacionExistente.setOrigenAlta("MT1");

        doNothing().when(storefrontService).validarRevendedorPublico(request.getRevendedorId());
        when(publicCatalogoService.getProductoDetalle(request.getProductoCanonicoId(), request.getRevendedorId()))
                .thenReturn(PublicProductoDetalleResponse.builder()
                        .productoCanonicoId(request.getProductoCanonicoId())
                        .demoraEstimadaDias(null)
                        .build());
        when(revendedorRepository.findById(request.getRevendedorId())).thenReturn(Optional.of(revendedor));
        when(usuarioRepository.findByEmailIgnoreCase("cliente@test.com")).thenReturn(Optional.of(clienteExistente));
        when(clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(clienteExistente.getId(), revendedor.getId()))
                .thenReturn(Optional.of(relacionExistente));
        when(pedidoService.crearPedidoCanonicoPublico(any(), any())).thenReturn(PedidoResponse.builder()
                .id(UUID.randomUUID())
                .estadoPedido("CREADO")
                .estadoPago("PENDIENTE")
                .totalEnvio(BigDecimal.ZERO)
                .totalSeguro(BigDecimal.ZERO)
                .totalFinal(new BigDecimal("120.00"))
                .trackingToken("tracking-publico-2")
                .build());

        PublicPedidoDirectoResponse response = publicPedidoDirectoService.crearPedidoDirecto(request);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(clienteRevendedorRepository, never()).save(any(ClienteRevendedor.class));
        // Production emits this event at transfer creation or payment confirmation, not at unpaid PSP checkout.
        verify(applicationEventPublisher, never()).publishEvent(any(EventoPedidoCreado.class));
        assertEquals(Integer.valueOf(5), response.getDemoraEstimadaDias());
        assertEquals(new BigDecimal("120.00"), response.getTotalProducto());
        assertEquals("tracking-publico-2", response.getTrackingToken());
    }

    @Test
    void creaRelacionParaTenantActualSiElClienteYaExisteEnOtroStorefront() {
        PublicPedidoDirectoRequest request = requestBase();
        Revendedor revendedorActual = revendedor(request.getRevendedorId());
        Revendedor revendedorPrevio = revendedor(UUID.randomUUID());
        Usuario clienteExistente = cliente(UUID.randomUUID(), "cliente@test.com");
        ClienteRevendedor relacionOtroTenant = new ClienteRevendedor();
        relacionOtroTenant.setUsuario(clienteExistente);
        relacionOtroTenant.setRevendedor(revendedorPrevio);
        relacionOtroTenant.setActivo(true);
        relacionOtroTenant.setOrigenAlta("TENANT_OTRO");

        doNothing().when(storefrontService).validarRevendedorPublico(request.getRevendedorId());
        when(publicCatalogoService.getProductoDetalle(request.getProductoCanonicoId(), request.getRevendedorId()))
                .thenReturn(PublicProductoDetalleResponse.builder()
                        .productoCanonicoId(request.getProductoCanonicoId())
                        .demoraEstimadaDias(6)
                        .build());
        when(revendedorRepository.findById(request.getRevendedorId())).thenReturn(Optional.of(revendedorActual));
        when(usuarioRepository.findByEmailIgnoreCase("cliente@test.com")).thenReturn(Optional.of(clienteExistente));
        when(clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(clienteExistente.getId(), revendedorActual.getId()))
                .thenReturn(Optional.empty());
        when(clienteRevendedorRepository.save(any(ClienteRevendedor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoService.crearPedidoCanonicoPublico(any(), any())).thenReturn(PedidoResponse.builder()
                .id(UUID.randomUUID())
                .estadoPedido("CREADO")
                .estadoPago("PENDIENTE")
                .totalEnvio(new BigDecimal("10.00"))
                .totalSeguro(BigDecimal.ZERO)
                .totalFinal(new BigDecimal("130.00"))
                .trackingToken("tracking-publico-tenant-2")
                .build());

        PublicPedidoDirectoResponse response = publicPedidoDirectoService.crearPedidoDirecto(request);

        ArgumentCaptor<ClienteRevendedor> relacionCaptor = ArgumentCaptor.forClass(ClienteRevendedor.class);
        verify(clienteRevendedorRepository, times(1)).save(relacionCaptor.capture());
        assertEquals(clienteExistente.getId(), relacionCaptor.getValue().getUsuario().getId());
        assertEquals(revendedorActual.getId(), relacionCaptor.getValue().getRevendedor().getId());
        assertEquals("COMPRA_PUBLICA_DIRECTA_V1", relacionCaptor.getValue().getOrigenAlta());
        // Production emits this event at transfer creation or payment confirmation, not at unpaid PSP checkout.
        verify(applicationEventPublisher, never()).publishEvent(any(EventoPedidoCreado.class));
        assertEquals(Integer.valueOf(6), response.getDemoraEstimadaDias());
        assertEquals("tracking-publico-tenant-2", response.getTrackingToken());
    }

    @Test
    void revendedorNoDisponiblePropagaNotFound() {
        PublicPedidoDirectoRequest request = requestBase();
        doNothing().when(storefrontService).validarRevendedorPublico(request.getRevendedorId());
        when(publicCatalogoService.getProductoDetalle(request.getProductoCanonicoId(), request.getRevendedorId()))
                .thenThrow(new ResourceNotFoundException("Storefront no disponible para host: " + request.getRevendedorId()));

        assertThrows(ResourceNotFoundException.class, () -> publicPedidoDirectoService.crearPedidoDirecto(request));
    }

    @Test
    void productoNoVisiblePropagaNotFound() {
        PublicPedidoDirectoRequest request = requestBase();
        doNothing().when(storefrontService).validarRevendedorPublico(request.getRevendedorId());
        when(publicCatalogoService.getProductoDetalle(request.getProductoCanonicoId(), request.getRevendedorId()))
                .thenThrow(new ResourceNotFoundException(
                        "Producto sin oferta disponible para revendedor: " + request.getProductoCanonicoId()));

        assertThrows(ResourceNotFoundException.class, () -> publicPedidoDirectoService.crearPedidoDirecto(request));
    }

    @Test
    void compraProductoPropioUsaReferenciaSeparadaSinMezclarIdsCanonicos() {
        PublicPedidoDirectoRequest request = requestBase();
        UUID productoPropioId = UUID.randomUUID();
        request.setProductoCanonicoId(null);
        request.setRevendedorProductoId(productoPropioId);
        Revendedor revendedor = revendedor(request.getRevendedorId());
        Usuario cliente = cliente(UUID.randomUUID(), "cliente@test.com");

        doNothing().when(storefrontService).validarRevendedorPublico(request.getRevendedorId());
        when(publicCatalogoService.getProductoDetalle(productoPropioId, request.getRevendedorId()))
                .thenReturn(PublicProductoDetalleResponse.builder()
                        .productoId(productoPropioId)
                        .origen("PROPIO_REVENDEDOR")
                        .demoraEstimadaDias(4)
                        .build());
        when(revendedorRepository.findById(request.getRevendedorId())).thenReturn(Optional.of(revendedor));
        when(usuarioRepository.findByEmailIgnoreCase("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(cliente.getId(), revendedor.getId()))
                .thenReturn(Optional.of(new ClienteRevendedor()));
        when(revendedorMercadoPagoService.resolveForRevendedor(revendedor.getId()))
                .thenReturn(new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("test", "test", false, true));
        when(pspConfigService.getPspFeeFactor()).thenReturn(new BigDecimal("0.08"));
        when(pedidoService.crearPedidoProductoPropio(
                any(), any(), any(), anyInt(), anyBoolean(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(PedidoResponse.builder()
                        .id(UUID.randomUUID())
                        .estadoPedido("CREADO")
                        .estadoPago("PENDIENTE")
                        .totalFinal(new BigDecimal("2500"))
                        .totalEnvio(BigDecimal.ZERO)
                        .totalSeguro(BigDecimal.ZERO)
                        .trackingToken("tracking-propio")
                        .build());

        PublicPedidoDirectoResponse response = publicPedidoDirectoService.crearPedidoDirecto(request);

        verify(pedidoService).crearPedidoProductoPropio(
                cliente.getId(), revendedor.getId(), productoPropioId, 1, true,
                "Calle 50 123, La Plata, Buenos Aires, CP 1900. Obs: Timbre azul", "PSP", new BigDecimal("0.08"));
        verify(pedidoService, never()).crearPedidoCanonicoPublico(any(), any());
        assertEquals("tracking-propio", response.getTrackingToken());
    }

    private PublicPedidoDirectoRequest requestBase() {
        PublicPedidoDirectoRequest request = new PublicPedidoDirectoRequest();
        request.setRevendedorId(UUID.randomUUID());
        request.setNombreApellido("Juan Perez");
        request.setEmail("Cliente@Test.com");
        request.setTelefonoWhatsapp("1122334455");
        request.setProvincia("Buenos Aires");
        request.setLocalidad("La Plata");
        request.setCodigoPostal("1900");
        request.setCalle("Calle 50");
        request.setNumero("123");
        request.setObservaciones("Timbre azul");
        request.setProductoCanonicoId(UUID.randomUUID());
        request.setCantidad(1);
        request.setSeguroTransporte(true);
        request.setAceptacionTerminos(true);
        return request;
    }

    private Revendedor revendedor(UUID id) {
        Revendedor revendedor = new Revendedor();
        revendedor.setId(id);
        revendedor.setActivo(true);
        return revendedor;
    }

    private Usuario cliente(UUID id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Cliente");
        usuario.setEmail(email);
        usuario.setTipoUsuario(TipoUsuario.CLIENTE);
        usuario.setActivo(true);
        return usuario;
    }
}
