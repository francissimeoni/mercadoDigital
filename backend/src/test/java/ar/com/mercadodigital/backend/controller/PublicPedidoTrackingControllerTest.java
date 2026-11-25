package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingEnvioResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingResponse;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.service.PublicPedidoTrackingService;

@WebMvcTest(PublicPedidoTrackingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPedidoTrackingControllerTest {

    private static final String STOREFRONT_HOST = "www.mercado-digital.online";

    @MockBean
    private ar.com.mercadodigital.backend.service.EnvioService envioService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPedidoTrackingService publicPedidoTrackingService;

    @Test
    void trackingOkDevuelve200() throws Exception {
        String trackingToken = "tracking-publico-1";
        when(publicPedidoTrackingService.getByTrackingToken(eq(trackingToken), eq(STOREFRONT_HOST)))
                .thenReturn(PublicPedidoTrackingResponse.builder()
                        .pedidoId(UUID.randomUUID())
                        .estadoPedido("CREADO")
                        .estadoPago("PENDIENTE")
                        .metodoPago("PSP")
                        .fechaCreacion(LocalDateTime.now())
                        .demoraEstimadaDias(5)
                        .direccionEnvio("Calle 123")
                        .totalProducto(new BigDecimal("100.00"))
                        .totalEnvio(BigDecimal.ZERO)
                        .totalSeguro(new BigDecimal("20.00"))
                        .totalFinal(new BigDecimal("120.00"))
                        .items(List.of(PublicPedidoTrackingItemResponse.builder()
                                .nombreProducto("Producto publico")
                                .cantidad(1)
                                .imagenPrincipal("https://cdn.local/producto.png")
                                .build()))
                        .envio(PublicPedidoTrackingEnvioResponse.builder()
                                .estadoEnvio("PENDIENTE_PREPARACION")
                                .trackingLogistica(null)
                                .build())
                        .build());

        mockMvc.perform(get("/public/pedido/{trackingToken}", trackingToken)
                        .param("storefrontHost", STOREFRONT_HOST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPedido").value("CREADO"))
                .andExpect(jsonPath("$.items[0].nombreProducto").value("Producto publico"));
    }

    @Test
    void trackingInexistenteDevuelve404() throws Exception {
        String trackingToken = "tracking-inexistente";
        when(publicPedidoTrackingService.getByTrackingToken(eq(trackingToken), eq(STOREFRONT_HOST)))
                .thenThrow(new ResourceNotFoundException("Pedido no encontrado para tracking token: " + trackingToken));

        mockMvc.perform(get("/public/pedido/{trackingToken}", trackingToken)
                        .param("storefrontHost", STOREFRONT_HOST))
                .andExpect(status().isNotFound());
    }
}
