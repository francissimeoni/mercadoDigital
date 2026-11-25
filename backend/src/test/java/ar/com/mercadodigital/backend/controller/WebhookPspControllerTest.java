package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.service.MercadoPagoClient;
import ar.com.mercadodigital.backend.service.MercadoPagoWebhookAdapter;
import ar.com.mercadodigital.backend.service.MercadoPagoWebhookSignatureVerifier;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import ar.com.mercadodigital.backend.service.WebhookPspService;

@WebMvcTest(WebhookPspController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebhookPspControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookPspService webhookPspService;
    @MockBean
    private MercadoPagoWebhookAdapter mercadoPagoWebhookAdapter;
    @MockBean
    private MercadoPagoClient mercadoPagoClient;
    @MockBean
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @MockBean
    private MercadoPagoWebhookSignatureVerifier signatureVerifier;

    @Test
    void noAceptaWebhookGenericoSinVerificacion() throws Exception {
        String body = """
                {
                  "idEventoPsp": "evt_1",
                  "tipoEvento": "PAGO_CONFIRMADO",
                  "referenciaPedido": "%s",
                  "referenciaPago": "pay_ref_1",
                  "monto": %s,
                  "estado": "CONFIRMADO"
                }
                """.formatted(UUID.randomUUID(), new BigDecimal("1000.00"));

        mockMvc.perform(post("/webhooks/psp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
        org.mockito.Mockito.verifyNoInteractions(webhookPspService);
    }

    @Test
    void recibirWebhookMercadoPagoOkDevuelve200() throws Exception {
        MercadoPagoWebhookAdapter.MercadoPagoWebhookNotification notification =
                new MercadoPagoWebhookAdapter.MercadoPagoWebhookNotification("12345", "98765", "payment", "payment.updated", "collector-1");

        ar.com.mercadodigital.backend.dto.request.WebhookPspRequest request = new ar.com.mercadodigital.backend.dto.request.WebhookPspRequest();
        request.setIdEventoPsp("mercadopago-12345");
        request.setTipoEvento("PAGO_CONFIRMADO");
        request.setReferenciaPedido(UUID.randomUUID().toString());
        request.setReferenciaPago("98765");
        request.setMonto(new BigDecimal("1000.00"));
        request.setEstado("CONFIRMADO");

        when(mercadoPagoWebhookAdapter.extraerNotificacion(any(), any(), any(), any(), any())).thenReturn(notification);
        when(mercadoPagoWebhookAdapter.esNotificacionDePago(notification)).thenReturn(true);
        when(revendedorMercadoPagoService.resolveForMercadoPagoUserId("collector-1"))
                .thenReturn(new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("seller-token", "collector-1", false, true));
        when(mercadoPagoClient.obtenerPago("98765", "seller-token")).thenReturn(objectMapper.readTree("""
                {
                  "id": 98765,
                  "status": "approved",
                  "collector_id": "collector-1",
                  "currency_id": "ARS",
                  "transaction_amount": 1000.00,
                  "external_reference": "%s"
                }
                """.formatted(UUID.randomUUID())));
        when(mercadoPagoWebhookAdapter.adaptarPago(any(), any())).thenReturn(request);

        mockMvc.perform(post("/webhooks/psp/mercadopago")
                        .param("data.id", "98765")
                        .header("x-signature", "firma-sintetica")
                        .header("x-request-id", "solicitud-sintetica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "type", "payment",
                                "data", java.util.Map.of("id", "98765"),
                                "action", "payment.updated"))))
                .andExpect(status().isOk());
        org.mockito.Mockito.verify(webhookPspService).procesarWebhookMercadoPago(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("collector-1"));
    }
}
