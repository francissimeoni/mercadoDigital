/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.controller;

import ar.com.mercadodigital.backend.dto.request.StorefrontGoogleOAuthExchangeRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontGoogleOAuthConfigResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontGoogleOAuthExchangeResponse;
import ar.com.mercadodigital.backend.service.StorefrontGoogleOAuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/public/storefront/auth/google"})
public class StorefrontGoogleOAuthController {
    private static final Logger log = LoggerFactory.getLogger(StorefrontGoogleOAuthController.class);
    private final StorefrontGoogleOAuthService googleOAuthService;

    @GetMapping(value={"/configuracion"})
    public StorefrontGoogleOAuthConfigResponse configuracion() {
        return StorefrontGoogleOAuthConfigResponse.builder().habilitado(this.googleOAuthService.estaHabilitado()).build();
    }

    @GetMapping(value={"/iniciar"})
    public ResponseEntity<Void> iniciar(@RequestParam String storefrontHost, @RequestParam(required=false) String returnPath) {
        return this.redirect(this.googleOAuthService.crearAuthorizationUrl(storefrontHost, returnPath));
    }

    @GetMapping(value={"/callback"})
    public ResponseEntity<Void> callback(@RequestParam(required=false) String code, @RequestParam(required=false) String state, @RequestParam(required=false) String error) {
        if (StringUtils.hasText(error)) {
            return this.redirect(this.googleOAuthService.construirRetornoError(state));
        }
        try {
            return this.redirect(this.googleOAuthService.procesarCallback(code, state));
        }
        catch (Exception ex) {
            log.warn("No se pudo completar el callback de Google OAuth. codePresent={}, statePresent={}", StringUtils.hasText(code), StringUtils.hasText(state), ex);
            return this.redirect(this.googleOAuthService.construirRetornoError(state));
        }
    }

    @PostMapping(value={"/canjear"})
    public StorefrontGoogleOAuthExchangeResponse canjear(@Valid @RequestBody StorefrontGoogleOAuthExchangeRequest request) {
        return this.googleOAuthService.canjear(request);
    }

    private ResponseEntity<Void> redirect(String url) {
        return ((ResponseEntity.BodyBuilder)ResponseEntity.status(302).header("Location", url)).build();
    }

    public StorefrontGoogleOAuthController(StorefrontGoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }
}

