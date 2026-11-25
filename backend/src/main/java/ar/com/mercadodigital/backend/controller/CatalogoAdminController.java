package ar.com.mercadodigital.backend.controller;

import static ar.com.mercadodigital.backend.dto.CatalogoAdminDto.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import ar.com.mercadodigital.backend.service.CatalogoAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequiredArgsConstructor
@RequestMapping("/revendedores/{revendedorId}/catalogo-config")
public class CatalogoAdminController {
    private final CatalogoAdminService service;
    @GetMapping public Config configuracion(@PathVariable UUID revendedorId) { return service.configuracion(revendedorId); }
    @PostMapping("/vista-previa") public Impacto previewConfig(@PathVariable UUID revendedorId, @Valid @RequestBody ConfigInput input) { return service.previewConfig(revendedorId, input); }
    @PutMapping public Config guardarConfig(@PathVariable UUID revendedorId, @Valid @RequestBody ConfigInput input) { return service.guardarConfig(revendedorId, input); }
    @GetMapping("/categorias") public List<Categoria> categorias(@PathVariable UUID revendedorId) { return service.categorias(revendedorId); }
    @PostMapping("/categorias") @ResponseStatus(HttpStatus.CREATED)
    public Categoria crearCategoria(@PathVariable UUID revendedorId, @Valid @RequestBody CategoriaInput input) { return service.crearCategoria(revendedorId, input); }
    @PostMapping("/categorias/{id}/vista-previa") public Impacto previewCategoria(@PathVariable UUID revendedorId, @PathVariable UUID id, @Valid @RequestBody CategoriaInput input) { return service.previewCategoria(revendedorId, id, input); }
    @PutMapping("/categorias/{id}") public Categoria guardarCategoria(@PathVariable UUID revendedorId, @PathVariable UUID id, @Valid @RequestBody CategoriaInput input) { return service.actualizarCategoria(revendedorId, id, input); }
    @DeleteMapping("/categorias/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCategoria(@PathVariable UUID revendedorId, @PathVariable UUID id, @RequestParam long version) { service.eliminarCategoria(revendedorId, id, version); }
    @GetMapping("/productos") public List<Producto> productos(@PathVariable UUID revendedorId) { return service.productos(revendedorId); }
    @PostMapping("/productos/{id}/vista-previa") public ProductoPreview previewProducto(@PathVariable UUID revendedorId, @PathVariable UUID id, @Valid @RequestBody ProductoInput input) { return service.previewProducto(revendedorId, id, input); }
    @PutMapping("/productos/{id}") public Producto guardarProducto(@PathVariable UUID revendedorId, @PathVariable UUID id, @Valid @RequestBody ProductoInput input) { return service.guardarProducto(revendedorId, id, input); }
}
