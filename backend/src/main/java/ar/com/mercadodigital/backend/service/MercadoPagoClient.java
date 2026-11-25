package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.exception.IntegracionPspException;

@Service
public class MercadoPagoClient {

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private final RestClient restClient;
    private final MercadoPagoProperties mercadoPagoProperties;

    public MercadoPagoClient(RestClient.Builder restClientBuilder, MercadoPagoProperties mercadoPagoProperties) {
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(mercadoPagoProperties.getBaseUrl()))
                .build();
        this.mercadoPagoProperties = mercadoPagoProperties;
    }

    public boolean isEnabled() { return mercadoPagoProperties.isEnabled(); }

    public void exigirHabilitado() {
        if (!isEnabled()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.GONE, "Mercado Pago fue retirado de MercadoDigital.");
    }

    public void validarMetodoPago(String metodo) {
        if (metodo == null || !"TRANSFERENCIA".equalsIgnoreCase(metodo.trim())) exigirHabilitado();
    }

    public MercadoPagoCheckoutData crearPreferencia(Pedido pedido, Pago pago) {
        return crearPreferencia(pedido, pago, null);
    }

    public MercadoPagoCheckoutData crearPreferencia(Pedido pedido, Pago pago, String accessToken) {
        this.validarConfiguracion();
        LinkedHashMap<String, Object> requestBody = new LinkedHashMap<String, Object>();
        requestBody.put("items", this.construirItems(pedido));
        requestBody.put("external_reference", pedido.getId().toString());
        requestBody.put("notification_url", this.mercadoPagoProperties.getWebhookUrl());
        requestBody.put("back_urls", this.construirBackUrls(pedido));
        requestBody.put("auto_return", "approved");
        requestBody.put("metadata", Map.of("pedidoId", pedido.getId().toString(), "referenciaPagoInterna", this.resolveReferenciaInterna(pago)));
        requestBody.put("payer", this.construirPayer(pedido));
        try {
            JsonNode response = ((RestClient.RequestBodySpec)((RestClient.RequestBodySpec)this.restClient.post().uri("/checkout/preferences", new Object[0])).header(HEADER_AUTHORIZATION, "Bearer " + this.resolveAccessToken(accessToken))).contentType(MediaType.APPLICATION_JSON).body(requestBody).retrieve().body(JsonNode.class);
            String checkoutUrl = this.extraerTextoObligatorio(response, "init_point", "sandbox_init_point");
            return new MercadoPagoCheckoutData(checkoutUrl, this.extraerTextoOpcional(response, "id"), this.extraerTextoOpcional(response, "init_point", "sandbox_init_point"));
        }
        catch (RestClientResponseException ex) {
            throw new IntegracionPspException("Mercado Pago rechazo la preparacion del checkout: HTTP " + ex.getRawStatusCode() + " body=" + this.truncateBody(ex.getResponseBodyAsString()), ex);
        }
        catch (RestClientException ex) {
            throw new IntegracionPspException("No se pudo iniciar checkout en Mercado Pago.", ex);
        }
    }

    public JsonNode obtenerPago(String paymentId) {
        return obtenerPago(paymentId, null);
    }

    public JsonNode obtenerPago(String paymentId, String accessToken) {
        validarConfiguracion();
        if (!StringUtils.hasText(paymentId)) {
            throw new IntegracionPspException("Mercado Pago envio una notificacion sin payment id.");
        }

        try {
            return restClient.get()
                    .uri("/v1/payments/{paymentId}", paymentId)
                    .header(HEADER_AUTHORIZATION, "Bearer " + resolveAccessToken(accessToken))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw new IntegracionPspException("Mercado Pago rechazo la consulta del pago: HTTP " + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new IntegracionPspException("No se pudo consultar el pago en Mercado Pago.", ex);
        }
    }

    public MercadoPagoOAuthTokens intercambiarAuthorizationCode(String code, String state) {
        validarConfiguracionOauth();
        if (!StringUtils.hasText(code)) {
            throw new IntegracionPspException("Mercado Pago no devolvio authorization code.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", mercadoPagoProperties.getClientId().trim());
        body.add("client_secret", mercadoPagoProperties.getClientSecret().trim());
        body.add("grant_type", "authorization_code");
        body.add("code", code.trim());
        body.add("redirect_uri", mercadoPagoProperties.getRedirectUri().trim());
        if (StringUtils.hasText(state)) {
            body.add("state", state.trim());
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String accessToken = extraerTextoObligatorio(response, "access_token");
            String publicKey = extraerTextoOpcional(response, "public_key");
            String refreshToken = extraerTextoOpcional(response, "refresh_token");
            boolean liveMode = response.path("live_mode").asBoolean(false);
            String userId = extraerTextoOpcional(response, "user_id");
            String scope = extraerTextoOpcional(response, "scope");
            long expiresIn = response.path("expires_in").asLong(0);
            LocalDateTime expiresAt = expiresIn > 0 ? LocalDateTime.now().plusSeconds(expiresIn) : null;

            return new MercadoPagoOAuthTokens(accessToken, publicKey, refreshToken, liveMode, userId, scope, expiresAt);
        } catch (RestClientResponseException ex) {
            throw new IntegracionPspException("Mercado Pago rechazo el intercambio OAuth: HTTP " + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new IntegracionPspException("No se pudo completar el intercambio OAuth con Mercado Pago.", ex);
        }
    }

    public MercadoPagoPayoutData crearPayout(
            String accessToken,
            String externalReference,
            List<MercadoPagoPayoutTransaction> transactions) {
        exigirHabilitado();
        if (!StringUtils.hasText(accessToken)) {
            throw new IntegracionPspException("No hay access token de Mercado Pago para ejecutar payout.");
        }
        if (!StringUtils.hasText(externalReference)) {
            throw new IntegracionPspException("externalReference obligatorio para payout.");
        }
        if (transactions == null || transactions.isEmpty()) {
            throw new IntegracionPspException("No hay transacciones de payout para enviar.");
        }

        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("external_reference", externalReference.trim());
        requestBody.put("description", "Split payout pedido " + externalReference.trim());
        if (StringUtils.hasText(mercadoPagoProperties.getPayoutNotificationUrl())) {
            requestBody.put("config", Map.of("notification_url", mercadoPagoProperties.getPayoutNotificationUrl().trim()));
        }
        requestBody.put("transactions", transactions.stream().map(tx -> {
            Map<String, Object> transactionBody = new LinkedHashMap<>();
            transactionBody.put("external_reference", externalReference.trim() + "-" + tx.actor());
            transactionBody.put("type", "account");
            transactionBody.put("account", Map.of("email", tx.email()));
            transactionBody.put("amount", Map.of(
                    "currency", mercadoPagoProperties.getCurrency(),
                    "value", tx.amount()));
            transactionBody.put("description", tx.description());
            return transactionBody;
        }).toList());

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/payouts")
                    .header(HEADER_AUTHORIZATION, "Bearer " + accessToken.trim())
                    .header("X-Idempotency-Key", idempotencyKey)
                    .header("X-signature", safeHeaderValue(mercadoPagoProperties.getPayoutHeaderSignature(), "true"))
                    .header("X-Enforce-Signature", safeHeaderValue(mercadoPagoProperties.getPayoutHeaderEnforceSignature(), "false"))
                    .header("X-Test-Token", safeHeaderValue(mercadoPagoProperties.getPayoutHeaderTestToken(), "false"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            return new MercadoPagoPayoutData(
                    extraerTextoOpcional(response, "id"),
                    idempotencyKey,
                    extraerTextoOpcional(response, "status"),
                    response != null ? response.toString() : null);
        } catch (RestClientResponseException ex) {
            throw new IntegracionPspException("Mercado Pago rechazo payout: HTTP " + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new IntegracionPspException("No se pudo ejecutar payout en Mercado Pago.", ex);
        }
    }

    private void validarConfiguracion() {
        exigirHabilitado();
        if (!StringUtils.hasText(mercadoPagoProperties.getReturnUrl())
                || !StringUtils.hasText(mercadoPagoProperties.getWebhookUrl())) {
            throw new IntegracionPspException(
                    "Configuracion Mercado Pago incompleta. Revisar MERCADOPAGO_RETURN_URL y MERCADOPAGO_WEBHOOK_URL.");
        }
    }

    private void validarConfiguracionOauth() {
        exigirHabilitado();
        if (!StringUtils.hasText(mercadoPagoProperties.getClientId())
                || !StringUtils.hasText(mercadoPagoProperties.getClientSecret())
                || !StringUtils.hasText(mercadoPagoProperties.getRedirectUri())) {
            throw new IntegracionPspException(
                    "Configuracion OAuth de Mercado Pago incompleta. Revisar MERCADOPAGO_CLIENT_ID, MERCADOPAGO_CLIENT_SECRET y MERCADOPAGO_REDIRECT_URI.");
        }
    }

    private String resolveAccessToken(String explicitAccessToken) {
        if (StringUtils.hasText(explicitAccessToken)) {
            return explicitAccessToken.trim();
        }
        if (StringUtils.hasText(mercadoPagoProperties.getAccessToken())) {
            return mercadoPagoProperties.getAccessToken().trim();
        }
        throw new IntegracionPspException("No hay access token de Mercado Pago disponible para operar este checkout.");
    }

    private List<Map<String, Object>> construirItems(Pedido pedido) {
        return pedido.getItems().stream()
                .map(this::mapearItem)
                .toList();
    }

    private Map<String, Object> mapearItem(PedidoItem item) {
        Producto producto = item.getProducto();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", item.getRevendedorProducto() != null
                ? item.getRevendedorProducto().getNombre()
                : resolverNombreProducto(producto));
        payload.put("quantity", item.getCantidad());
        payload.put("unit_price", resolverPrecioUnitario(item));
        payload.put("currency_id", mercadoPagoProperties.getCurrency());
        return payload;
    }

    private BigDecimal resolverPrecioUnitario(PedidoItem item) {
        if (item.getPrecioItemFinal() == null) {
            return BigDecimal.ZERO;
        }
        return item.getPrecioItemFinal();
    }

    private Map<String, Object> construirPayer(Pedido pedido) {
        Map<String, Object> payer = new LinkedHashMap<>();
        if (pedido.getCliente() != null && StringUtils.hasText(pedido.getCliente().getEmail())) {
            payer.put("email", pedido.getCliente().getEmail());
        }
        if (pedido.getCliente() != null && StringUtils.hasText(pedido.getCliente().getNombre())) {
            payer.put("name", pedido.getCliente().getNombre());
        }
        return payer;
    }

    private String resolveReferenciaInterna(Pago pago) {
        if (StringUtils.hasText(pago.getPspReferencia())) {
            return pago.getPspReferencia();
        }
        if (pago.getId() != null) {
            return "psp-publico-" + pago.getId();
        }
        return UUID.randomUUID().toString();
    }

    private String resolverNombreProducto(Producto producto) {
        if (producto == null) {
            return "Pedido Mercado Digital";
        }
        if (producto.getProductoCanonico() != null && StringUtils.hasText(producto.getProductoCanonico().getNombrePublico())) {
            return producto.getProductoCanonico().getNombrePublico();
        }
        if (StringUtils.hasText(producto.getNombre())) {
            return producto.getNombre();
        }
        return "Pedido Mercado Digital";
    }

    private String extraerTextoObligatorio(JsonNode root, String... paths) {
        String value = extraerTextoOpcional(root, paths);
        if (!StringUtils.hasText(value)) {
            throw new IntegracionPspException("Respuesta invalida de Mercado Pago: falta checkoutUrl.");
        }
        return value;
    }

    private String extraerTextoOpcional(JsonNode root, String... paths) {
        if (root == null) {
            return null;
        }
        for (String path : paths) {
            JsonNode current = root;
            for (String segment : path.split("\\.")) {
                current = current.path(segment);
            }
            if (!current.isMissingNode() && !current.isNull() && StringUtils.hasText(current.asText())) {
                return current.asText();
            }
        }
        return null;
    }

    private String safeHeaderValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String truncateBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "<empty>";
        }
        String trimmed = body.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= 350) {
            return trimmed;
        }
        return trimmed.substring(0, 350) + "...";
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.mercadopago.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public record MercadoPagoCheckoutData(String checkoutUrl, String preferenceId, String providerUrl) {
    }

    public record MercadoPagoOAuthTokens(
            String accessToken,
            String publicKey,
            String refreshToken,
            boolean liveMode,
            String userId,
            String scope,
            LocalDateTime expiresAt) {
    }

    public record MercadoPagoPayoutTransaction(
            String actor,
            String email,
            BigDecimal amount,
            String description) {
    }

    public record MercadoPagoPayoutData(
            String payoutId,
            String idempotencyKey,
            String status,
            String payloadRespuestaJson) {
    }


    Map<String, Object> construirBackUrls(Pedido pedido) {
        return Map.of("success", this.construirReturnUrl(pedido, "success"), "pending", this.construirReturnUrl(pedido, "pending"), "failure", this.construirReturnUrl(pedido, "failure"));
    }

    private String construirReturnUrl(Pedido pedido, String status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(this.resolverReturnUrl(pedido)).queryParam("source", "mercadopago").queryParam("status", status);
        if (pedido != null && pedido.getId() != null) {
            builder.queryParam("pedido", pedido.getId());
        }
        if (pedido != null && StringUtils.hasText(pedido.getTrackingToken())) {
            builder.queryParam("tracking", pedido.getTrackingToken().trim());
        }
        return builder.build().encode().toUriString();
    }

    private String resolverReturnUrl(Pedido pedido) {
        String host;
        if (pedido != null && pedido.getRevendedor() != null && StringUtils.hasText(pedido.getRevendedor().getDominio()) && StringUtils.hasText(host = pedido.getRevendedor().getDominio().trim().replaceFirst("^https?://", "").replaceFirst("/.*$", ""))) {
            return "https://" + host + "/pago/retorno";
        }
        return this.mercadoPagoProperties.getReturnUrl();
    }
}
