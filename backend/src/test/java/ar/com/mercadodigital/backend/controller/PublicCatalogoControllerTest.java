package ar.com.mercadodigital.backend.controller;

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

import ar.com.mercadodigital.backend.dto.response.PublicCatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.service.PublicCatalogoService;

@WebMvcTest(PublicCatalogoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicCatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicCatalogoService publicCatalogoService;

    @Test
    void getCatalogoPublicoDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        UUID productoCanonicoId = UUID.randomUUID();

        when(publicCatalogoService.getCatalogo(eq(revendedorId))).thenReturn(List.of(
                PublicCatalogoItemResponse.builder()
                        .productoCanonicoId(productoCanonicoId)
                        .nombrePublico("Producto publico")
                        .mejorPrecioFinalEstimado(new BigDecimal("1234"))
                        .demoraEstimadaDias(5)
                        .stockDisponible(8)
                        .cantidadOfertas(1)
                        .imagenPrincipal("https://cdn/producto.jpg")
                        .build()));

        mockMvc.perform(get("/public/catalogo").param("revendedorId", revendedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoCanonicoId").value(productoCanonicoId.toString()))
                .andExpect(jsonPath("$[0].nombrePublico").value("Producto publico"));
    }

    @Test
    void getDetallePublicoDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        UUID productoCanonicoId = UUID.randomUUID();

        when(publicCatalogoService.getProductoDetalle(eq(productoCanonicoId), eq(revendedorId)))
                .thenReturn(PublicProductoDetalleResponse.builder()
                        .productoCanonicoId(productoCanonicoId)
                        .nombrePublico("Producto publico")
                        .descripcion("Descripcion publica")
                        .mejorPrecioFinalEstimado(new BigDecimal("1234"))
                        .seguroTransporteEstimado(new BigDecimal("25"))
                        .totalConSeguroEstimado(new BigDecimal("1259"))
                        .demoraEstimadaDias(5)
                        .stockDisponible(8)
                        .imagenPrincipal("https://cdn/producto.jpg")
                        .build());

        mockMvc.perform(get("/public/catalogo/{id}", productoCanonicoId)
                        .param("revendedorId", revendedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoCanonicoId").value(productoCanonicoId.toString()))
                .andExpect(jsonPath("$.descripcion").value("Descripcion publica"))
                .andExpect(jsonPath("$.seguroTransporteEstimado").value(25))
                .andExpect(jsonPath("$.totalConSeguroEstimado").value(1259));
    }

    @Test
    void getDetallePublicoNoVisibleDevuelve404() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        UUID productoCanonicoId = UUID.randomUUID();

        when(publicCatalogoService.getProductoDetalle(eq(productoCanonicoId), eq(revendedorId)))
                .thenThrow(new ResourceNotFoundException("Producto sin oferta disponible para revendedor: " + productoCanonicoId));

        mockMvc.perform(get("/public/catalogo/{id}", productoCanonicoId)
                        .param("revendedorId", revendedorId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCatalogoPublicoSinRevendedorIdDevuelve400() throws Exception {
        mockMvc.perform(get("/public/catalogo"))
                .andExpect(status().isBadRequest());
    }
}
