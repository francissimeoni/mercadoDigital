/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.dto.response;

import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;

@lombok.Getter
@lombok.Builder
public class StorefrontGoogleOAuthExchangeResponse {
    private StorefrontAuthResponse session;
    private String returnPath;
}

