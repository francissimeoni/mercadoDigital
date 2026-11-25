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

import ar.com.mercadodigital.backend.dto.response.CatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoDetalleResponse;
import ar.com.mercadodigital.backend.service.CatalogoService;

@WebMvcTest(CatalogoController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogoService catalogoService;

    @Test
    void getCatalogoDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        UUID productoCanonicoId = UUID.randomUUID();

        when(catalogoService.getCatalogo(eq(revendedorId))).thenReturn(List.of(
                CatalogoItemResponse.builder()
                        .productoCanonicoId(productoCanonicoId)
                        .nombrePublico("Producto demo")
                        .mejorCostoOperativoEstimado(new BigDecimal("1234"))
                        .demoraEstimadaDias(5)
                        .stockDisponible(8)
                        .cantidadOfertas(1)
                        .imagenPrincipal("https://cdn/producto.jpg")
                        .build()));

        mockMvc.perform(get("/catalogo").param("revendedorId", revendedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoCanonicoId").value(productoCanonicoId.toString()));
    }

    @Test
    void getProductoDetalleDevuelve200() throws Exception {
        UUID revendedorId = UUID.randomUUID();
        UUID productoCanonicoId = UUID.randomUUID();

        when(catalogoService.getProductoDetalle(eq(productoCanonicoId), eq(revendedorId)))
                .thenReturn(ProductoDetalleResponse.builder()
                        .productoCanonicoId(productoCanonicoId)
                        .nombrePublico("Producto demo")
                        .descripcion("Descripcion demo")
                        .mejorCostoOperativoEstimado(new BigDecimal("1234"))
                        .demoraEstimadaDias(5)
                        .stockDisponible(8)
                        .imagenPrincipal("https://cdn/producto.jpg")
                        .build());

        mockMvc.perform(get("/catalogo/{id}", productoCanonicoId)
                        .param("revendedorId", revendedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoCanonicoId").value(productoCanonicoId.toString()))
                .andExpect(jsonPath("$.descripcion").value("Descripcion demo"));
    }
}
