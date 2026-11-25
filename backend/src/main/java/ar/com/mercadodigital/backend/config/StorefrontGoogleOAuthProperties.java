/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.storefront-google-oauth")
@lombok.Getter
@lombok.Setter
public class StorefrontGoogleOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUrl = "https://oauth2.googleapis.com/token";
    private String jwkSetUri = "https://www.googleapis.com/oauth2/v3/certs";
}

