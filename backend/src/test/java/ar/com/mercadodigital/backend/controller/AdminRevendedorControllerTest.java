package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.AdminRevendedorResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoOAuthLinkResponse;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontAssetResponse;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontResponse;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.service.AdminRevendedorService;
import ar.com.mercadodigital.backend.service.AdminRevendedorProductoImportService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import ar.com.mercadodigital.backend.service.StorefrontAssetStorageService;

@WebMvcTest(AdminRevendedorController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminRevendedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminRevendedorService adminRevendedorService;
    @MockBean
    private AdminRevendedorProductoImportService adminRevendedorProductoImportService;
    @MockBean
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @MockBean
    private MercadoPagoOAuthService mercadoPagoOAuthService;
    @MockBean
    private StorefrontAssetStorageService storefrontAssetStorageService;

    @Test
    void listadoAdminDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(adminRevendedorService.getRevendedores()).thenReturn(List.of(
                AdminRevendedorResponse.builder()
                        .id(revendedorId)
                        .nombre("Tienda Demo")
                        .dominio("mitienda.com")
                        .activo(true)
                        .build()));

        mockMvc.perform(get("/admin/revendedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(revendedorId.toString()))
                .andExpect(jsonPath("$[0].dominio").value("mitienda.com"));
    }

    @Test
    void detalleAdminDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(adminRevendedorService.getRevendedor(eq(revendedorId))).thenReturn(AdminRevendedorResponse.builder()
                .id(revendedorId)
                .nombre("Tienda Demo")
                .dominio("mitienda.com")
                .activo(true)
                .storefront(AdminStorefrontResponse.builder()
                        .nombreComercial("Mi Tienda")
                        .publicada(true)
                        .build())
                .build());

        mockMvc.perform(get("/admin/revendedores/{id}", revendedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(revendedorId.toString()))
                .andExpect(jsonPath("$.storefront.nombreComercial").value("Mi Tienda"));
    }

    @Test
    void crearRevendedorDevuelve201() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(adminRevendedorService.crear(org.mockito.ArgumentMatchers.any())).thenReturn(AdminRevendedorResponse.builder()
                .id(revendedorId)
                .nombre("Tienda Demo")
                .dominio("mitienda.com")
                .activo(true)
                .build());

        String body = """
                {
                  "nombre": "Tienda Demo",
                  "dominio": "mitienda.com",
                  "activo": true
                }
                """;

        mockMvc.perform(post("/admin/revendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(revendedorId.toString()))
                .andExpect(jsonPath("$.dominio").value("mitienda.com"));
    }

    @Test
    void actualizarRevendedorDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(adminRevendedorService.actualizar(eq(revendedorId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminRevendedorResponse.builder()
                        .id(revendedorId)
                        .nombre("Tienda Editada")
                        .dominio("tienda-editada.com")
                        .activo(false)
                        .build());

        String body = """
                {
                  "nombre": "Tienda Editada",
                  "dominio": "tienda-editada.com",
                  "activo": false
                }
                """;

        mockMvc.perform(put("/admin/revendedores/{id}", revendedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tienda Editada"))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void upsertStorefrontDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(adminRevendedorService.upsertStorefront(eq(revendedorId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminRevendedorResponse.builder()
                        .id(revendedorId)
                        .nombre("Tienda Demo")
                        .dominio("mitienda.com")
                        .activo(true)
                        .storefront(AdminStorefrontResponse.builder()
                                .nombreComercial("Mi Tienda")
                                .publicada(true)
                                .build())
                        .build());

        String body = """
                {
                  "nombreComercial": "Mi Tienda",
                  "publicada": true
                }
                """;

        mockMvc.perform(put("/admin/revendedores/{id}/storefront", revendedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storefront.nombreComercial").value("Mi Tienda"))
                .andExpect(jsonPath("$.storefront.publicada").value(true));
    }

    @Test
    void upsertMercadoPagoDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(revendedorMercadoPagoService.upsert(eq(revendedorId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AdminRevendedorMercadoPagoResponse.builder()
                        .estado("VINCULADA")
                        .readyForCheckout(true)
                        .mercadoPagoUserId("collector-1")
                        .accessTokenConfigured(true)
                        .refreshTokenConfigured(true)
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

        mockMvc.perform(put("/admin/revendedores/{id}/mercadopago", revendedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VINCULADA"))
                .andExpect(jsonPath("$.readyForCheckout").value(true))
                .andExpect(jsonPath("$.mercadoPagoUserId").value("collector-1"));
    }

    @Test
    void generarOAuthLinkDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(mercadoPagoOAuthService.generarAuthorizationLink(eq(revendedorId)))
                .thenReturn(AdminRevendedorMercadoPagoOAuthLinkResponse.builder()
                        .authorizationUrl("https://auth.mercadopago.com.ar/authorization?client_id=123")
                        .redirectUri("http://localhost:8080/public/mercadopago/oauth/callback")
                        .expiresAt(java.time.LocalDateTime.of(2026, 4, 3, 12, 0))
                        .build());

        mockMvc.perform(get("/admin/revendedores/{id}/mercadopago/oauth-link", revendedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value("https://auth.mercadopago.com.ar/authorization?client_id=123"))
                .andExpect(jsonPath("$.redirectUri").value("http://localhost:8080/public/mercadopago/oauth/callback"));
    }

    @Test
    void uploadStorefrontAssetDevuelve201() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "logo.png", "image/png", new byte[] { 1, 2, 3 });
        when(storefrontAssetStorageService.store(org.mockito.ArgumentMatchers.any(), eq("logo"), eq("http://localhost")))
                .thenReturn(AdminStorefrontAssetResponse.builder()
                        .url("http://localhost/public/assets/storefront/logo-demo.png")
                        .filename("logo-demo.png")
                        .contentType("image/png")
                        .size(3)
                        .build());

        mockMvc.perform(multipart("/admin/revendedores/storefront-assets")
                        .file(archivo)
                        .param("tipo", "logo"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("http://localhost/public/assets/storefront/logo-demo.png"));
    }

    @Test
    void validacionBasicaDevuelve400() throws Exception {
        String body = """
                {
                  "nombre": "Tienda Demo",
                  "activo": true
                }
                """;

        mockMvc.perform(post("/admin/revendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void duplicadoDevuelve409() throws Exception {
        when(adminRevendedorService.crear(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new OperacionEnConflictoException("Ya existe un revendedor con dominio: mitienda.com"));

        String body = """
                {
                  "nombre": "Tienda Demo",
                  "dominio": "mitienda.com",
                  "activo": true
                }
                """;

        mockMvc.perform(post("/admin/revendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un revendedor con dominio: mitienda.com"));
    }
}
