package ar.com.mercadodigital.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.service.OfertaProductoCanonicoService;

@WebMvcTest(ProductoCanonicoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductoCanonicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OfertaProductoCanonicoService ofertaProductoCanonicoService;

    @Test
    void responde200YListaNoVacia() throws Exception {
        UUID canonicoId = UUID.randomUUID();
        UUID revendedorId = UUID.randomUUID();

        when(ofertaProductoCanonicoService.getOfertas(eq(canonicoId), eq(revendedorId)))
                .thenReturn(List.of(OfertaProductoResponse.builder()
                        .productoId(UUID.randomUUID())
                        .productoCanonicoId(canonicoId)
                        .precioFinalEstimado(new BigDecimal("12345"))
                        .stockDisponible(10)
                        .demoraEstimadaDias(5)
                        .categoriaPaquete("CHICO")
                        .build()));

        mockMvc.perform(get("/productos-canonicos/{id}/ofertas", canonicoId)
                        .param("revendedorId", revendedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productoCanonicoId").value(canonicoId.toString()));
    }
}
