package ar.com.mercadodigital.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    // Retirado de MercadoDigital. No controla otros servicios del VPS.
    private boolean enabled = false;

    private String baseUrl = "https://api.mercadopago.com";
    private String authBaseUrl = "https://auth.mercadopago.com.ar";
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String adminRedirectUrl = "http://localhost:5173/admin/storefronts";
    private String accessToken;
    private String publicKey;
    private String returnUrl;
    private String webhookUrl;
    private String webhookSecret;
    private String currency = "ARS";
    private String payoutNotificationUrl;
    private String payoutHeaderSignature = "true";
    private String payoutHeaderEnforceSignature = "false";
    private String payoutHeaderTestToken = "false";
}
