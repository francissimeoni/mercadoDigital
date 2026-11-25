package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.request.StorefrontLoginRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontRefreshRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;
import ar.com.mercadodigital.backend.service.StorefrontAuthService;

@WebMvcTest(StorefrontAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorefrontAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorefrontAuthService storefrontAuthService;

    @Test
    void refreshDevuelveNuevaSesion() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(storefrontAuthService.refresh(any(StorefrontRefreshRequest.class)))
                .thenReturn(StorefrontAuthResponse.builder()
                        .token("access-nuevo")
                        .refreshToken("refresh-nuevo")
                        .email("tienda@test.com")
                        .nombre("Infonel")
                        .revendedorId(revendedorId.toString())
                        .roles(List.of("REVENDEDOR"))
                        .build());

        mockMvc.perform(post("/public/storefront/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storefrontHost": "www.infonel.com.ar",
                                  "refreshToken": "refresh-anterior"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-nuevo"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-nuevo"));
    }

    @Test
    void loginDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        when(storefrontAuthService.login(any(StorefrontLoginRequest.class)))
                .thenReturn(StorefrontAuthResponse.builder()
                        .token("jwt")
                        .email("tienda@test.com")
                        .nombre("Infonel")
                        .issuedAt("2026-08-28T17:00:00")
                        .revendedorId(revendedorId.toString())
                        .clienteId(null)
                        .asesorId(null)
                        .roles(List.of("REVENDEDOR"))
                        .build());

        mockMvc.perform(post("/public/storefront/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storefrontHost": "www.infonel.com.ar",
                                  "email": "tienda@test.com",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.roles[0]").value("REVENDEDOR"))
                .andExpect(jsonPath("$.revendedorId").value(revendedorId.toString()))
                .andExpect(jsonPath("$.nombre").value("Infonel"));
    }
}
