package ar.com.mercadodigital.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.AdminProveedorDespachoRequest;
import ar.com.mercadodigital.backend.dto.request.AdminRevendedorMercadoPagoRequest;
import ar.com.mercadodigital.backend.dto.request.AdminStorefrontRequest;
import ar.com.mercadodigital.backend.dto.response.AdminProveedorDespachoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoOAuthLinkResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontResponse;
import ar.com.mercadodigital.backend.service.AdminProveedorService;
import ar.com.mercadodigital.backend.service.AdminProveedorStorefrontService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.ProveedorMercadoPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/proveedores")
public class AdminProveedorController {

    private final ProveedorMercadoPagoService proveedorMercadoPagoService;
    private final MercadoPagoOAuthService mercadoPagoOAuthService;
    private final AdminProveedorService adminProveedorService;
    private final AdminProveedorStorefrontService adminProveedorStorefrontService;

    @GetMapping("/{id}/mercadopago")
    public ResponseEntity<AdminRevendedorMercadoPagoResponse> getMercadoPago(@PathVariable UUID id) {
        return ResponseEntity.ok(proveedorMercadoPagoService.getEstado(id));
    }

    @PutMapping("/{id}/mercadopago")
    public ResponseEntity<AdminRevendedorMercadoPagoResponse> upsertMercadoPago(
            @PathVariable UUID id,
            @RequestBody AdminRevendedorMercadoPagoRequest request) {
        return ResponseEntity.ok(proveedorMercadoPagoService.upsert(id, request));
    }

    @GetMapping("/{id}/mercadopago/oauth-link")
    public ResponseEntity<AdminRevendedorMercadoPagoOAuthLinkResponse> generarOAuthLink(@PathVariable UUID id) {
        return ResponseEntity.ok(mercadoPagoOAuthService.generarAuthorizationLinkProveedor(id));
    }

    @GetMapping("/{id}/despacho-config")
    public ResponseEntity<AdminProveedorDespachoResponse> getDespachoConfig(@PathVariable UUID id) {
        return ResponseEntity.ok(adminProveedorService.getDespacho(id));
    }

    @PutMapping("/{id}/despacho-config")
    public ResponseEntity<AdminProveedorDespachoResponse> actualizarDespachoConfig(
            @PathVariable UUID id,
            @Valid @RequestBody AdminProveedorDespachoRequest request) {
        return ResponseEntity.ok(adminProveedorService.actualizarDespacho(id, Boolean.TRUE.equals(request.getDespachaMercaderia())));
    }

    @GetMapping("/{id}/storefront")
    public ResponseEntity<AdminStorefrontResponse> getStorefront(@PathVariable UUID id) {
        return ResponseEntity.ok(adminProveedorStorefrontService.getStorefront(id));
    }

    @PutMapping("/{id}/storefront")
    public ResponseEntity<AdminStorefrontResponse> upsertStorefront(
            @PathVariable UUID id,
            @Valid @RequestBody AdminStorefrontRequest request) {
        return ResponseEntity.ok(adminProveedorStorefrontService.upsertStorefront(id, request));
    }
}
