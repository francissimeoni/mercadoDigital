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

import ar.com.mercadodigital.backend.dto.response.PublicCatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.service.PublicCatalogoService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/catalogo")
public class PublicCatalogoController {

    private final PublicCatalogoService publicCatalogoService;

    @GetMapping
    public ResponseEntity<List<PublicCatalogoItemResponse>> getCatalogo(@RequestParam UUID revendedorId) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(publicCatalogoService.getCatalogo(revendedorId));
    }

    @GetMapping("/proveedor")
    public ResponseEntity<List<PublicCatalogoItemResponse>> getCatalogoProveedor(@RequestParam UUID proveedorId) {
        return ResponseEntity.ok(publicCatalogoService.getCatalogoProveedor(proveedorId));
    }

    @GetMapping("/{productoCanonicoId}")
    public ResponseEntity<PublicProductoDetalleResponse> getProductoDetalle(
            @PathVariable UUID productoCanonicoId,
            @RequestParam UUID revendedorId) {
        return ResponseEntity.ok(publicCatalogoService.getProductoDetalle(productoCanonicoId, revendedorId));
    }

    @GetMapping("/proveedor/{productoId}")
    public ResponseEntity<PublicProductoDetalleResponse> getProductoDetalleProveedor(
            @PathVariable UUID productoId,
            @RequestParam UUID proveedorId) {
        return ResponseEntity.ok(publicCatalogoService.getProductoDetalleProveedor(productoId, proveedorId));
    }
}
