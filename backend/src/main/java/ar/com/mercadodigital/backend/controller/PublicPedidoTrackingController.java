package ar.com.mercadodigital.backend.controller;

import ar.com.mercadodigital.backend.dto.response.EnvioResponse;
import ar.com.mercadodigital.backend.service.EnvioService;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingResponse;
import ar.com.mercadodigital.backend.service.PublicPedidoTrackingService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/pedido")
public class PublicPedidoTrackingController {

    private final PublicPedidoTrackingService publicPedidoTrackingService;

    @GetMapping("/{trackingToken}")
    public ResponseEntity<PublicPedidoTrackingResponse> getByTrackingToken(
            @PathVariable String trackingToken,
            @RequestParam String storefrontHost) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(publicPedidoTrackingService.getByTrackingToken(trackingToken, storefrontHost));
    }

    private final EnvioService envioService;

    @PostMapping(value={"/{trackingToken}/confirmar-entrega"})
    public ResponseEntity<EnvioResponse> confirmarEntrega(@PathVariable String trackingToken, @RequestParam String storefrontHost) {
        return ResponseEntity.ok(this.envioService.marcarEntregadoPorCliente(trackingToken, storefrontHost));
    }
}
