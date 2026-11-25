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

import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.service.OfertaProductoCanonicoService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/productos-canonicos")
public class ProductoCanonicoController {

    private final OfertaProductoCanonicoService ofertaProductoCanonicoService;

    @GetMapping("/{id}/ofertas")
    public ResponseEntity<List<OfertaProductoResponse>> getOfertas(
            @PathVariable UUID id,
            @RequestParam UUID revendedorId) {
        return ResponseEntity.ok(ofertaProductoCanonicoService.getOfertas(id, revendedorId));
    }
}
