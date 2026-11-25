package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.com.mercadodigital.backend.dto.request.AdminRevendedorRequest;
import ar.com.mercadodigital.backend.dto.request.AdminRevendedorMercadoPagoRequest;
import ar.com.mercadodigital.backend.dto.request.AdminRevendedorProductoImportRequest;
import ar.com.mercadodigital.backend.dto.request.AdminStorefrontRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoOAuthLinkResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorProductoImportResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorResponse;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontAssetResponse;
import ar.com.mercadodigital.backend.service.AdminRevendedorService;
import ar.com.mercadodigital.backend.service.AdminRevendedorProductoImportService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import ar.com.mercadodigital.backend.service.StorefrontAssetStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/revendedores")
public class AdminRevendedorController {

    private final AdminRevendedorService adminRevendedorService;
    private final AdminRevendedorProductoImportService adminRevendedorProductoImportService;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final MercadoPagoOAuthService mercadoPagoOAuthService;
    private final StorefrontAssetStorageService storefrontAssetStorageService;

    @GetMapping
    public ResponseEntity<List<AdminRevendedorResponse>> getRevendedores() {
        return ResponseEntity.ok(adminRevendedorService.getRevendedores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminRevendedorResponse> getRevendedor(@PathVariable UUID id) {
        return ResponseEntity.ok(adminRevendedorService.getRevendedor(id));
    }

    @PostMapping
    public ResponseEntity<AdminRevendedorResponse> crear(@Valid @RequestBody AdminRevendedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminRevendedorService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminRevendedorResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRevendedorRequest request) {
        return ResponseEntity.ok(adminRevendedorService.actualizar(id, request));
    }

    @PutMapping("/{id}/storefront")
    public ResponseEntity<AdminRevendedorResponse> upsertStorefront(
            @PathVariable UUID id,
            @Valid @RequestBody AdminStorefrontRequest request) {
        return ResponseEntity.ok(adminRevendedorService.upsertStorefront(id, request));
    }

    @PostMapping("/{id}/productos-propios/importar")
    public ResponseEntity<AdminRevendedorProductoImportResponse> importarProductosPropios(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRevendedorProductoImportRequest request) {
        return ResponseEntity.ok(adminRevendedorProductoImportService.importar(id, request));
    }

    @PutMapping("/{id}/mercadopago")
    public ResponseEntity<ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse> upsertMercadoPago(
            @PathVariable UUID id,
            @RequestBody AdminRevendedorMercadoPagoRequest request) {
        return ResponseEntity.ok(revendedorMercadoPagoService.upsert(id, request));
    }

    @GetMapping("/{id}/mercadopago/oauth-link")
    public ResponseEntity<AdminRevendedorMercadoPagoOAuthLinkResponse> generarOAuthLink(@PathVariable UUID id) {
        return ResponseEntity.ok(mercadoPagoOAuthService.generarAuthorizationLink(id));
    }

    @PostMapping("/storefront-assets")
    public ResponseEntity<AdminStorefrontAssetResponse> uploadStorefrontAsset(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "tipo", required = false) String tipo,
            HttpServletRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storefrontAssetStorageService.store(archivo, tipo, baseUrl));
    }
}
