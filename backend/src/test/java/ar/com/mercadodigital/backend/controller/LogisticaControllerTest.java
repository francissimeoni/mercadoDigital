package ar.com.mercadodigital.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import ar.com.mercadodigital.backend.dto.request.CrearTransportistaRequest;
import ar.com.mercadodigital.backend.dto.response.TransportistaResponse;
import ar.com.mercadodigital.backend.service.BandejasOperativasService;
import ar.com.mercadodigital.backend.service.TransportistaService;

@WebMvcTest(LogisticaController.class)
@AutoConfigureMockMvc(addFilters = false)
class LogisticaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BandejasOperativasService bandejasOperativasService;

    @MockBean
    private TransportistaService transportistaService;

    @Test
    void transportistasDevuelve200() throws Exception {
        UUID transportistaId = UUID.fromString("99999999-9999-9999-9999-999999999901");
        when(transportistaService.getActivos()).thenReturn(List.of(
                TransportistaResponse.builder()
                        .id(transportistaId)
                        .nombre("Andreani")
                        .build()));

        mockMvc.perform(get("/logistica/transportistas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(transportistaId.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Andreani"));
    }

    @Test
    void crearTransportistaDevuelve200() throws Exception {
        UUID transportistaId = UUID.randomUUID();
        when(transportistaService.crear(org.mockito.ArgumentMatchers.any(CrearTransportistaRequest.class)))
                .thenReturn(TransportistaResponse.builder()
                        .id(transportistaId)
                        .nombre("Credifin Express")
                        .build());

        mockMvc.perform(post("/logistica/transportistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Credifin Express"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transportistaId.toString()))
                .andExpect(jsonPath("$.nombre").value("Credifin Express"));
    }
}
