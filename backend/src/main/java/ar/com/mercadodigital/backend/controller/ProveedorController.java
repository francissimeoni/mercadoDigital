package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ar.com.mercadodigital.backend.dto.request.ProductoMediaRequest;
import ar.com.mercadodigital.backend.dto.request.ProveedorCategoriaProductoSugerenciaRequest;
import ar.com.mercadodigital.backend.dto.request.ProveedorProductoUpsertRequest;
import ar.com.mercadodigital.backend.dto.response.PedidoProveedorResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoSugerenciaResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoImportResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoResponse;
import ar.com.mercadodigital.backend.service.BandejasOperativasService;
import ar.com.mercadodigital.backend.service.CategoriaProductoService;
import ar.com.mercadodigital.backend.service.ProveedorOwnershipService;
import ar.com.mercadodigital.backend.service.ProveedorProductoImportService;
import ar.com.mercadodigital.backend.service.ProveedorProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/proveedores")
public class ProveedorController {

    private final BandejasOperativasService bandejasOperativasService;
    private final ProveedorOwnershipService proveedorOwnershipService;
    private final ProveedorProductoService proveedorProductoService;
    private final ProveedorProductoImportService proveedorProductoImportService;
    private final CategoriaProductoService categoriaProductoService;

    @GetMapping("/{proveedorId}/pedidos-pendientes")
    public ResponseEntity<List<PedidoProveedorResponse>> getPedidosPendientes(
            @PathVariable UUID proveedorId,
            Authentication authentication) {
        proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        return ResponseEntity.ok(bandejasOperativasService.getPedidosPendientesProveedor(proveedorId));
    }

    @GetMapping("/{proveedorId}/productos")
    public ResponseEntity<List<ProveedorProductoResponse>> getProductos(
            @PathVariable UUID proveedorId,
            Authentication authentication) {
        return ResponseEntity.ok(proveedorProductoService.getProductos(proveedorId, authentication));
    }

    @GetMapping("/{proveedorId}/categorias-producto")
    public ResponseEntity<List<ProveedorCategoriaProductoResponse>> getCategoriasProducto(
            @PathVariable UUID proveedorId,
            Authentication authentication) {
        return ResponseEntity.ok(categoriaProductoService.getCategoriasActivasProveedor(proveedorId, authentication));
    }

    @PostMapping("/{proveedorId}/categorias-producto/sugerencias")
    public ResponseEntity<ProveedorCategoriaProductoSugerenciaResponse> sugerirCategoriaProducto(
            @PathVariable UUID proveedorId,
            @Valid @RequestBody ProveedorCategoriaProductoSugerenciaRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaProductoService.sugerirCategoriaProveedor(proveedorId, request, authentication));
    }

    @PostMapping("/{proveedorId}/productos")
    public ResponseEntity<ProveedorProductoResponse> crearProducto(
            @PathVariable UUID proveedorId,
            @Valid @RequestBody ProveedorProductoUpsertRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proveedorProductoService.crearProducto(proveedorId, request, authentication));
    }

    @PutMapping("/{proveedorId}/productos/{id}")
    public ResponseEntity<ProveedorProductoResponse> actualizarProducto(
            @PathVariable UUID proveedorId,
            @PathVariable UUID id,
            @Valid @RequestBody ProveedorProductoUpsertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(proveedorProductoService.actualizarProducto(proveedorId, id, request, authentication));
    }

    @DeleteMapping("/{proveedorId}/productos/{id}")
    public ResponseEntity<ProveedorProductoResponse> desactivarProducto(
            @PathVariable UUID proveedorId,
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(proveedorProductoService.desactivarProducto(proveedorId, id, authentication));
    }

    @PostMapping("/{proveedorId}/productos/importacion")
    public ResponseEntity<ProveedorProductoImportResponse> importarProductos(
            @PathVariable UUID proveedorId,
            @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication) {
        return ResponseEntity.ok(proveedorProductoImportService.importar(proveedorId, archivo, authentication));
    }

    @GetMapping("/{proveedorId}/productos/{id}/media")
    public ResponseEntity<List<ProductoMediaResponse>> getMedia(
            @PathVariable UUID proveedorId,
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(proveedorProductoService.getMedia(proveedorId, id, authentication));
    }

    @PostMapping("/{proveedorId}/productos/{id}/media")
    public ResponseEntity<ProductoMediaResponse> addMedia(
            @PathVariable UUID proveedorId,
            @PathVariable UUID id,
            @Valid @RequestBody ProductoMediaRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proveedorProductoService.addMedia(proveedorId, id, request, authentication));
    }

    @DeleteMapping("/{proveedorId}/productos/{id}/media/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable UUID proveedorId,
            @PathVariable UUID id,
            @PathVariable UUID mediaId,
            Authentication authentication) {
        proveedorProductoService.deleteMedia(proveedorId, id, mediaId, authentication);
        return ResponseEntity.noContent().build();
    }
}
