package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthStateService.OAuthSubjectType;

@WebMvcTest(PublicMercadoPagoOAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicMercadoPagoOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MercadoPagoOAuthService mercadoPagoOAuthService;

    @Test
    void callbackExitosoRedirigeAlAdmin() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        MercadoPagoOAuthService.OAuthCallbackResult result =
                new MercadoPagoOAuthService.OAuthCallbackResult(OAuthSubjectType.REVENDEDOR, revendedorId, true, "ok");

        when(mercadoPagoOAuthService.procesarCallback(eq("code-demo"), eq("state-demo"))).thenReturn(result);
        when(mercadoPagoOAuthService.buildAdminRedirectUrl(result))
                .thenReturn("http://localhost:5173/admin/storefronts?mp_oauth_status=success&mp_subject_type=REVENDEDOR");

        mockMvc.perform(get("/public/mercadopago/oauth/callback")
                        .param("code", "code-demo")
                        .param("state", "state-demo"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173/admin/storefronts?mp_oauth_status=success&mp_subject_type=REVENDEDOR"));
    }

    @Test
    void callbackConErrorProveedorRedirigeConError() throws Exception {
        when(mercadoPagoOAuthService.buildAdminErrorRedirectUrlFromState("state-demo", "access_denied"))
                .thenReturn("http://localhost:5173/admin/storefronts?mp_oauth_status=error");

        mockMvc.perform(get("/public/mercadopago/oauth/callback")
                        .param("state", "state-demo")
                        .param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173/admin/storefronts?mp_oauth_status=error"));
    }
}
