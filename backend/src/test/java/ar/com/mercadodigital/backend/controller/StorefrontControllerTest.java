package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.service.StorefrontService;

@WebMvcTest(StorefrontController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorefrontControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorefrontService storefrontService;

    @Test
    void resolveDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();

        when(storefrontService.resolverPorHost(eq("mitienda.com")))
                .thenReturn(StorefrontResponse.builder()
                        .revendedorId(revendedorId)
                        .dominio("mitienda.com")
                        .nombreComercial("Mi Tienda")
                        .publicada(true)
                        .build());

        mockMvc.perform(get("/public/storefront/resolve").param("host", "mitienda.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revendedorId").value(revendedorId.toString()))
                .andExpect(jsonPath("$.dominio").value("mitienda.com"))
                .andExpect(jsonPath("$.nombreComercial").value("Mi Tienda"))
                .andExpect(jsonPath("$.publicada").value(true));
    }

    @Test
    void resolveDominioNoDisponibleDevuelve404() throws Exception {
        when(storefrontService.resolverPorHost(eq("inexistente.com")))
                .thenThrow(new ResourceNotFoundException("Storefront no disponible para host: inexistente.com"));

        mockMvc.perform(get("/public/storefront/resolve").param("host", "inexistente.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Storefront no disponible para host: inexistente.com"));
    }

    @Test
    void resolveSinHostDevuelve400() throws Exception {
        mockMvc.perform(get("/public/storefront/resolve").param("host", " "))
                .andExpect(status().isBadRequest());
    }
}
