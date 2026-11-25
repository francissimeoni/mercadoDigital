package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.config.SecurityConfig;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorResponse;
import ar.com.mercadodigital.backend.dto.response.AuthResponse;
import ar.com.mercadodigital.backend.dto.response.EnvioResponse;
import ar.com.mercadodigital.backend.dto.response.NotificacionResponse;
import ar.com.mercadodigital.backend.dto.response.PublicCatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoDirectoResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoPagoPspResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorPricingConfigResponse;
import ar.com.mercadodigital.backend.dto.response.RmaResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorPricingPreviewItemResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.security.JwtFilter;
import ar.com.mercadodigital.backend.security.JwtService;
import ar.com.mercadodigital.backend.service.AdminRevendedorService;
import ar.com.mercadodigital.backend.service.AdminRevendedorProductoImportService;
import ar.com.mercadodigital.backend.service.AuthService;
import ar.com.mercadodigital.backend.service.BandejasOperativasService;
import ar.com.mercadodigital.backend.service.CatalogoService;
import ar.com.mercadodigital.backend.service.CategoriaProductoService;
import ar.com.mercadodigital.backend.service.CostoOperativoMdService;
import ar.com.mercadodigital.backend.service.EnvioService;
import ar.com.mercadodigital.backend.service.LiquidacionProveedorService;
import ar.com.mercadodigital.backend.service.MercadoPagoClient;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.MercadoPagoWebhookAdapter;
import ar.com.mercadodigital.backend.service.MercadoPagoWebhookSignatureVerifier;
import ar.com.mercadodigital.backend.service.NotificationService;
import ar.com.mercadodigital.backend.service.OfertaProductoCanonicoService;
import ar.com.mercadodigital.backend.service.ProductoMediaService;
import ar.com.mercadodigital.backend.service.ProductoService;
import ar.com.mercadodigital.backend.service.ProveedorOwnershipService;
import ar.com.mercadodigital.backend.service.ProveedorProductoImportService;
import ar.com.mercadodigital.backend.service.ProveedorProductoService;
import ar.com.mercadodigital.backend.service.PublicCatalogoService;
import ar.com.mercadodigital.backend.service.PublicPedidoDirectoService;
import ar.com.mercadodigital.backend.service.PublicPedidoPagoPspService;
import ar.com.mercadodigital.backend.service.PublicPedidoTrackingService;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import ar.com.mercadodigital.backend.service.RevendedorPricingV1Service;
import ar.com.mercadodigital.backend.service.RevendedorAsesorService;
import ar.com.mercadodigital.backend.service.RevendedorConsultasService;
import ar.com.mercadodigital.backend.service.RevendedorOwnershipService;
import ar.com.mercadodigital.backend.service.RevendedorProductoService;
import ar.com.mercadodigital.backend.service.RotuloEnvioService;
import ar.com.mercadodigital.backend.service.RotuloRmaService;
import ar.com.mercadodigital.backend.service.RmaService;
import ar.com.mercadodigital.backend.service.PspConfigService;
import ar.com.mercadodigital.backend.service.CorreoMdConfigService;
import ar.com.mercadodigital.backend.service.MercadoPagoMdConfigService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import ar.com.mercadodigital.backend.service.StorefrontAssetStorageService;
import ar.com.mercadodigital.backend.service.TransportistaService;
import ar.com.mercadodigital.backend.service.WebhookPspService;
import ar.com.mercadodigital.backend.service.TransferenciaService;
import ar.com.mercadodigital.backend.service.NotificacionConsultaEmailService;
import ar.com.mercadodigital.backend.service.StorefrontMetricasService;

@WebMvcTest(controllers = { AuthController.class, WebhookPspController.class, PublicMercadoPagoOAuthController.class, CatalogoController.class, RmaController.class,
        StorefrontClienteCuentaController.class, StorefrontController.class, PublicStorefrontMetricasController.class, PublicCatalogoController.class, ProductoController.class,
        PublicPedidoDirectoController.class, PublicPedidoPagoPspController.class, PublicPedidoTrackingController.class, ProductoCanonicoController.class, ProductoMediaController.class, NotificacionController.class,
        AdminRevendedorController.class,
        AdminConfiguracionController.class,
        ProveedorController.class, RevendedorController.class, RevendedorPricingController.class, LogisticaController.class, EnvioController.class }, properties = {
        "app.security.enabled=true",
        "app.security.jwt-secret=test-secret"
})
@Import({ SecurityConfig.class, JwtFilter.class, JwtService.class })
@org.springframework.test.context.TestPropertySource(properties = "mercadopago.enabled=true")
class SecurityAccessTest {

    @MockBean
    private ar.com.mercadodigital.backend.service.StorefrontClienteCuentaService customerAccount;

    @Test
    void cuentaClienteExigeRolClienteYNoPermiteOperacionesPropietario() throws Exception {
        String customer = jwtService.generarToken(UUID.randomUUID(), "customer@test.com", List.of("CLIENTE"));
        String owner = jwtService.generarToken(UUID.randomUUID(), "owner@test.com", List.of("REVENDEDOR"));
        mockMvc.perform(get("/cliente/storefront/pedidos").param("storefrontHost", "shop.test")).andExpect(status().isForbidden());
        mockMvc.perform(get("/cliente/storefront/pedidos").param("storefrontHost", "shop.test").header("Authorization", "Bearer " + owner)).andExpect(status().isForbidden());
        mockMvc.perform(get("/cliente/storefront/pedidos").param("storefrontHost", "shop.test").header("Authorization", "Bearer " + customer)).andExpect(status().isOk());
        org.mockito.Mockito.verify(customerAccount).listarPedidos("shop.test", "customer@test.com");
        mockMvc.perform(put("/revendedores/{id}/productos-propios/precios/ajuste-masivo", UUID.randomUUID()).header("Authorization", "Bearer " + customer).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}")).andExpect(status().isForbidden());
        mockMvc.perform(put("/revendedores/{id}/pedidos/{pedido}/cancelar-impago", UUID.randomUUID(), UUID.randomUUID()).header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private AdminRevendedorService adminRevendedorService;
    @MockBean
    private AdminRevendedorProductoImportService adminRevendedorProductoImportService;
    @MockBean
    private CostoOperativoMdService costoOperativoMdService;
    @MockBean
    private PspConfigService pspConfigService;
    @MockBean
    private CorreoMdConfigService correoMdConfigService;
    @MockBean
    private MercadoPagoMdConfigService mercadoPagoMdConfigService;
    @MockBean
    private AuthService authService;
    @MockBean
    private WebhookPspService webhookPspService;
    @MockBean
    private MercadoPagoWebhookAdapter mercadoPagoWebhookAdapter;
    @MockBean
    private MercadoPagoWebhookSignatureVerifier signatureVerifier;
    @MockBean
    private MercadoPagoClient mercadoPagoClient;
    @MockBean
    private MercadoPagoOAuthService mercadoPagoOAuthService;
    @MockBean
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @MockBean
    private StorefrontMetricasService storefrontMetricasService;
    @MockBean
    private CatalogoService catalogoService;
    @MockBean
    private PublicCatalogoService publicCatalogoService;
    @MockBean
    private PublicPedidoDirectoService publicPedidoDirectoService;
    @MockBean
    private PublicPedidoPagoPspService publicPedidoPagoPspService;
    @MockBean
    private PublicPedidoTrackingService publicPedidoTrackingService;
    @MockBean
    private RmaService rmaService;
    @MockBean
    private StorefrontService storefrontService;
    @MockBean
    private StorefrontAssetStorageService storefrontAssetStorageService;
    @MockBean
    private ProductoService productoService;
    @MockBean
    private OfertaProductoCanonicoService ofertaProductoCanonicoService;
    @MockBean
    private ProductoMediaService productoMediaService;
    @MockBean
    private NotificationService notificationService;
    @MockBean
    private BandejasOperativasService bandejasOperativasService;
    @MockBean
    private ProveedorOwnershipService proveedorOwnershipService;
    @MockBean
    private ProveedorProductoService proveedorProductoService;
    @MockBean
    private CategoriaProductoService categoriaProductoService;
    @MockBean
    private ProveedorProductoImportService proveedorProductoImportService;
    @MockBean
    private TransportistaService transportistaService;
    @MockBean
    private EnvioService envioService;
    @MockBean
    private LiquidacionProveedorService liquidacionProveedorService;
    @MockBean
    private RotuloEnvioService rotuloEnvioService;
    @MockBean
    private RotuloRmaService rotuloRmaService;
    @MockBean
    private RevendedorPricingV1Service revendedorPricingV1Service;
    @MockBean
    private RevendedorAsesorService revendedorAsesorService;
    @MockBean
    private RevendedorConsultasService revendedorConsultasService;
    @MockBean
    private RevendedorOwnershipService revendedorOwnershipService;
    @MockBean
    private RevendedorProductoService revendedorProductoService;
    @MockBean
    private ar.com.mercadodigital.backend.service.RevendedorPedidoService revendedorPedidoService;

    @MockBean
    private TransferenciaService transferenciaService;
    @MockBean
    private NotificacionConsultaEmailService notificacionConsultaEmailService;

    @Test
    void endpointsPublicosPermitenAccesoSinToken() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.builder()
                .token("jwt")
                .email("usuario@test.com")
                .issuedAt(java.time.LocalDateTime.now())
                .revendedorId(null)
                .proveedorId(null)
                .clienteId(null)
                .build());
        doNothing().when(webhookPspService).procesarWebhook(any());
        when(storefrontService.resolverPorHost("mitienda.com"))
                .thenReturn(StorefrontResponse.builder()
                        .revendedorId(UUID.randomUUID())
                        .dominio("mitienda.com")
                        .nombreComercial("Mi Tienda")
                        .publicada(true)
                        .build());
        when(publicCatalogoService.getCatalogo(any()))
                .thenReturn(List.of(PublicCatalogoItemResponse.builder()
                        .productoCanonicoId(UUID.randomUUID())
                        .nombrePublico("Producto publico")
                        .build()));
        when(publicPedidoDirectoService.crearPedidoDirecto(any()))
                .thenReturn(PublicPedidoDirectoResponse.builder()
                        .pedidoId(UUID.randomUUID())
                        .estadoPedido("CREADO")
                        .estadoPago("PENDIENTE")
                        .metodoPago("PSP")
                        .trackingToken("tracking-publico-1")
                        .build());
        when(publicPedidoPagoPspService.prepararCheckout(any()))
                .thenReturn(PublicPedidoPagoPspResponse.builder()
                        .pedidoId(UUID.randomUUID())
                        .estadoPedido("CREADO")
                        .estadoPago("PENDIENTE")
                        .metodoPago("PSP")
                        .monto(java.math.BigDecimal.TEN)
                        .referenciaPedido(UUID.randomUUID().toString())
                        .referenciaPago("psp-publico-ref")
                        .build());
        when(publicPedidoTrackingService.getByTrackingToken("tracking-publico-1", "mitienda.com"))
                .thenReturn(PublicPedidoTrackingResponse.builder()
                        .pedidoId(UUID.randomUUID())
                        .estadoPedido("CREADO")
                        .build());

        String loginBody = """
                {
                  "email": "usuario@test.com",
                  "password": "123456"
                }
                """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk());

        String webhookBody = """
                {
                  "idEventoPsp": "evt_1",
                  "tipoEvento": "PAGO_CONFIRMADO",
                  "referenciaPedido": "%s",
                  "referenciaPago": "pago_1",
                  "monto": 100.00,
                  "estado": "CONFIRMADO"
                }
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/webhooks/psp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isForbidden());

        MercadoPagoWebhookAdapter.MercadoPagoWebhookNotification notification =
                new MercadoPagoWebhookAdapter.MercadoPagoWebhookNotification("notif_1", "pay_1", "payment", "payment.updated", "collector-1");
        when(mercadoPagoWebhookAdapter.extraerNotificacion(any(), any(), any(), any(), any())).thenReturn(notification);
        when(mercadoPagoWebhookAdapter.esNotificacionDePago(notification)).thenReturn(true);
        when(revendedorMercadoPagoService.resolveForMercadoPagoUserId("collector-1"))
                .thenReturn(new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("seller-token", "collector-1", false, true));
        when(mercadoPagoClient.obtenerPago("pay_1", "seller-token")).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                {
                  "id": "pay_1",
                  "status": "approved",
                  "collector_id": "collector-1",
                  "currency_id": "ARS",
                  "transaction_amount": 100.00,
                  "external_reference": "%s"
                }
                """.formatted(UUID.randomUUID())));
        when(mercadoPagoWebhookAdapter.adaptarPago(any(), any())).thenReturn(new ar.com.mercadodigital.backend.dto.request.WebhookPspRequest() {{
            setIdEventoPsp("mercadopago-notif_1");
            setTipoEvento("PAGO_CONFIRMADO");
            setReferenciaPedido(UUID.randomUUID().toString());
            setReferenciaPago("pay_1");
            setMonto(new java.math.BigDecimal("100.00"));
            setEstado("CONFIRMADO");
        }});

        mockMvc.perform(post("/webhooks/psp/mercadopago")
                        .param("data.id", "pay_1")
                        .header("x-signature", "firma-sintetica")
                        .header("x-request-id", "solicitud-sintetica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "payment",
                                  "action": "payment.updated",
                                  "data": {
                                    "id": "pay_1"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        when(mercadoPagoOAuthService.buildAdminErrorRedirectUrlFromState("state-demo", "access_denied"))
                .thenReturn("http://localhost:5173/admin/storefronts?mp_oauth_status=error");
        mockMvc.perform(get("/public/mercadopago/oauth/callback")
                        .param("state", "state-demo")
                        .param("error", "access_denied"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/public/storefront/resolve").param("host", "mitienda.com"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/public/storefront/metricas/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "mitienda.com",
                                  "tipo": "VISITA",
                                  "visitanteId": "browser-test"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/public/catalogo").param("revendedorId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        String pedidoPublicoBody = """
                {
                  "revendedorId": "%s",
                  "nombreApellido": "Juan Perez",
                  "email": "cliente@test.com",
                  "telefonoWhatsapp": "1122334455",
                  "provincia": "Buenos Aires",
                  "localidad": "La Plata",
                  "codigoPostal": "1900",
                  "calle": "Calle 50",
                  "numero": "123",
                  "productoCanonicoId": "%s",
                  "cantidad": 1,
                  "seguroTransporte": true,
                  "aceptacionTerminos": true
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/public/pedido/directo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoPublicoBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/public/pedido/{pedidoId}/pago", UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/public/pedido/{trackingToken}", "tracking-publico-1")
                        .param("storefrontHost", "mitienda.com"))
                .andExpect(status().isOk());
    }

    @Test
    void catalogoExigeRolRevendedor() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(catalogoService.getCatalogo(revendedorId)).thenReturn(List.of());

        mockMvc.perform(get("/catalogo").param("revendedorId", revendedorId.toString()))
                .andExpect(status().isForbidden());

        String tokenRevendedor = jwtService.generarToken(
                UUID.randomUUID(),
                "revendedor@test.com",
                List.of("REVENDEDOR"));
        mockMvc.perform(get("/catalogo")
                        .param("revendedorId", revendedorId.toString())
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isOk());
    }

    @Test
    void rmaExigeRolAdmin() throws Exception {
        UUID rmaId = UUID.randomUUID();
        when(rmaService.aprobar(rmaId)).thenReturn(RmaResponse.builder()
                .id(rmaId)
                .estadoRma("APROBADO")
                .build());

        String tokenRevendedor = jwtService.generarToken(
                UUID.randomUUID(),
                "revendedor@test.com",
                List.of("REVENDEDOR"));
        mockMvc.perform(post("/rma/{id}/aprobar", rmaId)
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());

        String tokenAdmin = jwtService.generarToken(
                UUID.randomUUID(),
                "admin@test.com",
                List.of("ADMIN_MD"));
        mockMvc.perform(post("/rma/{id}/aprobar", rmaId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void notificacionesExigenAutenticacion() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(notificationService.getByUsuarioId(usuarioId)).thenReturn(List.of(
                NotificacionResponse.builder()
                        .id(UUID.randomUUID())
                        .titulo("Nueva notificacion")
                        .mensaje("Mensaje")
                        .leida(false)
                        .build()));

        mockMvc.perform(get("/notificaciones/{usuarioId}", usuarioId))
                .andExpect(status().isForbidden());

        String tokenProveedor = jwtService.generarToken(
                UUID.randomUUID(),
                "proveedor@test.com",
                List.of("PROVEEDOR"));
        mockMvc.perform(get("/notificaciones/{usuarioId}", usuarioId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isOk());
    }

    @Test
    void proveedorYListoParaRetiroExigenRolProveedor() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        when(bandejasOperativasService.getPedidosPendientesProveedor(proveedorId)).thenReturn(List.of());
        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(new ar.com.mercadodigital.backend.entity.Proveedor());
        when(envioService.marcarListoParaRetiro(pedidoId)).thenReturn(EnvioResponse.builder()
                .pedidoId(pedidoId)
                .estadoEnvio("LISTO_PARA_RETIRO")
                .build());
        when(proveedorProductoService.getProductos(eq(proveedorId), any())).thenReturn(List.of(
                ProveedorProductoResponse.builder()
                        .id(productoId)
                        .proveedorId(proveedorId)
                        .sku("SKU-1")
                        .nombre("Producto proveedor")
                        .estadoNormalizacion("PENDIENTE")
                        .build()));
        when(proveedorProductoService.getMedia(eq(proveedorId), eq(productoId), any())).thenReturn(List.of(
                ProductoMediaResponse.builder()
                        .id(mediaId)
                        .productoId(productoId)
                        .tipoMedia("IMAGEN")
                        .url("https://cdn.local/demo.png")
                        .orden(1)
                        .esPrincipal(true)
                        .build()));
        when(categoriaProductoService.getCategoriasActivasProveedor(eq(proveedorId), any())).thenReturn(List.of(
                ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoResponse.builder()
                        .id(UUID.randomUUID())
                        .nombre("Electrohogar")
                        .build()));
        when(categoriaProductoService.sugerirCategoriaProveedor(eq(proveedorId), any(), any())).thenReturn(
                ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoSugerenciaResponse.builder()
                        .categoriaId(UUID.randomUUID())
                        .nombre("Electrohogar")
                        .activa(false)
                        .estado("CREADA_PENDIENTE")
                        .mensaje("Categoria sugerida correctamente.")
                        .build());
        when(proveedorProductoService.crearProducto(eq(proveedorId), any(), any())).thenReturn(
                ProveedorProductoResponse.builder()
                        .id(productoId)
                        .proveedorId(proveedorId)
                        .sku("SKU-1")
                        .nombre("Producto proveedor")
                        .estadoNormalizacion("PENDIENTE")
                        .build());
        when(proveedorProductoService.actualizarProducto(eq(proveedorId), eq(productoId), any(), any())).thenReturn(
                ProveedorProductoResponse.builder()
                        .id(productoId)
                        .proveedorId(proveedorId)
                        .sku("SKU-1")
                        .nombre("Producto proveedor")
                        .estadoNormalizacion("ASOCIADO")
                        .build());
        when(proveedorProductoService.addMedia(eq(proveedorId), eq(productoId), any(), any())).thenReturn(
                ProductoMediaResponse.builder()
                        .id(mediaId)
                        .productoId(productoId)
                        .tipoMedia("IMAGEN")
                        .url("https://cdn.local/demo.png")
                        .orden(1)
                        .esPrincipal(true)
                        .build());
        doNothing().when(proveedorProductoService).deleteMedia(eq(proveedorId), eq(productoId), eq(mediaId), any());

        mockMvc.perform(get("/proveedores/{proveedorId}/pedidos-pendientes", proveedorId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/proveedores/{proveedorId}/productos", proveedorId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/proveedores/{proveedorId}/categorias-producto", proveedorId))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/envios/{pedidoId}/listo-para-retiro", pedidoId))
                .andExpect(status().isForbidden());

        String tokenRevendedor = jwtService.generarToken(
                UUID.randomUUID(),
                "revendedor@test.com",
                List.of("REVENDEDOR"));
        mockMvc.perform(get("/proveedores/{proveedorId}/pedidos-pendientes", proveedorId)
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/proveedores/{proveedorId}/productos", proveedorId)
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/proveedores/{proveedorId}/categorias-producto", proveedorId)
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/envios/{pedidoId}/listo-para-retiro", pedidoId)
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());

        String tokenProveedor = jwtService.generarToken(
                UUID.randomUUID(),
                "proveedor@test.com",
                List.of("PROVEEDOR"));
        mockMvc.perform(get("/proveedores/{proveedorId}/pedidos-pendientes", proveedorId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isOk());
        mockMvc.perform(get("/proveedores/{proveedorId}/productos", proveedorId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isOk());
        mockMvc.perform(get("/proveedores/{proveedorId}/categorias-producto", proveedorId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isOk());
        mockMvc.perform(post("/proveedores/{proveedorId}/categorias-producto/sugerencias", proveedorId)
                        .header("Authorization", "Bearer " + tokenProveedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Electrohogar" }
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/proveedores/{proveedorId}/productos", proveedorId)
                        .header("Authorization", "Bearer " + tokenProveedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"SKU-1",
                                  "nombre":"Producto proveedor",
                                  "precioCosto":1500.00,
                                  "stock":12,
                                  "categoriaPaquete":"CHICO"
                                }
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/proveedores/{proveedorId}/productos/{productoId}", proveedorId, productoId)
                        .header("Authorization", "Bearer " + tokenProveedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"SKU-1",
                                  "nombre":"Producto proveedor editado",
                                  "precioCosto":1800.00,
                                  "stock":8,
                                  "categoriaPaquete":"CHICO"
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/proveedores/{proveedorId}/productos/{productoId}/media", proveedorId, productoId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isOk());
        mockMvc.perform(post("/proveedores/{proveedorId}/productos/{productoId}/media", proveedorId, productoId)
                        .header("Authorization", "Bearer " + tokenProveedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoMedia":"IMAGEN",
                                  "url":"https://cdn.local/demo.png",
                                  "orden":1,
                                  "esPrincipal":true
                                }
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/proveedores/{proveedorId}/productos/{productoId}/media/{mediaId}", proveedorId, productoId, mediaId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/envios/{pedidoId}/listo-para-retiro", pedidoId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isOk());

        String tokenAdmin = jwtService.generarToken(
                UUID.randomUUID(),
                "admin@test.com",
                List.of("ADMIN_MD"));
        mockMvc.perform(get("/proveedores/{proveedorId}/productos", proveedorId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void logisticaYEnviosExigenRolLogistica() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(bandejasOperativasService.getRetirosPendientes()).thenReturn(List.of());
        when(envioService.getByPedidoId(pedidoId)).thenReturn(EnvioResponse.builder()
                .pedidoId(pedidoId)
                .estadoEnvio("RETIRADO")
                .build());

        mockMvc.perform(get("/logistica/retiros-pendientes"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/envios/{pedidoId}", pedidoId))
                .andExpect(status().isForbidden());

        String tokenProveedor = jwtService.generarToken(
                UUID.randomUUID(),
                "proveedor@test.com",
                List.of("PROVEEDOR"));
        mockMvc.perform(get("/logistica/retiros-pendientes")
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/envios/{pedidoId}", pedidoId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isForbidden());

        String tokenLogistica = jwtService.generarToken(
                UUID.randomUUID(),
                "logistica@test.com",
                List.of("LOGISTICA_MD"));
        mockMvc.perform(get("/logistica/retiros-pendientes")
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk());
        mockMvc.perform(get("/envios/{pedidoId}", pedidoId)
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk());
    }

    @Test
    void endpointsOperativosSensiblesQuedanCerradosYSoloAdminPuedeAcceder() throws Exception {
        UUID productoId = UUID.randomUUID();
        UUID productoCanonicoId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        UUID revendedorId = UUID.randomUUID();

        when(productoService.getProductos()).thenReturn(List.of(
                ProductoResponse.builder()
                        .id(productoId)
                        .nombre("Producto interno")
                        .build()));
        when(productoService.getProductoById(productoId)).thenReturn(ProductoResponse.builder()
                .id(productoId)
                .nombre("Producto interno")
                .build());
        when(ofertaProductoCanonicoService.getOfertas(productoCanonicoId, revendedorId)).thenReturn(List.of());
        when(productoMediaService.getMedia(productoId)).thenReturn(List.of(
                ProductoMediaResponse.builder()
                        .id(mediaId)
                        .productoId(productoId)
                        .url("https://cdn.local/media.png")
                        .esPrincipal(true)
                        .build()));
        doNothing().when(productoMediaService).deleteMedia(mediaId);

        mockMvc.perform(get("/productos"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/productos/{id}", productoId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/productos-canonicos/{id}/ofertas", productoCanonicoId)
                        .param("revendedorId", revendedorId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/productos/{id}/media", productoId))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/media/{id}", mediaId))
                .andExpect(status().isForbidden());

        String tokenRevendedor = jwtService.generarToken(
                UUID.randomUUID(),
                "revendedor@test.com",
                List.of("REVENDEDOR"));
        mockMvc.perform(get("/productos")
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());

        String tokenAdmin = jwtService.generarToken(
                UUID.randomUUID(),
                "admin@test.com",
                List.of("ADMIN_MD"));
        mockMvc.perform(get("/productos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/productos/{id}", productoId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/productos-canonicos/{id}/ofertas", productoCanonicoId)
                        .param("revendedorId", revendedorId.toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/productos/{id}/media", productoId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/media/{id}", mediaId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminRevendedoresExigeRolAdmin() throws Exception {
        when(adminRevendedorService.getRevendedores()).thenReturn(List.of(
                AdminRevendedorResponse.builder()
                        .id(UUID.randomUUID())
                        .nombre("Tienda Demo")
                        .dominio("mitienda.com")
                        .activo(true)
                        .build()));

        mockMvc.perform(get("/admin/revendedores"))
                .andExpect(status().isForbidden());

        String tokenRevendedor = jwtService.generarToken(
                UUID.randomUUID(),
                "revendedor@test.com",
                List.of("REVENDEDOR"));
        mockMvc.perform(get("/admin/revendedores")
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());

        String tokenAdmin = jwtService.generarToken(
                UUID.randomUUID(),
                "admin@test.com",
                List.of("ADMIN_MD"));
        mockMvc.perform(get("/admin/revendedores")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void adminConfiguracionExigeRolAdmin() throws Exception {
        when(costoOperativoMdService.getConfiguracionAdmin()).thenReturn(
                ar.com.mercadodigital.backend.dto.response.AdminCostoMdConfigResponse.builder()
                        .items(List.of())
                        .build());

        mockMvc.perform(get("/admin/configuracion/costo-md"))
                .andExpect(status().isForbidden());

        String tokenRevendedor = jwtService.generarToken(
                UUID.randomUUID(),
                "revendedor@test.com",
                List.of("REVENDEDOR"));
        mockMvc.perform(get("/admin/configuracion/costo-md")
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isForbidden());

        String tokenAdmin = jwtService.generarToken(
                UUID.randomUUID(),
                "admin@test.com",
                List.of("ADMIN_MD"));
        mockMvc.perform(get("/admin/configuracion/costo-md")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void pricingRevendedorExigeRolRevendedor() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(revendedorPricingV1Service.getConfiguracion(revendedorId))
                .thenReturn(RevendedorPricingConfigResponse.builder()
                        .revendedorId(revendedorId)
                        .markupPorcentaje(java.math.BigDecimal.ZERO)
                        .markupFijo(java.math.BigDecimal.ZERO)
                        .comisionAsesorPorcentaje(java.math.BigDecimal.ZERO)
                        .comisionAsesorFija(java.math.BigDecimal.ZERO)
                        .build());
        when(revendedorPricingV1Service.getPreview(revendedorId))
                .thenReturn(List.of(RevendedorPricingPreviewItemResponse.builder()
                        .productoCanonicoId(UUID.randomUUID())
                        .productoBaseId(UUID.randomUUID())
                        .nombrePublico("Producto")
                        .costoOperativo(java.math.BigDecimal.TEN)
                        .markupPorcentaje(java.math.BigDecimal.ZERO)
                        .markupFijo(java.math.BigDecimal.ZERO)
                        .comisionAsesorPorcentaje(java.math.BigDecimal.ZERO)
                        .comisionAsesorFija(java.math.BigDecimal.ZERO)
                        .markupBruto(java.math.BigDecimal.ZERO)
                        .comisionAsesorMonto(java.math.BigDecimal.ZERO)
                        .gananciaNetaRevendedor(java.math.BigDecimal.ZERO)
                        .precioFinalEstimado(java.math.BigDecimal.TEN)
                        .demoraEstimadaDias(5)
                        .stockDisponible(3)
                        .overrideActivo(false)
                        .origenConfiguracion("GLOBAL")
                        .build()));

        mockMvc.perform(get("/revendedores/{revendedorId}/pricing-v1/config", revendedorId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/revendedores/{revendedorId}/pricing-v1/preview", revendedorId))
                .andExpect(status().isForbidden());

        String tokenRevendedor = jwtService.generarToken(
                UUID.randomUUID(),
                "revendedor@test.com",
                List.of("REVENDEDOR"));
        mockMvc.perform(get("/revendedores/{revendedorId}/pricing-v1/config", revendedorId)
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isOk());
        mockMvc.perform(get("/revendedores/{revendedorId}/pricing-v1/preview", revendedorId)
                        .header("Authorization", "Bearer " + tokenRevendedor))
                .andExpect(status().isOk());
    }
}
