package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import ar.com.mercadodigital.backend.dto.request.CrearPedidoCanonicoRequest;
import ar.com.mercadodigital.backend.dto.request.CrearPedidoRequest;
import ar.com.mercadodigital.backend.dto.request.PedidoItemCanonicoRequest;
import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoResponse;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorMarkup;
import ar.com.mercadodigital.backend.entity.SplitPedidoItem;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.SinStockException;
import ar.com.mercadodigital.backend.mapper.PedidoMapper;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import ar.com.mercadodigital.backend.repository.RevendedorMarkupRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.SplitPedidoItemRepository;
import ar.com.mercadodigital.backend.service.postcompra.NotificacionInternaEmailService;

@ExtendWith(MockitoExtension.class)
class PedidoServiceCanonicoTest {
    @org.mockito.Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private CatalogoModoService modoCatalogo;

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private ProductoService productoService;
    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private ProveedorStockRepository proveedorStockRepository;
    @Mock
    private RevendedorMarkupRepository revendedorMarkupRepository;
    @Mock
    private SplitPedidoItemRepository splitPedidoItemRepository;
    @Mock
    private OfertaProductoCanonicoService ofertaProductoCanonicoService;
    @Mock
    private PricingService pricingService;
    @Mock
    private RevendedorPricingV1Service revendedorPricingV1Service;
    @Mock
    private CostoOperativoMdService costoOperativoMdService;
    @Mock
    private CostoEnvioService costoEnvioService;
    @Mock
    private DescuentoRevendedorService descuentoRevendedorService;
    @Mock
    private PedidoMapper pedidoMapper;
    @Mock
    private AuditoriaEventoRepository auditoriaEventoRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificacionInternaEmailService notificacionInternaEmailService;
    @Mock
    private RevendedorOwnershipService ownershipService;

    @InjectMocks
    private PedidoService pedidoService;

    private UUID clienteId;
    private UUID revendedorId;
    private UUID canonicoId;
    private Usuario cliente;
    private Revendedor revendedor;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        revendedorId = UUID.randomUUID();
        canonicoId = UUID.randomUUID();

        cliente = new Usuario();
        cliente.setId(clienteId);
        cliente.setNombre("Cliente");
        cliente.setEmail("cliente@test.com");
        cliente.setTipoUsuario(TipoUsuario.CLIENTE);
        cliente.setActivo(true);

        revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setNombre("Revendedor");
        revendedor.setActivo(true);

        Mockito.lenient().when(descuentoRevendedorService.resolveDescuento(any(), any(), any()))
                .thenReturn(new DescuentoRevendedorService.DescuentoAplicado(null, BigDecimal.ZERO));
    }

    @Test
    void crearPedidoCanonicoSinStockLanza409() {
        CrearPedidoCanonicoRequest request = request(1, false);
        when(usuarioService.getEntity(clienteId)).thenReturn(cliente);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonicoId, revendedorId)).thenReturn(List.of());

        assertThrows(SinStockException.class, () -> pedidoService.crearPedidoCanonico(request));
    }

    @Test
    void crearPedidoCanonicoCantidadMayorAStockLanza409() {
        CrearPedidoCanonicoRequest request = request(10, false);
        when(usuarioService.getEntity(clienteId)).thenReturn(cliente);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonicoId, revendedorId))
                .thenReturn(List.of(oferta(UUID.randomUUID(), canonicoId, new BigDecimal("1000"), 3)));

        assertThrows(SinStockException.class, () -> pedidoService.crearPedidoCanonico(request));
    }

    @Test
    void crearPedidoCanonicoSinProveedorStockLanza409SinDescuento() {
        UUID productoId = UUID.randomUUID();
        CrearPedidoCanonicoRequest request = request(2, false);
        when(usuarioService.getEntity(clienteId)).thenReturn(cliente);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonicoId, revendedorId))
                .thenReturn(List.of(oferta(productoId, canonicoId, new BigDecimal("1200"), 10)));

        Producto producto = new Producto();
        producto.setId(productoId);
        when(productoService.getEntity(productoId)).thenReturn(producto);
        when(proveedorStockRepository.findByProductoIdForUpdate(productoId)).thenReturn(List.of());

        assertThrows(SinStockException.class, () -> pedidoService.crearPedidoCanonico(request));
        verify(proveedorStockRepository, never()).save(any(ProveedorStock.class));
    }

    @Test
    void crearPedidoCanonicoEligeMejorDescuentaStockYGeneraSplit() {
        UUID productoMejorId = UUID.randomUUID();
        UUID productoPeorId = UUID.randomUUID();
        UUID proveedorId = UUID.randomUUID();
        CrearPedidoCanonicoRequest request = request(2, true);

        when(usuarioService.getEntity(clienteId)).thenReturn(cliente);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonicoId, revendedorId))
                .thenReturn(List.of(
                        oferta(productoMejorId, canonicoId, new BigDecimal("1200"), 5),
                        oferta(productoPeorId, canonicoId, new BigDecimal("1400"), 20)));

        Producto producto = new Producto();
        producto.setId(productoMejorId);
        producto.setPrecioCosto(new BigDecimal("1000"));
        producto.setMoneda("ARS");
        producto.setCategoriaPaquete("CHICO");
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        proveedor.setNombre("Prov");
        proveedor.setActivo(true);
        producto.setProveedor(proveedor);
        when(productoService.getEntity(productoMejorId)).thenReturn(producto);

        ProveedorStock stock = new ProveedorStock();
        stock.setStock(5);
        stock.setPrecioCostoActual(new BigDecimal("1000"));
        when(proveedorStockRepository.findByProductoIdForUpdate(productoMejorId)).thenReturn(List.of(stock));
        when(proveedorStockRepository.save(any(ProveedorStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(costoOperativoMdService.getMontoParaCategoria("CHICO", new BigDecimal("1000"))).thenReturn(new BigDecimal("50"));
        when(costoEnvioService.getMontoParaCategoria("CHICO")).thenReturn(new BigDecimal("1500"));

        when(pricingService.calcularCostoOperativoItem(new BigDecimal("1000"), new BigDecimal("50"), BigDecimal.ZERO, true))
                .thenReturn(new PricingService.PricingDetalle(
                        new BigDecimal("1141"),
                        new BigDecimal("20"),
                        new BigDecimal("91"),
                        BigDecimal.ZERO,
                        new BigDecimal("1050")));

        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.getItems().forEach(i -> i.setId(UUID.randomUUID()));
            return p;
        });
        when(splitPedidoItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any(AuditoriaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            return PedidoResponse.builder()
                    .id(pedido.getId())
                    .revendedorId(pedido.getRevendedor().getId())
                    .clienteId(pedido.getCliente().getId())
                    .items(List.of())
                    .totalFinal(pedido.getTotalFinal())
                    .build();
        });

        PedidoResponse response = pedidoService.crearPedidoCanonico(request);

        assertEquals(revendedorId, response.getRevendedorId());
        assertEquals(clienteId, response.getClienteId());
        assertEquals(0, new BigDecimal("5322").compareTo(response.getTotalFinal()));
        assertEquals(3, stock.getStock());

        ArgumentCaptor<List<SplitPedidoItem>> splitCaptor = ArgumentCaptor.forClass(List.class);
        verify(splitPedidoItemRepository).saveAll(splitCaptor.capture());
        List<SplitPedidoItem> splits = splitCaptor.getValue();
        assertEquals(1, splits.size());
        SplitPedidoItem split = splits.get(0);
        assertEquals(productoMejorId, split.getPedidoItem().getProducto().getId());
        assertEquals(proveedorId, split.getPedidoItem().getProveedor().getId());
        assertEquals(0, new BigDecimal("2000").compareTo(split.getMontoProveedor()));
        assertEquals(0, BigDecimal.ZERO.compareTo(split.getMontoRevendedor()));
        assertEquals(0, new BigDecimal("100").compareTo(split.getMontoMd()));
        assertEquals(0, new BigDecimal("3000").compareTo(split.getMontoEnvio()));
        assertEquals(0, new BigDecimal("40").compareTo(split.getMontoSeguro()));
        assertEquals(0, new BigDecimal("182").compareTo(split.getMontoPsp()));

        ArgumentCaptor<AuditoriaEvento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(auditoriaEventoRepository, org.mockito.Mockito.times(2)).save(auditoriaCaptor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                auditoriaCaptor.getAllValues().stream().anyMatch(evento -> "STOCK_RESERVADO".equals(evento.getTipoEvento())));
    }

    @Test
    void crearPedidoCanonicoPublicoPersisteMontoPspEnSplit() {
        UUID productoId = UUID.randomUUID();
        CrearPedidoCanonicoRequest request = request(1, false);

        when(usuarioService.getEntity(clienteId)).thenReturn(cliente);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(ofertaProductoCanonicoService.getOfertasConStock(canonicoId, revendedorId))
                .thenReturn(List.of(oferta(productoId, canonicoId, new BigDecimal("1180"), 5)));

        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setPrecioCosto(new BigDecimal("1000"));
        producto.setMoneda("ARS");
        producto.setCategoriaPaquete("CHICO");
        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        producto.setProveedor(proveedor);
        when(productoService.getEntity(productoId)).thenReturn(producto);

        ProveedorStock stock = new ProveedorStock();
        stock.setStock(5);
        stock.setPrecioCostoActual(new BigDecimal("1000"));
        when(proveedorStockRepository.findByProductoIdForUpdate(productoId)).thenReturn(List.of(stock));
        when(proveedorStockRepository.save(any(ProveedorStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(costoOperativoMdService.getMontoParaCategoria("CHICO", new BigDecimal("1000"))).thenReturn(new BigDecimal("50"));
        when(costoEnvioService.getMontoParaCategoria("CHICO")).thenReturn(new BigDecimal("1500"));
        when(revendedorPricingV1Service.resolvePricingRule(revendedorId, producto))
                .thenReturn(new RevendedorPricingV1Service.PricingRuleResolution(
                        new BigDecimal("0.10"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, "GLOBAL"));
        when(pricingService.calcularPrecioItem(new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("0.10"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .thenReturn(new PricingService.PricingDetalle(
                        new BigDecimal("1255"),
                        BigDecimal.ZERO,
                        new BigDecimal("100"),
                        new BigDecimal("105"),
                        new BigDecimal("1155")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(UUID.randomUUID());
            pedido.getItems().forEach(item -> item.setId(UUID.randomUUID()));
            return pedido;
        });
        when(splitPedidoItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any(AuditoriaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            return PedidoResponse.builder()
                    .id(pedido.getId())
                    .trackingToken(pedido.getTrackingToken())
                    .totalFinal(pedido.getTotalFinal())
                    .build();
        });

        pedidoService.crearPedidoCanonicoPublico(request, "Calle 1");

        ArgumentCaptor<List<SplitPedidoItem>> splitCaptor = ArgumentCaptor.forClass(List.class);
        verify(splitPedidoItemRepository).saveAll(splitCaptor.capture());
        SplitPedidoItem split = splitCaptor.getValue().get(0);
        assertEquals(0, new BigDecimal("1000").compareTo(split.getMontoProveedor()));
        assertEquals(0, new BigDecimal("105").compareTo(split.getMontoRevendedor()));
        assertEquals(0, new BigDecimal("50").compareTo(split.getMontoMd()));
        assertEquals(0, new BigDecimal("1500").compareTo(split.getMontoEnvio()));
        assertEquals(0, new BigDecimal("100").compareTo(split.getMontoPsp()));
    }

    @Test
    void crearPedidoCanonicoPublicoGeneraTrackingTokenSoloParaOrigenPublico() {
        UUID productoId = UUID.randomUUID();
        CrearPedidoCanonicoRequest request = request(1, false);

        when(usuarioService.getEntity(clienteId)).thenReturn(cliente);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(ofertaProductoCanonicoService.getOfertasConStock(canonicoId, revendedorId))
                .thenReturn(List.of(oferta(productoId, canonicoId, new BigDecimal("1200"), 5)));

        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setPrecioCosto(new BigDecimal("1000"));
        producto.setMoneda("ARS");
        producto.setCategoriaPaquete("CHICO");
        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        producto.setProveedor(proveedor);
        when(productoService.getEntity(productoId)).thenReturn(producto);

        ProveedorStock stock = new ProveedorStock();
        stock.setStock(5);
        stock.setPrecioCostoActual(new BigDecimal("1000"));
        when(proveedorStockRepository.findByProductoIdForUpdate(productoId)).thenReturn(List.of(stock));
        when(proveedorStockRepository.save(any(ProveedorStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(costoOperativoMdService.getMontoParaCategoria("CHICO", new BigDecimal("1000"))).thenReturn(BigDecimal.ZERO);
        when(costoEnvioService.getMontoParaCategoria("CHICO")).thenReturn(BigDecimal.ZERO);
        when(revendedorPricingV1Service.resolvePricingRule(revendedorId, producto))
                .thenReturn(new RevendedorPricingV1Service.PricingRuleResolution(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, "GLOBAL"));
        when(pricingService.calcularPrecioItem(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false))
                .thenReturn(new PricingService.PricingDetalle(
                        new BigDecimal("1000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("1000")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(UUID.randomUUID());
            return pedido;
        });
        when(splitPedidoItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any(AuditoriaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            return PedidoResponse.builder()
                    .id(pedido.getId())
                    .trackingToken(pedido.getTrackingToken())
                    .build();
        });

        PedidoResponse response = pedidoService.crearPedidoCanonicoPublico(request, "Calle 1");

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(pedidoCaptor.capture());
        assertEquals(OrigenPedido.PUBLICO_DIRECTO, pedidoCaptor.getValue().getOrigenPedido());
        assertEquals(EnvioEstado.PENDIENTE_PAGO, pedidoCaptor.getValue().getEnvio().getEstadoEnvio());
        org.junit.jupiter.api.Assertions.assertNotNull(pedidoCaptor.getValue().getTrackingToken());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getTrackingToken());
    }

    @Test
    void crearPedidoCanonicoPublicoAplicaDescuentoContraMarkupDelRevendedor() {
        UUID productoId = UUID.randomUUID();
        CrearPedidoCanonicoRequest request = request(1, false);
        request.setCodigoDescuento("PROMO10");

        when(usuarioService.getEntity(clienteId)).thenReturn(cliente);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(ofertaProductoCanonicoService.getOfertasConStock(canonicoId, revendedorId))
                .thenReturn(List.of(oferta(productoId, canonicoId, new BigDecimal("1200"), 5)));

        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setPrecioCosto(new BigDecimal("1000"));
        producto.setMoneda("ARS");
        producto.setCategoriaPaquete("CHICO");
        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        producto.setProveedor(proveedor);
        when(productoService.getEntity(productoId)).thenReturn(producto);

        ProveedorStock stock = new ProveedorStock();
        stock.setStock(5);
        stock.setPrecioCostoActual(new BigDecimal("1000"));
        when(proveedorStockRepository.findByProductoIdForUpdate(productoId)).thenReturn(List.of(stock));
        when(proveedorStockRepository.save(any(ProveedorStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(costoOperativoMdService.getMontoParaCategoria("CHICO", new BigDecimal("1000"))).thenReturn(new BigDecimal("50"));
        when(costoEnvioService.getMontoParaCategoria("CHICO")).thenReturn(new BigDecimal("200"));
        when(revendedorPricingV1Service.resolvePricingRule(revendedorId, producto))
                .thenReturn(new RevendedorPricingV1Service.PricingRuleResolution(
                        new BigDecimal("0.10"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, "GLOBAL"));
        when(pricingService.calcularPrecioItem(new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("0.10"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .thenReturn(new PricingService.PricingDetalle(
                        new BigDecimal("1155"),
                        BigDecimal.ZERO,
                        new BigDecimal("100"),
                        new BigDecimal("105"),
                        new BigDecimal("1155")));
        when(descuentoRevendedorService.resolveDescuento(revendedor, "PROMO10", new BigDecimal("105")))
                .thenReturn(new DescuentoRevendedorService.DescuentoAplicado("PROMO10", new BigDecimal("90")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(UUID.randomUUID());
            pedido.getItems().forEach(item -> item.setId(UUID.randomUUID()));
            return pedido;
        });
        when(splitPedidoItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any(AuditoriaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            return PedidoResponse.builder()
                    .id(pedido.getId())
                    .totalFinal(pedido.getTotalFinal())
                    .totalDescuento(pedido.getTotalDescuento())
                    .codigoDescuentoAplicado(pedido.getCodigoDescuentoAplicado())
                    .build();
        });

        PedidoResponse response = pedidoService.crearPedidoCanonicoPublico(request, "Calle 1");

        assertEquals(0, new BigDecimal("1265").compareTo(response.getTotalFinal()));
        assertEquals(0, new BigDecimal("90").compareTo(response.getTotalDescuento()));
        assertEquals("PROMO10", response.getCodigoDescuentoAplicado());

        ArgumentCaptor<List<SplitPedidoItem>> splitCaptor = ArgumentCaptor.forClass(List.class);
        verify(splitPedidoItemRepository).saveAll(splitCaptor.capture());
        SplitPedidoItem split = splitCaptor.getValue().get(0);
        assertEquals(0, new BigDecimal("15").compareTo(split.getMontoRevendedor()));
    }

    @Test
    void cancelarPedidoReponeStockYCambiaEstado() {
        UUID pedidoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID proveedorId = UUID.randomUUID();

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setRevendedor(revendedor);

        Producto producto = new Producto();
        producto.setId(productoId);
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setProveedor(proveedor);
        item.setCantidad(3);
        item.setPrecioCosto(new BigDecimal("1000"));
        item.setMarkupAplicado(new BigDecimal("100"));
        item.setPrecioItemFinal(new BigDecimal("1100"));
        item.setMontoSeguro(BigDecimal.ZERO);
        pedido.setItems(List.of(item));

        ProveedorStock stock = new ProveedorStock();
        stock.setStock(2);
        stock.setProducto(producto);
        stock.setProveedor(proveedor);

        Pago pagoPendiente = new Pago();
        pagoPendiente.setEstadoPago(PagoEstado.PENDIENTE);
        pedido.setPago(pagoPendiente);
        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));
        when(proveedorStockRepository.findByProductoIdForUpdate(productoId)).thenReturn(List.of(stock));
        when(proveedorStockRepository.save(any(ProveedorStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any(AuditoriaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenReturn(PedidoResponse.builder()
                .id(pedidoId)
                .estadoPedido("CANCELADO")
                .items(List.of())
                .build());

        PedidoResponse response = pedidoService.cancelarPedido(pedidoId);

        assertEquals(5, stock.getStock());
        assertEquals(PedidoEstado.CANCELADO, pedido.getEstadoPedido());
        assertEquals(PagoEstado.CANCELADO, pedido.getPago().getEstadoPago());
        assertEquals("CANCELADO", response.getEstadoPedido());
        verify(auditoriaEventoRepository, org.mockito.Mockito.times(2)).save(any(AuditoriaEvento.class));
    }

    @Test
    void cancelarPedidoEnEstadoNoPermitidoDevuelve409() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        pedido.setRevendedor(revendedor);
        pedido.setItems(List.of());

        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));

        assertThrows(OperacionEnConflictoException.class, () -> pedidoService.cancelarPedido(pedidoId));
        verify(proveedorStockRepository, never()).save(any(ProveedorStock.class));
    }

    @Test
    void tiendaAjenaNoLeeNiCancelaPedido() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setRevendedor(revendedor);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));
        Mockito.doThrow(new AccessDeniedException("Tienda ajena"))
                .when(ownershipService).validarOwnership(revendedorId);

        assertThrows(AccessDeniedException.class, () -> pedidoService.getById(pedidoId));
        assertThrows(AccessDeniedException.class, () -> pedidoService.cancelarPedido(pedidoId));
        verify(pedidoMapper, never()).toResponse(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(proveedorStockRepository, never()).save(any(ProveedorStock.class));
    }

    @Test
    void tiendaAjenaNoCreaPedidosOperativos() {
        Mockito.doThrow(new AccessDeniedException("Tienda ajena"))
                .when(ownershipService).validarOwnership(revendedorId);
        CrearPedidoRequest simple = new CrearPedidoRequest();
        simple.setRevendedorId(revendedorId);
        CrearPedidoCanonicoRequest canonico = request(1, false);

        assertThrows(AccessDeniedException.class, () -> pedidoService.crearPedido(simple));
        assertThrows(AccessDeniedException.class, () -> pedidoService.crearPedidoCanonico(canonico));
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(proveedorStockRepository, never()).save(any(ProveedorStock.class));
    }

    private CrearPedidoCanonicoRequest request(int cantidad, boolean seguro) {
        PedidoItemCanonicoRequest item = new PedidoItemCanonicoRequest();
        item.setProductoCanonicoId(canonicoId);
        item.setCantidad(cantidad);
        item.setTieneSeguro(seguro);

        CrearPedidoCanonicoRequest request = new CrearPedidoCanonicoRequest();
        request.setClienteId(clienteId);
        request.setRevendedorId(revendedorId);
        request.setItems(List.of(item));
        return request;
    }

    private OfertaProductoResponse oferta(UUID productoId, UUID canonicoId, BigDecimal precio, int stock) {
        return OfertaProductoResponse.builder()
                .productoId(productoId)
                .productoCanonicoId(canonicoId)
                .precioFinalEstimado(precio)
                .stockDisponible(stock)
                .demoraEstimadaDias(5)
                .categoriaPaquete("CHICO")
                .build();
    }
}
