package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.SinStockException;
import ar.com.mercadodigital.backend.service.PedidoService;

@WebMvcTest(PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PedidoControllerCanonicoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService pedidoService;

    @Test
    void pedidoCanonicoSinStockDevuelve409() throws Exception {
        when(pedidoService.crearPedidoCanonico(any()))
                .thenThrow(new SinStockException("Sin stock para producto canonico"));

        String body = """
                {
                  "clienteId":"%s",
                  "revendedorId":"%s",
                  "items":[{"productoCanonicoId":"%s","cantidad":1,"tieneSeguro":false}]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/pedido/canonico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sin stock para producto canonico"));
    }

    @Test
    void cancelarPedidoEnEstadoInvalidoDevuelve409() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(pedidoService.cancelarPedido(pedidoId))
                .thenThrow(new OperacionEnConflictoException("No se puede cancelar el pedido en estado: PAGADO"));

        mockMvc.perform(post("/pedido/{id}/cancelar", pedidoId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede cancelar el pedido en estado: PAGADO"));
    }
}
