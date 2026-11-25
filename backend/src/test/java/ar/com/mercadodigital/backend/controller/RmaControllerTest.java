package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.RmaResponse;
import ar.com.mercadodigital.backend.service.RmaService;
import ar.com.mercadodigital.backend.service.RotuloRmaService;

@WebMvcTest(RmaController.class)
@AutoConfigureMockMvc(addFilters = false)
class RmaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RmaService rmaService;
    @MockBean
    private RotuloRmaService rotuloRmaService;

    @Test
    void listarRmasDevuelve200() throws Exception {
        UUID rmaId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com",
                null,
                List.of(new SimpleGrantedAuthority("ADMIN_MD")));

        when(rmaService.getVisibles(eq("admin@test.com"), eq(List.of("ADMIN_MD")), eq(null)))
                .thenReturn(List.of(RmaResponse.builder()
                        .id(rmaId)
                        .estadoRma("SOLICITADO")
                        .build()));

        mockMvc.perform(get("/rma").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rmaId.toString()));
    }

    @Test
    void detalleRmaDevuelve200() throws Exception {
        UUID rmaId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com",
                null,
                List.of(new SimpleGrantedAuthority("ADMIN_MD")));

        when(rmaService.getDetalleVisible(eq(rmaId), eq("admin@test.com"), eq(List.of("ADMIN_MD"))))
                .thenReturn(RmaResponse.builder()
                        .id(rmaId)
                        .estadoRma("SOLICITADO")
                        .motivo("Producto defectuoso")
                        .build());

        mockMvc.perform(get("/rma/{id}", rmaId).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rmaId.toString()))
                .andExpect(jsonPath("$.motivo").value("Producto defectuoso"));
    }

    @Test
    void solicitarSinMotivoDevuelve400() throws Exception {
        String body = """
                {
                  "pedidoItemId": "%s",
                  "motivo": "",
                  "descripcion": "detalle"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/rma/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolverSinResolucionDevuelve400() throws Exception {
        String body = """
                {
                  "resolucionFinal": "",
                  "observacionesInspeccion": "obs",
                  "responsableProbable": "PROVEEDOR"
                }
                """;

        mockMvc.perform(post("/rma/{id}/resolver", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
