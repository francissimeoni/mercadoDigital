package ar.com.mercadodigital.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.StorefrontClienteRegistroRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontLoginRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontRefreshRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;
import ar.com.mercadodigital.backend.service.StorefrontAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/storefront/auth")
public class StorefrontAuthController {

    private final StorefrontAuthService storefrontAuthService;

    @PostMapping("/login")
    public ResponseEntity<StorefrontAuthResponse> login(@Valid @RequestBody StorefrontLoginRequest request) {
        return ResponseEntity.ok(storefrontAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<StorefrontAuthResponse> refresh(@Valid @RequestBody StorefrontRefreshRequest request) {
        return ResponseEntity.ok(storefrontAuthService.refresh(request));
    }

    @PostMapping("/clientes/registro")
    public ResponseEntity<StorefrontAuthResponse> registrarCliente(
            @Valid @RequestBody StorefrontClienteRegistroRequest request) {
        return ResponseEntity.ok(storefrontAuthService.registrarCliente(request));
    }
}
