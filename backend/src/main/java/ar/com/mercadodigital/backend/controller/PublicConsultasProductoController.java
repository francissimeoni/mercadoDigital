package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.PublicDisponibilidadRequest;
import ar.com.mercadodigital.backend.dto.request.PublicProductoPreguntaRequest;
import ar.com.mercadodigital.backend.dto.response.ConsultaDisponibilidadResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoPreguntaResponse;
import ar.com.mercadodigital.backend.service.PublicConsultasProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/consultas-producto")
public class PublicConsultasProductoController {

    private final PublicConsultasProductoService service;

    @GetMapping("/preguntas")
    public ResponseEntity<List<ProductoPreguntaResponse>> listarPreguntas(
            @RequestParam UUID productoId,
            @RequestParam UUID revendedorId) {
        return ResponseEntity.ok(service.listarPreguntas(productoId, revendedorId));
    }

    @PostMapping("/preguntas")
    public ResponseEntity<ProductoPreguntaResponse> crearPregunta(
            @Valid @RequestBody PublicProductoPreguntaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearPregunta(request));
    }

    @PostMapping("/disponibilidad")
    public ResponseEntity<ConsultaDisponibilidadResponse> crearDisponibilidad(
            @Valid @RequestBody PublicDisponibilidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearDisponibilidad(request));
    }

    @GetMapping("/disponibilidad/{token}")
    public ResponseEntity<ConsultaDisponibilidadResponse> getDisponibilidad(@PathVariable String token) {
        return ResponseEntity.ok(service.getDisponibilidad(token));
    }
}
