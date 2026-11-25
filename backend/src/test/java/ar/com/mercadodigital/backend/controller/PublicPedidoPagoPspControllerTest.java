package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoPagoPspResponse;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.service.PublicPedidoPagoPspService;
import ar.com.mercadodigital.backend.entity.Pedido;

@WebMvcTest(PublicPedidoPagoPspController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPedidoPagoPspControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPedidoPagoPspService publicPedidoPagoPspService;
    @MockBean
    private ar.com.mercadodigital.backend.service.PublicPedidoTrackingService publicPedidoTrackingService;

    @Test
    void prepararCheckoutOkDevuelve200() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        autorizar(pedidoId);
        when(publicPedidoPagoPspService.prepararCheckout(eq(pedidoId)))
                .thenReturn(PublicPedidoPagoPspResponse.builder()
                        .pedidoId(pedidoId)
                        .estadoPedido("CREADO")
                        .estadoPago("PENDIENTE")
                        .metodoPago("PSP")
                        .monto(new BigDecimal("120.00"))
                        .referenciaPedido(pedidoId.toString())
                        .referenciaPago("psp-publico-ref")
                        .checkoutUrl("https://www.mercadopago.com/checkout/v1/redirect?pref=1")
                        .build());

        mockMvc.perform(post("/public/pedido/{pedidoId}/pago", pedidoId)
                        .param("trackingToken", "seguimiento").param("storefrontHost", "tienda.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(pedidoId.toString()))
                .andExpect(jsonPath("$.referenciaPago").value("psp-publico-ref"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://www.mercadopago.com/checkout/v1/redirect?pref=1"));
    }

    @Test
    void prepararCheckoutPedidoInexistenteDevuelve404() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        autorizar(pedidoId);
        when(publicPedidoPagoPspService.prepararCheckout(eq(pedidoId)))
                .thenThrow(new ResourceNotFoundException("Pedido no encontrado: " + pedidoId));

        mockMvc.perform(post("/public/pedido/{pedidoId}/pago", pedidoId)
                        .param("trackingToken", "seguimiento").param("storefrontHost", "tienda.test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void prepararCheckoutEstadoInvalidoDevuelve409() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        autorizar(pedidoId);
        when(publicPedidoPagoPspService.prepararCheckout(eq(pedidoId)))
                .thenThrow(new OperacionEnConflictoException("Pedido en estado invalido para iniciar PSP: PAGADO"));

        mockMvc.perform(post("/public/pedido/{pedidoId}/pago", pedidoId)
                        .param("trackingToken", "seguimiento").param("storefrontHost", "tienda.test"))
                .andExpect(status().isConflict());
    }

    @Test
    void uuidSoloNoAutorizaPrepararPago() throws Exception {
        mockMvc.perform(post("/public/pedido/{pedidoId}/pago", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(publicPedidoPagoPspService);
    }

    @Test
    void referenciaDeOtroPedidoNoAutorizaPago() throws Exception {
        autorizar(UUID.randomUUID());
        mockMvc.perform(post("/public/pedido/{pedidoId}/pago", UUID.randomUUID())
                        .param("trackingToken", "seguimiento").param("storefrontHost", "tienda.test"))
                .andExpect(status().isNotFound());
        org.mockito.Mockito.verifyNoInteractions(publicPedidoPagoPspService);
    }

    private void autorizar(UUID pedidoId) {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        when(publicPedidoTrackingService.getPedidoPublicoByTrackingToken("seguimiento", "tienda.test"))
                .thenReturn(pedido);
    }
}
