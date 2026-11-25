package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import ar.com.mercadodigital.backend.dto.request.WebhookPspRequest;
import ar.com.mercadodigital.backend.exception.BadRequestException;

@Service
public class MercadoPagoWebhookAdapter {

    public MercadoPagoWebhookNotification extraerNotificacion(
            JsonNode payload,
            String topic,
            String type,
            String id,
            String dataId) {
        String notificationType = firstNonBlank(
                extraerTextoOpcional(payload, "type", "topic"),
                type,
                topic);
        String paymentId = firstNonBlank(
                extraerTextoOpcional(payload, "data.id"),
                parsePaymentIdDesdeResource(extraerTextoOpcional(payload, "resource")),
                dataId,
                id);
        String mercadoPagoUserId = firstNonBlank(extraerTextoOpcional(payload, "user_id"), extraerTextoOpcional(payload, "userId"));
        String notificationId = firstNonBlank(extraerTextoOpcional(payload, "id"), paymentId);
        String action = extraerTextoOpcional(payload, "action");

        return new MercadoPagoWebhookNotification(notificationId, paymentId, notificationType, action, mercadoPagoUserId);
    }

    public boolean esNotificacionDePago(MercadoPagoWebhookNotification notification) {
        if (notification == null) {
            return false;
        }
        return "payment".equalsIgnoreCase(trimToNull(notification.type()))
                && StringUtils.hasText(notification.paymentId());
    }

    public WebhookPspRequest adaptarPago(MercadoPagoWebhookNotification notification, JsonNode paymentPayload) {
        String paymentId = extraerTextoObligatorio(paymentPayload, "id");
        if (!paymentId.equals(notification.paymentId())) {
            throw new BadRequestException("La notificacion no coincide con el pago consultado.");
        }
        String externalReference = extraerTextoObligatorio(paymentPayload, "external_reference");
        BigDecimal monto = extraerDecimal(paymentPayload, "transaction_amount");
        String estadoProveedor = extraerTextoObligatorio(paymentPayload, "status");
        if (!"ARS".equalsIgnoreCase(extraerTextoObligatorio(paymentPayload, "currency_id"))) {
            throw new BadRequestException("La moneda del pago no coincide con el pedido.");
        }

        WebhookPspRequest request = new WebhookPspRequest();
        // El mismo pago puede generar varias notificaciones con estados diferentes.
        request.setIdEventoPsp("mp-" + paymentId + "-" + estadoProveedor.toLowerCase(Locale.ROOT)
                + "-" + firstNonBlank(extraerTextoOpcional(paymentPayload, "date_last_updated"), "sin-fecha"));
        request.setTipoEvento(esEstadoAprobado(estadoProveedor) ? "PAGO_CONFIRMADO" : "PAGO_ACTUALIZADO");
        request.setReferenciaPedido(externalReference);
        request.setReferenciaPago(paymentId);
        request.setMonto(monto);
        request.setEstado(esEstadoAprobado(estadoProveedor) ? "CONFIRMADO" : normalizarEstado(estadoProveedor));
        return request;
    }

    private boolean esEstadoAprobado(String estadoProveedor) {
        if (!StringUtils.hasText(estadoProveedor)) {
            return false;
        }
        return "approved".equalsIgnoreCase(estadoProveedor.trim());
    }

    private String normalizarEstado(String estadoProveedor) {
        if (!StringUtils.hasText(estadoProveedor)) {
            return "DESCONOCIDO";
        }
        return estadoProveedor.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String extraerTextoObligatorio(JsonNode payload, String... paths) {
        String value = extraerTextoOpcional(payload, paths);
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Webhook Mercado Pago invalido: faltan campos requeridos.");
        }
        return value;
    }

    private String extraerTextoOpcional(JsonNode payload, String... paths) {
        if (payload == null) {
            return null;
        }
        for (String path : paths) {
            JsonNode current = payload;
            for (String segment : path.split("\\.")) {
                current = current.path(segment);
            }
            if (!current.isMissingNode() && !current.isNull() && StringUtils.hasText(current.asText())) {
                return current.asText();
            }
        }
        return null;
    }

    private BigDecimal extraerDecimal(JsonNode payload, String... paths) {
        String value = extraerTextoOpcional(payload, paths);
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Webhook Mercado Pago invalido: monto ausente.");
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Webhook Mercado Pago invalido: monto no numerico.");
        }
    }

    private String parsePaymentIdDesdeResource(String resource) {
        if (!StringUtils.hasText(resource)) {
            return null;
        }
        int lastSlash = resource.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == resource.length() - 1) {
            return resource;
        }
        return resource.substring(lastSlash + 1);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record MercadoPagoWebhookNotification(String notificationId, String paymentId, String type, String action,
            String mercadoPagoUserId) {
    }
}
