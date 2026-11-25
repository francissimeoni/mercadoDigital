package ar.com.mercadodigital.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import ar.com.mercadodigital.backend.dto.request.PublicPedidoDirectoRequest;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoDirectoResponse;
import ar.com.mercadodigital.backend.dto.response.TransferenciaComprobanteResponse;
import ar.com.mercadodigital.backend.service.PublicPedidoDirectoService;
import ar.com.mercadodigital.backend.service.TransferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/pedido")
public class PublicPedidoDirectoController {

    private final PublicPedidoDirectoService publicPedidoDirectoService;
    private final TransferenciaService transferenciaService;

    @PostMapping("/directo")
    public ResponseEntity<PublicPedidoDirectoResponse> crearPedidoDirecto(
            @Valid @RequestBody PublicPedidoDirectoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicPedidoDirectoService.crearPedidoDirecto(request));
    }

    @PostMapping("/transferencia/{token}/comprobante")
    public ResponseEntity<TransferenciaComprobanteResponse> cargarComprobante(
            @PathVariable String token,
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(transferenciaService.cargarComprobante(token, archivo));
    }
}
