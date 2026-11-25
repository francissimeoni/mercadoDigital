package ar.com.mercadodigital.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoPagoPspResponse;
import ar.com.mercadodigital.backend.service.PublicPedidoPagoPspService;
import ar.com.mercadodigital.backend.service.PublicPedidoTrackingService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/pedido")
public class PublicPedidoPagoPspController {

    private final PublicPedidoPagoPspService publicPedidoPagoPspService;
    private final PublicPedidoTrackingService publicPedidoTrackingService;

    /** Reanudación explícita: valida el pedido y su tienda antes de tocar PSP. */
    @PostMapping("/{trackingToken}/reanudar-pago")
    public ResponseEntity<PublicPedidoPagoPspResponse> reanudarPago(
            @PathVariable String trackingToken, @RequestParam String storefrontHost) {
        var pedido = publicPedidoTrackingService.getPedidoPublicoByTrackingToken(trackingToken, storefrontHost);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(publicPedidoPagoPspService.prepararCheckout(pedido.getId()));
    }

    @PostMapping("/{pedidoId}/pago")
    public ResponseEntity<PublicPedidoPagoPspResponse> prepararCheckout(@PathVariable UUID pedidoId,
            @RequestParam String trackingToken, @RequestParam String storefrontHost) {
        var pedido = publicPedidoTrackingService.getPedidoPublicoByTrackingToken(trackingToken, storefrontHost);
        if (!pedidoId.equals(pedido.getId())) {
            throw new ar.com.mercadodigital.backend.exception.ResourceNotFoundException("Pedido no encontrado");
        }
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(publicPedidoPagoPspService.prepararCheckout(pedido.getId()));
    }
}
