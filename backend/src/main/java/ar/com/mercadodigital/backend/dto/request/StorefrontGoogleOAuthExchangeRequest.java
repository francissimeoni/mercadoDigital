/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

@lombok.Getter
@lombok.Setter
public class StorefrontGoogleOAuthExchangeRequest {
    @NotBlank
    private String ticket;
    @NotBlank
    private String storefrontHost;
}

