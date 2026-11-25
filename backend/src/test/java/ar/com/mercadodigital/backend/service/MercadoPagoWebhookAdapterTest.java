package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.dto.request.WebhookPspRequest;
import ar.com.mercadodigital.backend.exception.BadRequestException;

class MercadoPagoWebhookAdapterTest {

    private final MercadoPagoWebhookAdapter adapter = new MercadoPagoWebhookAdapter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void adaptaNotificacionDePagoAContratoInterno() throws Exception {
        MercadoPagoWebhookAdapter.MercadoPagoWebhookNotification notification = adapter.extraerNotificacion(
                objectMapper.readTree("""
                        {
                          "id": 12345,
                          "type": "payment",
                          "action": "payment.updated",
                          "data": {
                            "id": "998877"
                          }
                        }
                        """),
                null,
                null,
                null,
                null);

        assertTrue(adapter.esNotificacionDePago(notification));

        WebhookPspRequest request = adapter.adaptarPago(notification, objectMapper.readTree("""
                {
                  "id": 998877,
                  "status": "approved",
                  "currency_id": "ARS",
                  "transaction_amount": 1000.00,
                  "external_reference": "11111111-1111-1111-1111-111111111111"
                }
                """));

        assertEquals("mp-998877-approved-sin-fecha", request.getIdEventoPsp());
        assertEquals("PAGO_CONFIRMADO", request.getTipoEvento());
        assertEquals("11111111-1111-1111-1111-111111111111", request.getReferenciaPedido());
        assertEquals("998877", request.getReferenciaPago());
        assertEquals("CONFIRMADO", request.getEstado());
    }

    @Test
    void rechazaPagoDistintoAlNotificadoYMonedaAjena() throws Exception {
        var notification = new MercadoPagoWebhookAdapter.MercadoPagoWebhookNotification(
                "notif", "998877", "payment", "payment.updated", "collector");
        assertThrows(BadRequestException.class, () -> adapter.adaptarPago(notification, objectMapper.readTree("""
                {"id": 123, "status": "approved", "currency_id": "ARS", "transaction_amount": 100,
                 "external_reference": "11111111-1111-1111-1111-111111111111"}
                """)));
        assertThrows(BadRequestException.class, () -> adapter.adaptarPago(notification, objectMapper.readTree("""
                {"id": 998877, "status": "approved", "currency_id": "USD", "transaction_amount": 100,
                 "external_reference": "11111111-1111-1111-1111-111111111111"}
                """)));
    }
}
