package ar.com.mercadodigital.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import ar.com.mercadodigital.backend.dto.request.AdminUsuarioCreateRequest;
import ar.com.mercadodigital.backend.dto.request.AdminUsuarioUpdateRequest;
import ar.com.mercadodigital.backend.dto.response.AdminUsuarioResponse;
import ar.com.mercadodigital.backend.service.AdminUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @GetMapping
    public ResponseEntity<List<AdminUsuarioResponse>> listar() {
        return ResponseEntity.ok(adminUsuarioService.listar());
    }

    @PostMapping
    public ResponseEntity<AdminUsuarioResponse> crear(@Valid @RequestBody AdminUsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUsuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUsuarioResponse> actualizar(
            @PathVariable java.util.UUID id,
            @Valid @RequestBody AdminUsuarioUpdateRequest request) {
        return ResponseEntity.ok(adminUsuarioService.actualizar(id, request));
    }
}
