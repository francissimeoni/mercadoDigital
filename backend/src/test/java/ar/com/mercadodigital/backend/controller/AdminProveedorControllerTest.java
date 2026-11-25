package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoOAuthLinkResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminProveedorDespachoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontResponse;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.service.AdminProveedorService;
import ar.com.mercadodigital.backend.service.AdminProveedorStorefrontService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.ProveedorMercadoPagoService;

@WebMvcTest(AdminProveedorController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProveedorMercadoPagoService proveedorMercadoPagoService;
    @MockBean
    private AdminProveedorService adminProveedorService;
    @MockBean
    private AdminProveedorStorefrontService adminProveedorStorefrontService;
    @MockBean
    private MercadoPagoOAuthService mercadoPagoOAuthService;

    @Test
    void estadoMercadoPagoProveedorDevuelve200() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(proveedorMercadoPagoService.getEstado(eq(proveedorId)))
                .thenReturn(AdminRevendedorMercadoPagoResponse.builder()
                        .estado("PENDIENTE")
                        .readyForCheckout(false)
                        .build());

        mockMvc.perform(get("/admin/proveedores/{id}/mercadopago", proveedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void upsertMercadoPagoProveedorDevuelve200() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(proveedorMercadoPagoService.upsert(eq(proveedorId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminRevendedorMercadoPagoResponse.builder()
                        .estado("VINCULADA")
                        .readyForCheckout(true)
                        .build());

        String body = """
                {
                  "mercadoPagoUserId": "collector-1",
                  "publicKey": "APP_USR-demo",
                  "accessToken": "token-demo",
                  "refreshToken": "refresh-demo",
                  "tokenExpiresAt": "2026-10-01T10:00:00",
                  "liveMode": false,
                  "scope": "offline_access payments write"
                }
                """;

        mockMvc.perform(put("/admin/proveedores/{id}/mercadopago", proveedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VINCULADA"));
    }

    @Test
    void oauthLinkProveedorDevuelve200() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(mercadoPagoOAuthService.generarAuthorizationLinkProveedor(eq(proveedorId)))
                .thenReturn(AdminRevendedorMercadoPagoOAuthLinkResponse.builder()
                        .authorizationUrl("https://auth.mercadopago.com.ar/authorization?client_id=123")
                        .redirectUri("http://localhost:8081/public/mercadopago/oauth/callback")
                        .expiresAt(java.time.LocalDateTime.of(2026, 4, 26, 18, 0))
                        .build());

        mockMvc.perform(get("/admin/proveedores/{id}/mercadopago/oauth-link", proveedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl")
                        .value("https://auth.mercadopago.com.ar/authorization?client_id=123"));
    }

    @Test
    void getDespachoConfigProveedorDevuelve200() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(adminProveedorService.getDespacho(eq(proveedorId)))
                .thenReturn(AdminProveedorDespachoResponse.builder()
                        .proveedorId(proveedorId)
                        .nombre("Proveedor demo")
                        .despachaMercaderia(true)
                        .build());

        mockMvc.perform(get("/admin/proveedores/{id}/despacho-config", proveedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.despachaMercaderia").value(true));
    }

    @Test
    void actualizarDespachoConfigProveedorDevuelve200() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(adminProveedorService.actualizarDespacho(eq(proveedorId), eq(true)))
                .thenReturn(AdminProveedorDespachoResponse.builder()
                        .proveedorId(proveedorId)
                        .nombre("Proveedor demo")
                        .despachaMercaderia(true)
                        .build());

        String body = """
                {
                  "despachaMercaderia": true
                }
                """;

        mockMvc.perform(put("/admin/proveedores/{id}/despacho-config", proveedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.despachaMercaderia").value(true));
    }

    @Test
    void getStorefrontProveedorDevuelve200() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(adminProveedorStorefrontService.getStorefront(eq(proveedorId)))
                .thenReturn(AdminStorefrontResponse.builder()
                        .id(UUID.randomUUID())
                        .tipoStorefront(TipoStorefront.PROVEEDOR)
                        .dominioPublico("proveedor.demo.com")
                        .nombreComercial("Tienda proveedor")
                        .publicada(true)
                        .build());

        mockMvc.perform(get("/admin/proveedores/{id}/storefront", proveedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoStorefront").value("PROVEEDOR"))
                .andExpect(jsonPath("$.dominioPublico").value("proveedor.demo.com"));
    }

    @Test
    void upsertStorefrontProveedorDevuelve200() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(adminProveedorStorefrontService.upsertStorefront(eq(proveedorId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminStorefrontResponse.builder()
                        .id(UUID.randomUUID())
                        .tipoStorefront(TipoStorefront.PROVEEDOR)
                        .dominioPublico("proveedor.demo.com")
                        .nombreComercial("Tienda proveedor")
                        .publicada(true)
                        .build());

        String body = """
                {
                  "dominioPublico": "proveedor.demo.com",
                  "nombreComercial": "Tienda proveedor",
                  "publicada": true
                }
                """;

        mockMvc.perform(put("/admin/proveedores/{id}/storefront", proveedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoStorefront").value("PROVEEDOR"))
                .andExpect(jsonPath("$.publicada").value(true));
    }
}
