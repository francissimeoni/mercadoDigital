package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ar.com.mercadodigital.backend.dto.response.*;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.service.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/catalogo/tienda")
public class PublicCatalogoTiendaController {
    private final StorefrontService storefronts;
    private final PublicCatalogoService catalogo;
    private final CatalogoPublicoService independiente;
    private final CatalogoModoService modo;

    @GetMapping
    public ResponseEntity<List<PublicCatalogoItemResponse>> listado(@RequestParam String host) {
        var sf = storefronts.resolverPorHost(host);
        return sinCache(sf.getProveedorId() != null ? catalogo.getCatalogoProveedor(sf.getProveedorId())
                : catalogo.getCatalogo(sf.getRevendedorId()));
    }

    @GetMapping("/productos/{productoId}")
    public ResponseEntity<PublicProductoDetalleResponse> detalle(@RequestParam String host, @PathVariable UUID productoId) {
        var sf = storefronts.resolverPorHost(host);
        return sinCache(sf.getProveedorId() != null ? catalogo.getProductoDetalleProveedor(productoId, sf.getProveedorId())
                : catalogo.getProductoDetalle(productoId, sf.getRevendedorId()));
    }

    @GetMapping("/productos/{productoId}/contacto")
    public ResponseEntity<PublicCatalogoContactoResponse> contacto(@RequestParam String host, @PathVariable UUID productoId) {
        var sf = storefronts.resolverPorHost(host);
        if (sf.getProveedorId() != null || !modo.activo(sf.getRevendedorId()))
            throw new ResourceNotFoundException("Contacto de catálogo no disponible para esta tienda.");
        return sinCache(independiente.contacto(sf.getRevendedorId(), productoId, sf.getDominio()));
    }

    private <T> ResponseEntity<T> sinCache(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
