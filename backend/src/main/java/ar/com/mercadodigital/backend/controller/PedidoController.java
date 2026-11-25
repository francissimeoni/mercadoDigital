package ar.com.mercadodigital.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.CrearPedidoRequest;
import ar.com.mercadodigital.backend.dto.request.CrearPedidoCanonicoRequest;
import ar.com.mercadodigital.backend.dto.response.PedidoResponse;
import ar.com.mercadodigital.backend.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/pedido")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody CrearPedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearPedido(request));
    }

    @PostMapping("/canonico")
    public ResponseEntity<PedidoResponse> crearPedidoCanonico(@Valid @RequestBody CrearPedidoCanonicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearPedidoCanonico(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> getPedidoById(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.getById(id));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelarPedido(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.cancelarPedido(id));
    }
}
