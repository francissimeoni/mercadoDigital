package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionCandidatoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionPendienteResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionResultadoResponse;
import ar.com.mercadodigital.backend.service.AdminCanonizacionService;

@WebMvcTest(AdminCanonizacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCanonizacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminCanonizacionService adminCanonizacionService;

    @Test
    void listarPendientesDevuelve200() throws Exception {
        UUID productoId = UUID.randomUUID();
        when(adminCanonizacionService.listarPendientes())
                .thenReturn(List.of(AdminCanonizacionPendienteResponse.builder()
                        .productoId(productoId)
                        .sku("SKU-1")
                        .nombre("Producto pendiente")
                        .build()));

        mockMvc.perform(get("/admin/canonizacion/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").value(productoId.toString()))
                .andExpect(jsonPath("$[0].sku").value("SKU-1"));
    }

    @Test
    void listarCandidatosDevuelve200() throws Exception {
        UUID canonicoId = UUID.randomUUID();
        when(adminCanonizacionService.listarCandidatos(eq("notebook"), eq(10)))
                .thenReturn(List.of(AdminCanonizacionCandidatoResponse.builder()
                        .productoCanonicoId(canonicoId)
                        .nombrePublico("Notebook 14")
                        .build()));

        mockMvc.perform(get("/admin/canonizacion/candidatos")
                        .param("q", "notebook")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoCanonicoId").value(canonicoId.toString()));
    }

    @Test
    void asociarExistenteDevuelve200() throws Exception {
        UUID productoId = UUID.randomUUID();
        UUID canonicoId = UUID.randomUUID();
        when(adminCanonizacionService.asociarAExistente(eq(productoId), eq(canonicoId)))
                .thenReturn(AdminCanonizacionResultadoResponse.builder()
                        .productoId(productoId)
                        .productoCanonicoId(canonicoId)
                        .accion("ASOCIADO_EXISTENTE")
                        .build());

        mockMvc.perform(put("/admin/canonizacion/productos/{productoId}/asociar/{productoCanonicoId}", productoId, canonicoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accion").value("ASOCIADO_EXISTENTE"));
    }

    @Test
    void crearCanonicoDevuelve200() throws Exception {
        UUID productoId = UUID.randomUUID();
        UUID canonicoId = UUID.randomUUID();
        when(adminCanonizacionService.crearCanonicoYAsociar(eq(productoId), any()))
                .thenReturn(AdminCanonizacionResultadoResponse.builder()
                        .productoId(productoId)
                        .productoCanonicoId(canonicoId)
                        .accion("CREADO_Y_ASOCIADO")
                        .build());

        mockMvc.perform(post("/admin/canonizacion/productos/{productoId}/crear-canonico", productoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "marca": "Marca Demo",
                                  "modelo": "Modelo Demo",
                                  "nombrePublico": "Producto Demo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accion").value("CREADO_Y_ASOCIADO"));
    }
}
