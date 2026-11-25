package ar.com.mercadodigital.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.CrearTransportistaRequest;
import ar.com.mercadodigital.backend.dto.response.PedidoLogisticaResponse;
import ar.com.mercadodigital.backend.dto.response.TransportistaResponse;
import ar.com.mercadodigital.backend.service.BandejasOperativasService;
import ar.com.mercadodigital.backend.service.TransportistaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/logistica")
public class LogisticaController {

    private final BandejasOperativasService bandejasOperativasService;
    private final TransportistaService transportistaService;

    @GetMapping("/retiros-pendientes")
    public ResponseEntity<List<PedidoLogisticaResponse>> getRetirosPendientes() {
        return ResponseEntity.ok(bandejasOperativasService.getRetirosPendientes());
    }

    @GetMapping("/despachos-pendientes")
    public ResponseEntity<List<PedidoLogisticaResponse>> getDespachosPendientes() {
        return ResponseEntity.ok(bandejasOperativasService.getDespachosPendientes());
    }

    @GetMapping("/transportistas")
    public ResponseEntity<List<TransportistaResponse>> getTransportistas() {
        return ResponseEntity.ok(transportistaService.getActivos());
    }

    @PostMapping("/transportistas")
    public ResponseEntity<TransportistaResponse> crearTransportista(
            @Valid @RequestBody CrearTransportistaRequest request) {
        return ResponseEntity.ok(transportistaService.crear(request));
    }
}
