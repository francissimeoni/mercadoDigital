package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.ProductoMediaRequest;
import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.service.ProductoMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
public class ProductoMediaController {

    private final ProductoMediaService productoMediaService;

    @PostMapping("/productos/{id}/media")
    public ResponseEntity<ProductoMediaResponse> addMedia(
            @PathVariable UUID id,
            @Valid @RequestBody ProductoMediaRequest request) {
        boolean esPrincipal = request.getEsPrincipal() != null && request.getEsPrincipal();
        ProductoMediaResponse response = productoMediaService.addMedia(
                id,
                request.getTipoMedia(),
                request.getUrl(),
                request.getOrden(),
                esPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/productos/{id}/media")
    public ResponseEntity<List<ProductoMediaResponse>> getMedia(@PathVariable UUID id) {
        return ResponseEntity.ok(productoMediaService.getMedia(id));
    }

    @DeleteMapping("/media/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable UUID id) {
        productoMediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }
}
