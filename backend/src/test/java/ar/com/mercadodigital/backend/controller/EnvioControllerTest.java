package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.LiquidacionProveedorResponse;
import ar.com.mercadodigital.backend.dto.response.EnvioResponse;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.service.EnvioService;
import ar.com.mercadodigital.backend.service.LiquidacionProveedorService;
import ar.com.mercadodigital.backend.service.RotuloEnvioService;

@WebMvcTest(EnvioController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvioService envioService;
    @MockBean
    private RotuloEnvioService rotuloEnvioService;
    @MockBean
    private LiquidacionProveedorService liquidacionProveedorService;

    @Test
    void getEnvioDevuelve200() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(envioService.getByPedidoId(pedidoId)).thenReturn(EnvioResponse.builder()
                .pedidoId(pedidoId)
                .estadoEnvio("PENDIENTE_PREPARACION")
                .tracking(null)
                .transportistaNombre(null)
                .costoEnvio(new BigDecimal("100.00"))
                .build());

        mockMvc.perform(get("/envios/{pedidoId}", pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(pedidoId.toString()))
                .andExpect(jsonPath("$.estadoEnvio").value("PENDIENTE_PREPARACION"));
    }

    @Test
    void despacharSinTrackingDevuelve400() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        String body = """
                {
                  "transportistaId": "%s",
                  "tracking": ""
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/envios/{pedidoId}/despachar", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void despacharConTransportistaIdInvalidoDevuelve400() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        String body = """
                {
                  "transportistaId": "CREDIFIN EXPRESS",
                  "tracking": "TRK-001"
                }
                """;

        mockMvc.perform(post("/envios/{pedidoId}/despachar", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body invalido"));
    }

    @Test
    void listoParaRetiroDevuelve200() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(envioService.marcarListoParaRetiro(eq(pedidoId))).thenReturn(EnvioResponse.builder()
                .pedidoId(pedidoId)
                .estadoEnvio("LISTO_PARA_RETIRO")
                .costoEnvio(BigDecimal.ZERO)
                .build());

        mockMvc.perform(post("/envios/{pedidoId}/listo-para-retiro", pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoEnvio").value("LISTO_PARA_RETIRO"));
    }

    @Test
    void liberarProveedorManualDevuelve200() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(liquidacionProveedorService.liberarManual(eq(pedidoId), eq("excepcion validada")))
                .thenReturn(LiquidacionProveedorResponse.builder()
                        .pedidoId(pedidoId)
                        .estadoLiquidacionProveedor("LIBERADO_MANUAL")
                        .motivoLiberacionManual("excepcion validada")
                        .totalProveedor(new BigDecimal("1000.00"))
                        .build());

        mockMvc.perform(post("/envios/{pedidoId}/liberar-proveedor", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivo": "excepcion validada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoLiquidacionProveedor").value("LIBERADO_MANUAL"))
                .andExpect(jsonPath("$.motivoLiberacionManual").value("excepcion validada"));
    }

    @Test
    void rotuloDevuelvePdf() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(rotuloEnvioService.generarRotuloPdf(pedidoId)).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get("/envios/{pedidoId}/rotulo", pedidoId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void rotuloPedidoInexistenteDevuelve404() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(rotuloEnvioService.generarRotuloPdf(pedidoId))
                .thenThrow(new ResourceNotFoundException("Envio no encontrado para pedido: " + pedidoId));

        mockMvc.perform(get("/envios/{pedidoId}/rotulo", pedidoId))
                .andExpect(status().isNotFound());
    }
}
