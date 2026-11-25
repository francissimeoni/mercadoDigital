package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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

import ar.com.mercadodigital.backend.dto.request.AdminCanonizacionCrearCanonicoRequest;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionAsignadoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionCandidatoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionPendienteResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionResultadoResponse;
import ar.com.mercadodigital.backend.service.AdminCanonizacionService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/canonizacion")
public class AdminCanonizacionController {

    private final AdminCanonizacionService adminCanonizacionService;

    @GetMapping("/pendientes")
    public ResponseEntity<List<AdminCanonizacionPendienteResponse>> listarPendientes() {
        return ResponseEntity.ok(adminCanonizacionService.listarPendientes());
    }

    @GetMapping("/asignados")
    public ResponseEntity<List<AdminCanonizacionAsignadoResponse>> listarAsignados(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(adminCanonizacionService.listarAsignados(q, limit));
    }

    @GetMapping("/candidatos")
    public ResponseEntity<List<AdminCanonizacionCandidatoResponse>> listarCandidatos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(adminCanonizacionService.listarCandidatos(q, limit));
    }

    @PutMapping("/productos/{productoId}/asociar/{productoCanonicoId}")
    public ResponseEntity<AdminCanonizacionResultadoResponse> asociarExistente(
            @PathVariable UUID productoId,
            @PathVariable UUID productoCanonicoId) {
        return ResponseEntity.ok(adminCanonizacionService.asociarAExistente(productoId, productoCanonicoId));
    }

    @PostMapping("/productos/{productoId}/crear-canonico")
    public ResponseEntity<AdminCanonizacionResultadoResponse> crearCanonico(
            @PathVariable UUID productoId,
            @RequestBody(required = false) AdminCanonizacionCrearCanonicoRequest request) {
        return ResponseEntity.ok(adminCanonizacionService.crearCanonicoYAsociar(productoId, request));
    }

    @DeleteMapping("/productos/{productoId}/canonico")
    public ResponseEntity<AdminCanonizacionResultadoResponse> desasociarCanonico(@PathVariable UUID productoId) {
        return ResponseEntity.ok(adminCanonizacionService.desasociarCanonico(productoId));
    }
}
