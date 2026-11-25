package ar.com.mercadodigital.backend.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ar.com.mercadodigital.backend.dto.CatalogoFichaDto.*;
import ar.com.mercadodigital.backend.dto.CatalogoAdminDto.ProductoPreview;
import ar.com.mercadodigital.backend.service.CatalogoFichaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequiredArgsConstructor
@RequestMapping("/revendedores/{revendedorId}/catalogo-config/fichas")
public class CatalogoFichaController {
    private final CatalogoFichaService service;
    @GetMapping("/{id}") public Ficha obtener(@PathVariable UUID revendedorId, @PathVariable UUID id) { return service.obtener(revendedorId, id); }
    @PostMapping("/vista-previa") public ProductoPreview previewNueva(@PathVariable UUID revendedorId, @Valid @RequestBody Input input) { return service.preview(revendedorId, null, input); }
    @PostMapping("/{id}/vista-previa") public ProductoPreview preview(@PathVariable UUID revendedorId, @PathVariable UUID id, @Valid @RequestBody Input input) { return service.preview(revendedorId, id, input); }
    @PostMapping(consumes = "multipart/form-data") public Ficha crear(@PathVariable UUID revendedorId,
            @Valid @RequestPart("ficha") Input input, @RequestPart(value = "imagenes", required = false) List<MultipartFile> archivos) {
        return guardar(revendedorId, null, input, archivos);
    }
    @PutMapping(value = "/{id}", consumes = "multipart/form-data") public Ficha actualizar(@PathVariable UUID revendedorId, @PathVariable UUID id,
            @Valid @RequestPart("ficha") Input input, @RequestPart(value = "imagenes", required = false) List<MultipartFile> archivos) {
        return guardar(revendedorId, id, input, archivos);
    }
    private Ficha guardar(UUID tienda, UUID id, Input input, List<MultipartFile> archivos) {
        return service.guardar(tienda, id, input, archivos == null ? List.of() : archivos,
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
    }
}
