package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

import ar.com.mercadodigital.backend.dto.response.PublicPedidoDirectoResponse;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.service.PublicPedidoDirectoService;
import ar.com.mercadodigital.backend.service.TransferenciaService;

@WebMvcTest(PublicPedidoDirectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPedidoDirectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPedidoDirectoService publicPedidoDirectoService;
    @MockBean
    private TransferenciaService transferenciaService;

    @Test
    void compraDirectaExitosaDevuelve201() throws Exception {
        UUID pedidoId = UUID.randomUUID();

        when(publicPedidoDirectoService.crearPedidoDirecto(any()))
                .thenReturn(PublicPedidoDirectoResponse.builder()
                        .pedidoId(pedidoId)
                        .estadoPedido("CREADO")
                        .estadoPago("PENDIENTE")
                        .metodoPago("PSP")
                        .totalProducto(new BigDecimal("100.00"))
                        .totalEnvio(BigDecimal.ZERO)
                        .totalSeguro(new BigDecimal("20.00"))
                        .totalFinal(new BigDecimal("120.00"))
                        .demoraEstimadaDias(5)
                        .trackingToken("tracking-publico-1")
                        .build());

        mockMvc.perform(post("/public/pedido/directo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBase()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").value(pedidoId.toString()))
                .andExpect(jsonPath("$.estadoPedido").value("CREADO"))
                .andExpect(jsonPath("$.metodoPago").value("PSP"));
    }

    @Test
    void compraDirectaProductoNoVisibleDevuelve404() throws Exception {
        when(publicPedidoDirectoService.crearPedidoDirecto(any()))
                .thenThrow(new ResourceNotFoundException("Producto sin oferta disponible para revendedor"));

        mockMvc.perform(post("/public/pedido/directo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBase()))
                .andExpect(status().isNotFound());
    }

    @Test
    void compraDirectaRequestInvalidoDevuelve400() throws Exception {
        String body = """
                {
                  "revendedorId": "%s",
                  "nombreApellido": "",
                  "email": "invalido",
                  "telefonoWhatsapp": "",
                  "provincia": "",
                  "localidad": "",
                  "codigoPostal": "",
                  "calle": "",
                  "numero": "",
                  "productoCanonicoId": "%s",
                  "cantidad": 0,
                  "seguroTransporte": false,
                  "aceptacionTerminos": false
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/public/pedido/directo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private String bodyBase() {
        return """
                {
                  "revendedorId": "%s",
                  "nombreApellido": "Juan Perez",
                  "email": "cliente@test.com",
                  "telefonoWhatsapp": "1122334455",
                  "provincia": "Buenos Aires",
                  "localidad": "La Plata",
                  "codigoPostal": "1900",
                  "calle": "Calle 50",
                  "numero": "123",
                  "pisoDepto": "2B",
                  "observaciones": "Timbre azul",
                  "productoCanonicoId": "%s",
                  "cantidad": 1,
                  "seguroTransporte": true,
                  "aceptacionTerminos": true
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
