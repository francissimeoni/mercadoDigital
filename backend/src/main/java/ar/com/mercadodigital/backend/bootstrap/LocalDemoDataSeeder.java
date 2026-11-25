package ar.com.mercadodigital.backend.bootstrap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ar.com.mercadodigital.backend.entity.CategoriaPaquete;
import ar.com.mercadodigital.backend.entity.ClienteRevendedor;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Notificacion;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorMarkup;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.TarifaTransportista;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Transportista;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.repository.EnvioRepository;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.NotificacionRepository;
import ar.com.mercadodigital.backend.repository.PagoRepository;
import ar.com.mercadodigital.backend.repository.PedidoItemRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import ar.com.mercadodigital.backend.repository.RevendedorMarkupRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import ar.com.mercadodigital.backend.repository.RolRepository;
import ar.com.mercadodigital.backend.repository.TarifaTransportistaRepository;
import ar.com.mercadodigital.backend.repository.TransportistaRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo.enabled", havingValue = "true")
public class LocalDemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDemoDataSeeder.class);

    private static final UUID ROL_REVENDEDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final UUID ROL_PROVEEDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");
    private static final UUID ROL_LOGISTICA_ID = UUID.fromString("11111111-1111-1111-1111-111111111103");
    private static final UUID ROL_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111104");
    private static final UUID ROL_CLIENTE_ID = UUID.fromString("11111111-1111-1111-1111-111111111105");

    private static final UUID USUARIO_REVENDEDOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID USUARIO_REVENDEDOR_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222206");
    private static final UUID USUARIO_PROVEEDOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222202");
    private static final UUID USUARIO_LOGISTICA_ID = UUID.fromString("22222222-2222-2222-2222-222222222203");
    private static final UUID USUARIO_ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222204");
    private static final UUID USUARIO_CLIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222205");

    private static final UUID REVENDEDOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333301");
    private static final UUID PROVEEDOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333302");

    private static final UUID CANONICO_1_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID PRODUCTO_1_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID STOCK_1_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");
    private static final UUID MARKUP_1_ID = UUID.fromString("77777777-7777-7777-7777-777777777701");
    private static final UUID MEDIA_1_ID = UUID.fromString("88888888-8888-8888-8888-888888888801");
    private static final int CANTIDAD_PRODUCTOS_CATALOGO = 30;

    private static final UUID TRANSPORTISTA_ID = UUID.fromString("99999999-9999-9999-9999-999999999901");
    private static final UUID TARIFA_ID = UUID.fromString("99999999-9999-9999-9999-999999999902");

    private static final UUID PEDIDO_1_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01");
    private static final UUID PEDIDO_2_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02");
    private static final UUID PEDIDO_3_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa03");

    private static final UUID PEDIDO_1_ITEM_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01");
    private static final UUID PEDIDO_2_ITEM_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02");
    private static final UUID PEDIDO_3_ITEM_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03");

    private static final UUID PAGO_1_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccc01");
    private static final UUID PAGO_2_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccc02");
    private static final UUID PAGO_3_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccc03");

    private static final UUID ENVIO_1_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddd01");
    private static final UUID ENVIO_2_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddd02");
    private static final UUID ENVIO_3_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddd03");

    private static final UUID NOTIF_1_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01");
    private static final UUID NOTIF_2_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02");
    private static final UUID NOTIF_3_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03");

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRevendedorRepository clienteRevendedorRepository;
    private final RevendedorRepository revendedorRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoCanonicoRepository productoCanonicoRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final RevendedorMarkupRepository revendedorMarkupRepository;
    private final RevendedorStorefrontRepository revendedorStorefrontRepository;
    private final ProductoMediaRepository productoMediaRepository;
    private final TransportistaRepository transportistaRepository;
    private final TarifaTransportistaRepository tarifaTransportistaRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final NotificacionRepository notificacionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seed demo local: inicio de carga de usuarios y datos operativos");
        Rol rolRevendedor = upsertRol(ROL_REVENDEDOR_ID, "REVENDEDOR");
        Rol rolProveedor = upsertRol(ROL_PROVEEDOR_ID, "PROVEEDOR");
        Rol rolLogistica = upsertRol(ROL_LOGISTICA_ID, "LOGISTICA_MD");
        Rol rolAdmin = upsertRol(ROL_ADMIN_ID, "ADMIN_MD");
        Rol rolCliente = upsertRol(ROL_CLIENTE_ID, "CLIENTE");

        Usuario usuarioRevendedor = upsertUsuario(
                USUARIO_REVENDEDOR_ID,
                "revendedor.demo@md.local",
                "Revendedor Demo",
                TipoUsuario.REVENDEDOR,
                "Revendedor123!",
                rolRevendedor);
        Usuario usuarioRevendedorSecundario = upsertUsuario(
                USUARIO_REVENDEDOR_2_ID,
                "revendedor.tenant2@md.local",
                "Revendedor Tenant Dos",
                TipoUsuario.REVENDEDOR,
                "Revendedor123!",
                rolRevendedor);
        Usuario usuarioProveedor = upsertUsuario(
                USUARIO_PROVEEDOR_ID,
                "proveedor.demo@md.local",
                "Proveedor Demo",
                TipoUsuario.PROVEEDOR,
                "Proveedor123!",
                rolProveedor);
        Usuario usuarioLogistica = upsertUsuario(
                USUARIO_LOGISTICA_ID,
                "logistica.demo@md.local",
                "Logistica Demo",
                TipoUsuario.ADMIN_MD,
                "Logistica123!",
                rolLogistica);
        Usuario usuarioAdmin = upsertUsuario(
                USUARIO_ADMIN_ID,
                "admin.demo@md.local",
                "Admin Demo",
                TipoUsuario.ADMIN_MD,
                "Admin123!",
                rolAdmin);
        Usuario usuarioCliente = upsertUsuario(
                USUARIO_CLIENTE_ID,
                "cliente.demo@md.local",
                "Cliente Demo",
                TipoUsuario.CLIENTE,
                "Cliente123!",
                rolCliente);

        Revendedor revendedor = upsertRevendedor(usuarioRevendedor);
        upsertStorefront(revendedor);
        Revendedor revendedorSecundario = upsertSegundoRevendedor(usuarioRevendedorSecundario);
        upsertSegundoStorefront(revendedorSecundario);
        upsertClienteRevendedor(usuarioCliente, revendedor);
        Proveedor proveedor = upsertProveedor(usuarioProveedor);
        List<Producto> productosCatalogo = upsertCatalogoDemo(proveedor, revendedor);
        Producto producto = productosCatalogo.get(0);
        Transportista transportista = upsertTransportista();
        upsertTarifaTransportista(transportista);

        Pedido pedidoPendientePreparacion = upsertPedido(
                PEDIDO_1_ID,
                revendedor,
                usuarioCliente,
                PedidoEstado.PAGADO,
                new BigDecimal("12000.00"));
        upsertPedidoItem(PEDIDO_1_ITEM_ID, pedidoPendientePreparacion, producto, proveedor, 1);
        upsertPago(PAGO_1_ID, pedidoPendientePreparacion, new BigDecimal("12000.00"), PagoEstado.CONFIRMADO);
        upsertEnvio(
                ENVIO_1_ID,
                pedidoPendientePreparacion,
                null,
                EnvioEstado.PENDIENTE_PREPARACION,
                null,
                "Av. Siempre Viva 742, CABA");

        Pedido pedidoListoRetiro = upsertPedido(
                PEDIDO_2_ID,
                revendedor,
                usuarioCliente,
                PedidoEstado.PAGADO,
                new BigDecimal("15000.00"));
        upsertPedidoItem(PEDIDO_2_ITEM_ID, pedidoListoRetiro, producto, proveedor, 1);
        upsertPago(PAGO_2_ID, pedidoListoRetiro, new BigDecimal("15000.00"), PagoEstado.CONFIRMADO);
        upsertEnvio(
                ENVIO_2_ID,
                pedidoListoRetiro,
                null,
                EnvioEstado.LISTO_PARA_RETIRO,
                null,
                "Calle Falsa 123, Rosario");

        Pedido pedidoRetirado = upsertPedido(
                PEDIDO_3_ID,
                revendedor,
                usuarioCliente,
                PedidoEstado.PAGADO,
                new BigDecimal("18000.00"));
        upsertPedidoItem(PEDIDO_3_ITEM_ID, pedidoRetirado, producto, proveedor, 1);
        upsertPago(PAGO_3_ID, pedidoRetirado, new BigDecimal("18000.00"), PagoEstado.CONFIRMADO);
        upsertEnvio(
                ENVIO_3_ID,
                pedidoRetirado,
                transportista,
                EnvioEstado.RETIRADO,
                null,
                "San Martin 500, Cordoba");

        upsertNotificacion(
                NOTIF_1_ID,
                usuarioRevendedor,
                "PEDIDO_CREADO",
                "Pedido creado",
                "Se creo el pedido " + PEDIDO_1_ID + " correctamente.",
                false);
        upsertNotificacion(
                NOTIF_2_ID,
                usuarioCliente,
                "ENVIO_LISTO_PARA_RETIRO",
                "Envio listo para retiro",
                "Tu pedido " + PEDIDO_2_ID + " esta listo para retiro.",
                false);
        upsertNotificacion(
                NOTIF_3_ID,
                usuarioLogistica,
                "ENVIO_DESPACHADO",
                "Despacho pendiente de cierre",
                "Revisar estado logistico del pedido " + PEDIDO_3_ID + ".",
                true);

        log.info("Seed demo local: carga completada (usuarios demo: {}, {}, {}, {}, {}, {})",
                "revendedor.demo@md.local",
                "revendedor.tenant2@md.local",
                "proveedor.demo@md.local",
                "logistica.demo@md.local",
                "admin.demo@md.local",
                "cliente.demo@md.local");
        log.info("Seed demo local: catalogo storefront preparado con {} productos canonicos visibles", productosCatalogo.size());
    }

    private Rol upsertRol(UUID id, String nombre) {
        Rol rol = rolRepository.findByNombre(nombre).orElseGet(Rol::new);
        rol.setNombre(nombre);
        return rolRepository.save(rol);
    }

    private Usuario upsertUsuario(UUID id, String email, String nombre, TipoUsuario tipoUsuario, String passwordPlano, Rol rol) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseGet(Usuario::new);
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setActivo(true);
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        HashSet<Rol> roles = new HashSet<>();
        roles.add(rol);
        usuario.setRoles(roles);
        return usuarioRepository.save(usuario);
    }

    private Revendedor upsertRevendedor(Usuario usuarioRevendedor) {
        Revendedor revendedor = revendedorRepository.findByDominioIgnoreCase("www.navarroventas.com")
                .or(() -> revendedorRepository.findByDominioIgnoreCase("demo.revendedor.local"))
                .or(() -> revendedorRepository.findByUsuario_Id(usuarioRevendedor.getId()))
                .orElseGet(Revendedor::new);
        revendedor.setNombre("Navarro Ventas");
        revendedor.setDominio("www.navarroventas.com");
        revendedor.setActivo(true);
        revendedor.setUsuario(usuarioRevendedor);
        revendedor.setMarkupPorcentajeDefault(new BigDecimal("0.18"));
        revendedor.setMarkupFijoDefault(BigDecimal.ZERO);
        revendedor.setComisionAsesorPorcentajeDefault(new BigDecimal("0.10"));
        revendedor.setComisionAsesorFijaDefault(BigDecimal.ZERO);
        return revendedorRepository.save(revendedor);
    }

    private Revendedor upsertSegundoRevendedor(Usuario usuarioRevendedor) {
        Revendedor revendedor = revendedorRepository.findByDominioIgnoreCase("www.vidrieraurbana.com")
                .or(() -> revendedorRepository.findByUsuario_Id(usuarioRevendedor.getId()))
                .orElseGet(Revendedor::new);
        revendedor.setNombre("Vidriera Urbana");
        revendedor.setDominio("www.vidrieraurbana.com");
        revendedor.setActivo(true);
        revendedor.setUsuario(usuarioRevendedor);
        revendedor.setMarkupPorcentajeDefault(new BigDecimal("0.23"));
        revendedor.setMarkupFijoDefault(BigDecimal.ZERO);
        revendedor.setComisionAsesorPorcentajeDefault(new BigDecimal("0.12"));
        revendedor.setComisionAsesorFijaDefault(BigDecimal.ZERO);
        return revendedorRepository.save(revendedor);
    }

    private RevendedorStorefront upsertStorefront(Revendedor revendedor) {
        RevendedorStorefront storefront = revendedorStorefrontRepository.findByRevendedor_Id(revendedor.getId())
                .orElseGet(() -> {
                    RevendedorStorefront nuevo = new RevendedorStorefront();
                    nuevo.setRevendedor(revendedor);
                    return nuevo;
                });
        storefront.setNombreComercial("Navarro Ventas");
        storefront.setColorPrimario("#0f172a");
        storefront.setColorSecundario("#e2e8f0");
        storefront.setHeroTitulo("Navarro Ventas");
        storefront.setHeroSubtitulo("Un sitio de compra. De Francis Rafael Navarro.");
        storefront.setEmailComercial(null);
        storefront.setTelefonoComercial(null);
        storefront.setDireccionComercial("Buenos Aires");
        storefront.setFooterTexto("Navarro Ventas - Un sitio de compra");
        storefront.setPublicada(true);

        RevendedorStorefront storefrontGuardada = revendedorStorefrontRepository.save(storefront);
        revendedor.setStorefront(storefrontGuardada);
        return storefrontGuardada;
    }

    private RevendedorStorefront upsertSegundoStorefront(Revendedor revendedor) {
        RevendedorStorefront storefront = revendedorStorefrontRepository.findByRevendedor_Id(revendedor.getId())
                .orElseGet(() -> {
                    RevendedorStorefront nuevo = new RevendedorStorefront();
                    nuevo.setRevendedor(revendedor);
                    return nuevo;
                });
        storefront.setNombreComercial("Vidriera Urbana");
        storefront.setColorPrimario("#14532d");
        storefront.setColorSecundario("#dcfce7");
        storefront.setHeroTitulo("Vidriera Urbana");
        storefront.setHeroSubtitulo("Tecnologia y hogar curados para una segunda tienda white-label.");
        storefront.setEmailComercial("ventas@vidrieraurbana.com");
        storefront.setTelefonoComercial("+54 11 5555 2020");
        storefront.setDireccionComercial("Cordoba, Argentina");
        storefront.setFooterTexto("Vidriera Urbana - Tu storefront demo alternativo");
        storefront.setPublicada(true);

        RevendedorStorefront storefrontGuardada = revendedorStorefrontRepository.save(storefront);
        revendedor.setStorefront(storefrontGuardada);
        return storefrontGuardada;
    }

    private Proveedor upsertProveedor(Usuario usuarioProveedor) {
        Proveedor proveedor = proveedorRepository.findByUsuario_Id(usuarioProveedor.getId()).orElseGet(Proveedor::new);
        proveedor.setNombre("Proveedor Demo");
        proveedor.setActivo(true);
        proveedor.setUsuario(usuarioProveedor);
        return proveedorRepository.save(proveedor);
    }

    private ClienteRevendedor upsertClienteRevendedor(Usuario usuarioCliente, Revendedor revendedor) {
        ClienteRevendedor clienteRevendedor = clienteRevendedorRepository
                .findByUsuario_IdAndRevendedor_Id(usuarioCliente.getId(), revendedor.getId())
                .orElseGet(ClienteRevendedor::new);
        clienteRevendedor.setUsuario(usuarioCliente);
        clienteRevendedor.setRevendedor(revendedor);
        clienteRevendedor.setActivo(true);
        clienteRevendedor.setOrigenAlta("SEED_LOCAL");
        return clienteRevendedorRepository.save(clienteRevendedor);
    }

    private List<Producto> upsertCatalogoDemo(Proveedor proveedor, Revendedor revendedor) {
        List<Producto> productos = new ArrayList<>();
        for (int index = 1; index <= CANTIDAD_PRODUCTOS_CATALOGO; index++) {
            ProductoCanonico canonico = upsertProductoCanonico(index);
            Producto producto = upsertProducto(index, proveedor, canonico);
            upsertProveedorStock(index, proveedor, producto);
            upsertRevendedorMarkup(index, revendedor, producto);
            upsertProductoMedia(index, producto);
            productos.add(producto);
        }
        return productos;
    }

    private ProductoCanonico upsertProductoCanonico(int index) {
        String modelo = "DB-" + String.format("%04d", 1000 + index);
        ProductoCanonico canonico = productoCanonicoRepository.findAll().stream()
                .filter(item -> modelo.equalsIgnoreCase(item.getModelo()))
                .findFirst()
                .orElseGet(ProductoCanonico::new);
        canonico.setMarca("DemoBrand");
        canonico.setModelo(modelo);
        canonico.setNombrePublico(nombreProducto(index));
        canonico.setAtributosJson("""
                {"color":"%s","conexion":"bluetooth","segmento":"demo","indice":%d}
                """.formatted(colorProducto(index), index));
        canonico.setActivo(true);
        return productoCanonicoRepository.save(canonico);
    }

    private Producto upsertProducto(int index, Proveedor proveedor, ProductoCanonico canonico) {
        String sku = "DEMO-SKU-" + String.format("%04d", index);
        Producto producto = productoRepository.findByProveedor_IdAndSkuIgnoreCase(proveedor.getId(), sku).orElseGet(Producto::new);
        producto.setProveedor(proveedor);
        producto.setProductoCanonico(canonico);
        producto.setSku(sku);
        producto.setNombre(nombreProducto(index) + " Proveedor Demo");
        producto.setDescripcion("Producto demo " + index + " para pruebas end-to-end de catalogo, detalle y checkout.");
        producto.setPrecioCosto(precioCosto(index));
        producto.setMoneda("ARS");
        producto.setCategoriaPaquete(CategoriaPaquete.CHICO.name());
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private ProveedorStock upsertProveedorStock(int index, Proveedor proveedor, Producto producto) {
        ProveedorStock stock = proveedorStockRepository.findAll().stream()
                .filter(item -> item.getProveedor() != null
                        && item.getProducto() != null
                        && proveedor.getId().equals(item.getProveedor().getId())
                        && producto.getId().equals(item.getProducto().getId()))
                .findFirst()
                .orElseGet(ProveedorStock::new);
        stock.setProveedor(proveedor);
        stock.setProducto(producto);
        stock.setStock(12 + index);
        stock.setPrecioCostoActual(precioCosto(index));
        return proveedorStockRepository.save(stock);
    }

    private RevendedorMarkup upsertRevendedorMarkup(int index, Revendedor revendedor, Producto producto) {
        RevendedorMarkup markup = revendedorMarkupRepository
                .findByRevendedor_IdAndProducto_IdAndActivoTrue(revendedor.getId(), producto.getId())
                .orElseGet(RevendedorMarkup::new);
        markup.setRevendedor(revendedor);
        markup.setProducto(producto);
        markup.setMarkupPorcentaje(markupPorcentaje(index));
        markup.setMarkupFijo(BigDecimal.ZERO);
        markup.setComisionAsesorPorcentaje(new BigDecimal("0.10"));
        markup.setComisionAsesorFija(BigDecimal.ZERO);
        markup.setActivo(true);
        return revendedorMarkupRepository.save(markup);
    }

    private ProductoMedia upsertProductoMedia(int index, Producto producto) {
        ProductoMedia media = productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(producto.getId())
                .orElseGet(ProductoMedia::new);
        media.setProducto(producto);
        media.setTipoMedia(TipoMedia.IMAGEN);
        media.setUrl(imagenProducto(index));
        media.setOrden(1);
        media.setEsPrincipal(true);
        return productoMediaRepository.save(media);
    }

    private UUID seedUuid(String scope, int index) {
        return UUID.nameUUIDFromBytes(("seed-demo:" + scope + ":" + index).getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal precioCosto(int index) {
        return new BigDecimal(9000 + (index * 850) + ".00");
    }

    private BigDecimal markupPorcentaje(int index) {
        return switch (index % 4) {
            case 0 -> new BigDecimal("0.18");
            case 1 -> new BigDecimal("0.20");
            case 2 -> new BigDecimal("0.22");
            default -> new BigDecimal("0.25");
        };
    }

    private String nombreProducto(int index) {
        return switch (index) {
            case 1 -> "Auriculares Inalambricos Demo";
            case 2 -> "Parlante Portatil Demo";
            case 3 -> "Teclado Mecanico Demo";
            case 4 -> "Mouse Gamer Demo";
            case 5 -> "Monitor Full HD Demo";
            case 6 -> "Webcam USB Demo";
            case 7 -> "Microfono Streaming Demo";
            case 8 -> "Silla Oficina Demo";
            case 9 -> "Notebook Ultraliviana Demo";
            case 10 -> "Tablet Android Demo";
            case 11 -> "Smartwatch Deportivo Demo";
            case 12 -> "Impresora Multifuncion Demo";
            case 13 -> "Disco SSD Externo Demo";
            case 14 -> "Router WiFi 6 Demo";
            case 15 -> "Lampara LED Escritorio Demo";
            case 16 -> "Cafetera Capsulas Demo";
            case 17 -> "Aspiradora Robot Demo";
            case 18 -> "Freidora de Aire Demo";
            case 19 -> "Batidora Planetaria Demo";
            case 20 -> "Juego de Sartenes Demo";
            case 21 -> "Mochila Urbana Demo";
            case 22 -> "Valija Cabina Demo";
            case 23 -> "Campera Impermeable Demo";
            case 24 -> "Zapatillas Running Demo";
            case 25 -> "Bicicleta Plegable Demo";
            case 26 -> "Kit Herramientas Demo";
            case 27 -> "Taladro Percutor Demo";
            case 28 -> "Jardinera Electrica Demo";
            case 29 -> "Juego de Comedor Demo";
            case 30 -> "Mesa Ratona Demo";
            default -> "Producto Demo " + index;
        };
    }

    private String colorProducto(int index) {
        return switch (index % 5) {
            case 0 -> "negro";
            case 1 -> "gris";
            case 2 -> "azul";
            case 3 -> "blanco";
            default -> "verde";
        };
    }

    private String imagenProducto(int index) {
        String[] imagenes = {
                "https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60",
                "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60",
                "https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60",
                "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60",
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60",
                "https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60"
        };
        return imagenes[(index - 1) % imagenes.length] + "&sig=" + index;
    }

    private Transportista upsertTransportista() {
        Transportista transportista = transportistaRepository.findById(TRANSPORTISTA_ID).orElseGet(Transportista::new);
        transportista.setId(TRANSPORTISTA_ID);
        transportista.setNombre("Andreani");
        transportista.setActivo(true);
        return transportistaRepository.save(transportista);
    }

    private TarifaTransportista upsertTarifaTransportista(Transportista transportista) {
        TarifaTransportista tarifa = tarifaTransportistaRepository.findById(TARIFA_ID).orElseGet(TarifaTransportista::new);
        tarifa.setId(TARIFA_ID);
        tarifa.setTransportista(transportista);
        tarifa.setCategoriaPaquete(CategoriaPaquete.CHICO.name());
        tarifa.setZona("DEFAULT");
        tarifa.setPrecio(new BigDecimal("1500.00"));
        tarifa.setDemoraEstimadaDias(5);
        return tarifaTransportistaRepository.save(tarifa);
    }

    private Pedido upsertPedido(UUID id, Revendedor revendedor, Usuario cliente, PedidoEstado estadoPedido, BigDecimal totalFinal) {
        String trackingToken = id.toString();
        Pedido pedido = pedidoRepository.findByTrackingToken(trackingToken).orElseGet(Pedido::new);
        pedido.setRevendedor(revendedor);
        pedido.setCliente(cliente);
        pedido.setEstadoPedido(estadoPedido);
        pedido.setTrackingToken(trackingToken);
        pedido.setTotalBruto(totalFinal);
        pedido.setTotalPsp(BigDecimal.ZERO);
        pedido.setTotalEnvio(BigDecimal.ZERO);
        pedido.setTotalSeguro(BigDecimal.ZERO);
        pedido.setTotalFinal(totalFinal);
        return pedidoRepository.save(pedido);
    }

    private PedidoItem upsertPedidoItem(UUID id, Pedido pedido, Producto producto, Proveedor proveedor, Integer cantidad) {
        PedidoItem item = pedidoItemRepository.findAll().stream()
                .filter(existing -> existing.getPedido() != null
                        && existing.getProducto() != null
                        && pedido.getId().equals(existing.getPedido().getId())
                        && producto.getId().equals(existing.getProducto().getId()))
                .findFirst()
                .orElseGet(PedidoItem::new);
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setProveedor(proveedor);
        item.setCantidad(cantidad);
        item.setPrecioCosto(new BigDecimal("10000.00"));
        item.setMarkupAplicado(new BigDecimal("2000.00"));
        item.setPrecioItemFinal(new BigDecimal("12000.00"));
        item.setTieneSeguro(false);
        item.setMontoSeguro(BigDecimal.ZERO);
        return pedidoItemRepository.save(item);
    }

    private Pago upsertPago(UUID id, Pedido pedido, BigDecimal monto, PagoEstado estadoPago) {
        Pago pago = pagoRepository.findAll().stream()
                .filter(existing -> existing.getPedido() != null && pedido.getId().equals(existing.getPedido().getId()))
                .findFirst()
                .orElseGet(Pago::new);
        pago.setPedido(pedido);
        pago.setEstadoPago(estadoPago);
        pago.setMetodo("PSP");
        pago.setMonto(monto);
        pago.setPspReferencia("demo-" + pedido.getId());
        return pagoRepository.save(pago);
    }

    private Envio upsertEnvio(
            UUID id,
            Pedido pedido,
            Transportista transportista,
        EnvioEstado estadoEnvio,
        String tracking,
        String direccionEnvio) {
        Envio envio = envioRepository.findByPedido_Id(pedido.getId()).orElseGet(Envio::new);
        envio.setPedido(pedido);
        envio.setTransportista(transportista);
        envio.setCategoriaPaquete(CategoriaPaquete.CHICO.name());
        envio.setTracking(tracking);
        envio.setEstadoEnvio(estadoEnvio);
        envio.setDireccionEnvio(direccionEnvio);
        envio.setCostoEnvio(new BigDecimal("1500.00"));
        return envioRepository.save(envio);
    }

    private Notificacion upsertNotificacion(
            UUID id,
            Usuario usuario,
            String tipo,
            String titulo,
            String mensaje,
            boolean leida) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseGet(() -> notificacionRepository.findByUsuario_IdOrderByCreatedAtDesc(usuario.getId()).stream()
                        .filter(item -> tipo.equals(item.getTipo())
                                && titulo.equals(item.getTitulo())
                                && mensaje.equals(item.getMensaje()))
                        .findFirst()
                        .orElseGet(Notificacion::new));
        if (notificacion.getId() == null) {
            notificacion.setId(id);
        }
        notificacion.setUsuario(usuario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setLeida(leida);
        return notificacionRepository.save(notificacion);
    }
}
