package ar.com.mercadodigital.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/mercadopago/oauth")
public class PublicMercadoPagoOAuthController {

    private final MercadoPagoOAuthService mercadoPagoOAuthService;

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {
        String redirectUrl;
        if (StringUtils.hasText(error)) {
            redirectUrl = mercadoPagoOAuthService.buildAdminErrorRedirectUrlFromState(
                    state,
                    StringUtils.hasText(errorDescription) ? errorDescription : error.trim());
        } else {
            try {
                MercadoPagoOAuthService.OAuthCallbackResult result = mercadoPagoOAuthService.procesarCallback(code, state);
                redirectUrl = mercadoPagoOAuthService.buildAdminRedirectUrl(result);
            } catch (Exception ex) {
                redirectUrl = mercadoPagoOAuthService.buildAdminErrorRedirectUrlFromState(state, ex.getMessage());
            }
        }

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }
}
