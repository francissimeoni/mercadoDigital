package ar.com.mercadodigital.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import ar.com.mercadodigital.backend.service.MercadoPagoClient;
import ar.com.mercadodigital.backend.service.MercadoPagoWebhookAdapter;
import ar.com.mercadodigital.backend.service.MercadoPagoWebhookSignatureVerifier;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import ar.com.mercadodigital.backend.service.WebhookPspService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks/psp")
public class WebhookPspController {

    private final WebhookPspService webhookPspService;
    private final MercadoPagoWebhookAdapter mercadoPagoWebhookAdapter;
    private final MercadoPagoClient mercadoPagoClient;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final MercadoPagoWebhookSignatureVerifier signatureVerifier;

    @PostMapping
    public ResponseEntity<Void> rechazarWebhookNoVerificado() {
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> recibirWebhookMercadoPago(
            @RequestBody(required = false) JsonNode payload,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId) {
        signatureVerifier.verify(signature, requestId, dataId);
        MercadoPagoWebhookAdapter.MercadoPagoWebhookNotification notification =
                mercadoPagoWebhookAdapter.extraerNotificacion(payload, topic, type, id, dataId);
        if (!dataId.equals(notification.paymentId())) {
            return ResponseEntity.badRequest().build();
        }
        if (!mercadoPagoWebhookAdapter.esNotificacionDePago(notification)) {
            return ResponseEntity.ok().build();
        }
        RevendedorMercadoPagoService.RevendedorMercadoPagoResolution resolution =
                revendedorMercadoPagoService.resolveForMercadoPagoUserId(notification.mercadoPagoUserId());
        JsonNode paymentPayload = mercadoPagoClient.obtenerPago(notification.paymentId(), resolution.accessToken());
        webhookPspService.procesarWebhookMercadoPago(
                mercadoPagoWebhookAdapter.adaptarPago(notification, paymentPayload),
                resolution, paymentPayload.path("collector_id").asText(null));
        return ResponseEntity.ok().build();
    }
}
