package ar.com.mercadodigital.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.DespacharEnvioRequest;
import ar.com.mercadodigital.backend.dto.request.LiberarProveedorRequest;
import ar.com.mercadodigital.backend.dto.response.EnvioResponse;
import ar.com.mercadodigital.backend.dto.response.LiquidacionPartesResponse;
import ar.com.mercadodigital.backend.dto.response.LiquidacionProveedorResponse;
import ar.com.mercadodigital.backend.service.EnvioService;
import ar.com.mercadodigital.backend.service.LiquidacionProveedorService;
import ar.com.mercadodigital.backend.service.RotuloEnvioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/envios")
public class EnvioController {

    private final EnvioService envioService;
    private final RotuloEnvioService rotuloEnvioService;
    private final LiquidacionProveedorService liquidacionProveedorService;

    @GetMapping("/{pedidoId}")
    public ResponseEntity<EnvioResponse> getByPedidoId(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(envioService.getByPedidoId(pedidoId));
    }

    @GetMapping(value = "/{pedidoId}/rotulo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getRotulo(@PathVariable UUID pedidoId) {
        byte[] pdf = rotuloEnvioService.generarRotuloPdf(pedidoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=rotulo-" + pedidoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{pedidoId}/listo-para-retiro")
    public ResponseEntity<EnvioResponse> marcarListoParaRetiro(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(envioService.marcarListoParaRetiro(pedidoId));
    }

    @PostMapping("/{pedidoId}/retirado")
    public ResponseEntity<EnvioResponse> marcarRetirado(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(envioService.marcarRetirado(pedidoId));
    }

    @PostMapping("/{pedidoId}/despachar")
    public ResponseEntity<EnvioResponse> despachar(
            @PathVariable UUID pedidoId,
            @Valid @RequestBody DespacharEnvioRequest request) {
        return ResponseEntity.ok(envioService.despachar(pedidoId, request));
    }

    @PostMapping("/{pedidoId}/entregar")
    public ResponseEntity<EnvioResponse> marcarEntregado(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(envioService.marcarEntregado(pedidoId));
    }

    @GetMapping("/{pedidoId}/liquidacion-proveedor")
    public ResponseEntity<LiquidacionProveedorResponse> getLiquidacionProveedor(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(liquidacionProveedorService.getByPedidoId(pedidoId));
    }

    @GetMapping("/{pedidoId}/liquidacion-partes")
    public ResponseEntity<LiquidacionPartesResponse> getLiquidacionPartes(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(liquidacionProveedorService.getPartesByPedidoId(pedidoId));
    }

    @PostMapping("/{pedidoId}/liberar-proveedor")
    public ResponseEntity<LiquidacionProveedorResponse> liberarProveedor(
            @PathVariable UUID pedidoId,
            @Valid @RequestBody LiberarProveedorRequest request) {
        return ResponseEntity.ok(liquidacionProveedorService.liberarManual(pedidoId, request.getMotivo()));
    }
}
