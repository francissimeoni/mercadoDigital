package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.RevendedorMarkupOverrideRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorPricingConfigRequest;
import ar.com.mercadodigital.backend.dto.response.RevendedorPricingConfigResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorPricingPreviewItemResponse;
import ar.com.mercadodigital.backend.service.RevendedorPricingV1Service;
import ar.com.mercadodigital.backend.service.RevendedorOwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/revendedores/{revendedorId}/pricing-v1")
public class RevendedorPricingController {

    private final RevendedorPricingV1Service revendedorPricingV1Service;
    private final RevendedorOwnershipService ownershipService;

    @GetMapping("/config")
    public ResponseEntity<RevendedorPricingConfigResponse> getConfiguracion(@PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(revendedorPricingV1Service.getConfiguracion(revendedorId));
    }

    @PutMapping("/config")
    public ResponseEntity<RevendedorPricingConfigResponse> actualizarConfiguracion(
            @PathVariable UUID revendedorId,
            @Valid @RequestBody RevendedorPricingConfigRequest request) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(revendedorPricingV1Service.actualizarConfiguracion(revendedorId, request));
    }

    @GetMapping("/preview")
    public ResponseEntity<List<RevendedorPricingPreviewItemResponse>> getPreview(@PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(revendedorPricingV1Service.getPreview(revendedorId));
    }

    @PutMapping("/productos/{productoId}")
    public ResponseEntity<RevendedorPricingPreviewItemResponse> upsertOverride(
            @PathVariable UUID revendedorId,
            @PathVariable UUID productoId,
            @Valid @RequestBody RevendedorMarkupOverrideRequest request) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(revendedorPricingV1Service.upsertOverride(revendedorId, productoId, request));
    }

    @DeleteMapping("/productos/{productoId}")
    public ResponseEntity<Void> eliminarOverride(
            @PathVariable UUID revendedorId,
            @PathVariable UUID productoId) {
        ownershipService.validarOwnership(revendedorId);
        revendedorPricingV1Service.desactivarOverride(revendedorId, productoId);
        return ResponseEntity.noContent().build();
    }
}
