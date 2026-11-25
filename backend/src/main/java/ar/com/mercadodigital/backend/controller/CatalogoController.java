package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.response.CatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoDetalleResponse;
import ar.com.mercadodigital.backend.service.CatalogoService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping
    public ResponseEntity<List<CatalogoItemResponse>> getCatalogo(@RequestParam UUID revendedorId) {
        return ResponseEntity.ok(catalogoService.getCatalogo(revendedorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDetalleResponse> getProductoDetalle(
            @PathVariable UUID id,
            @RequestParam UUID revendedorId) {
        return ResponseEntity.ok(catalogoService.getProductoDetalle(id, revendedorId));
    }
}
