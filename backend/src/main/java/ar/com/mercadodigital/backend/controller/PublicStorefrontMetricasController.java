package ar.com.mercadodigital.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.PublicStorefrontEventoRequest;
import ar.com.mercadodigital.backend.service.StorefrontMetricasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/storefront/metricas")
public class PublicStorefrontMetricasController {
    private final StorefrontMetricasService metricasService;

    @PostMapping("/eventos")
    public ResponseEntity<Void> registrar(@Valid @RequestBody PublicStorefrontEventoRequest request) {
        metricasService.registrar(request);
        return ResponseEntity.noContent().build();
    }
}
