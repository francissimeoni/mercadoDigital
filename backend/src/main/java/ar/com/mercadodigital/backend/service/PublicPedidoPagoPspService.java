package ar.com.mercadodigital.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoPagoPspResponse;
import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicPedidoPagoPspService {

    private static final String METODO_PSP = "PSP";

    private final PedidoRepository pedidoRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;

    @Transactional
    public PublicPedidoPagoPspResponse prepararCheckout(UUID pedidoId) {
        mercadoPagoClient.exigirHabilitado();
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + pedidoId));

        if (!OrigenPedido.PUBLICO_DIRECTO.equals(pedido.getOrigenPedido())) {
            throw new ResourceNotFoundException("Pedido publico no disponible: " + pedidoId);
        }

        if (!PedidoEstado.CREADO.equals(pedido.getEstadoPedido())) {
            throw new OperacionEnConflictoException(
                    "Pedido en estado invalido para iniciar PSP: " + pedido.getEstadoPedido());
        }

        Pago pago = pedido.getPago();
        if (pago == null) {
            throw new OperacionEnConflictoException("Pedido sin pago pendiente para iniciar PSP: " + pedidoId);
        }

        if (!PagoEstado.PENDIENTE.equals(pago.getEstadoPago())) {
            throw new OperacionEnConflictoException(
                    "Pago en estado invalido para iniciar PSP: " + pago.getEstadoPago());
        }

        if (!METODO_PSP.equalsIgnoreCase(pago.getMetodo())) {
            throw new OperacionEnConflictoException("Metodo de pago invalido para PSP: " + pago.getMetodo());
        }

        if (pago.getPspReferencia() == null || pago.getPspReferencia().isBlank()) {
            pago.setPspReferencia("psp-publico-" + pago.getId());
        }

        if (pago.getPspCheckoutUrl() == null || pago.getPspCheckoutUrl().isBlank()) {
            RevendedorMercadoPagoService.RevendedorMercadoPagoResolution resolution =
                    revendedorMercadoPagoService.resolveForRevendedor(pedido.getRevendedor().getId());
            if (!resolution.readyForCheckout()) {
                throw new OperacionEnConflictoException(
                        "No se pudo iniciar checkout: el revendedor no tiene una cuenta Mercado Pago lista para cobrar.");
            }
            MercadoPagoClient.MercadoPagoCheckoutData checkoutData =
                    mercadoPagoClient.crearPreferencia(pedido, pago, resolution.accessToken());
            pago.setPspCheckoutUrl(checkoutData.checkoutUrl());
            pedidoRepository.save(pedido);
        }

        return PublicPedidoPagoPspResponse.builder()
                .pedidoId(pedido.getId())
                .estadoPedido(pedido.getEstadoPedido().name())
                .estadoPago(pago.getEstadoPago().name())
                .metodoPago(pago.getMetodo())
                .monto(pago.getMonto())
                .referenciaPedido(pedido.getId().toString())
                .referenciaPago(pago.getPspReferencia())
                .checkoutUrl(pago.getPspCheckoutUrl())
                .build();
    }

}
