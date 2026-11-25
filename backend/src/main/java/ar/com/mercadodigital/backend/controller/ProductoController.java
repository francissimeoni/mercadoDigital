package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.response.ProductoResponse;
import ar.com.mercadodigital.backend.service.ProductoService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getProductos() {
        return ResponseEntity.ok(productoService.getProductos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.getProductoById(id));
    }
}

