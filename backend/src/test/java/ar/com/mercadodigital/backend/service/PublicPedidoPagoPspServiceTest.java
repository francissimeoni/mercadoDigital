package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoPagoPspResponse;
import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.exception.IntegracionPspException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class PublicPedidoPagoPspServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private MercadoPagoClient mercadoPagoClient;
    @Mock
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private PublicPedidoPagoPspService publicPedidoPagoPspService;

    @Test
    void preparaCheckoutPspExitosamenteYGeneraReferenciaUnaSolaVez() {
        UUID pedidoId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Pedido pedido = pedidoPublicoBase(pedidoId, pagoId);

        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revendedorMercadoPagoService.resolveForRevendedor(pedido.getRevendedor().getId()))
                .thenReturn(new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("seller-token", "collector-1", false, true));
        when(mercadoPagoClient.crearPreferencia(any(Pedido.class), any(Pago.class), any(String.class)))
                .thenReturn(new MercadoPagoClient.MercadoPagoCheckoutData("https://www.mercadopago.com/checkout/v1/redirect?pref=1", "pref_1", "https://www.mercadopago.com/checkout/v1/redirect?pref=1"));

        PublicPedidoPagoPspResponse response = publicPedidoPagoPspService.prepararCheckout(pedidoId);

        assertEquals(pedidoId, response.getPedidoId());
        assertEquals("CREADO", response.getEstadoPedido());
        assertEquals("PENDIENTE", response.getEstadoPago());
        assertEquals("PSP", response.getMetodoPago());
        assertEquals(new BigDecimal("120.00"), response.getMonto());
        assertEquals(pedidoId.toString(), response.getReferenciaPedido());
        assertEquals("psp-publico-" + pagoId, response.getReferenciaPago());
        assertEquals("https://www.mercadopago.com/checkout/v1/redirect?pref=1", response.getCheckoutUrl());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void reutilizaCheckoutExistenteSinRecrearEnMercadoPago() {
        UUID pedidoId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Pedido pedido = pedidoPublicoBase(pedidoId, pagoId);
        pedido.getPago().setPspReferencia("psp-publico-" + pagoId);
        pedido.getPago().setPspCheckoutUrl("https://www.mercadopago.com/checkout/v1/redirect?pref=reutilizada");

        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));

        PublicPedidoPagoPspResponse response = publicPedidoPagoPspService.prepararCheckout(pedidoId);

        assertEquals("psp-publico-" + pagoId, response.getReferenciaPago());
        assertEquals("https://www.mercadopago.com/checkout/v1/redirect?pref=reutilizada", response.getCheckoutUrl());
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(mercadoPagoClient, never()).crearPreferencia(any(Pedido.class), any(Pago.class), any(String.class));
    }

    @Test
    void falloMercadoPagoPropagaErrorSinConfirmarPago() {
        UUID pedidoId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Pedido pedido = pedidoPublicoBase(pedidoId, pagoId);

        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));
        when(revendedorMercadoPagoService.resolveForRevendedor(pedido.getRevendedor().getId()))
                .thenReturn(new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("seller-token", "collector-1", false, true));
        when(mercadoPagoClient.crearPreferencia(any(Pedido.class), any(Pago.class), any(String.class)))
                .thenThrow(new IntegracionPspException("No se pudo iniciar checkout en Mercado Pago."));

        assertThrows(IntegracionPspException.class, () -> publicPedidoPagoPspService.prepararCheckout(pedidoId));
        verify(pedidoRepository, never()).save(any(Pedido.class));
        assertEquals(PedidoEstado.CREADO, pedido.getEstadoPedido());
        assertEquals(PagoEstado.PENDIENTE, pedido.getPago().getEstadoPago());
        verify(pedidoService, never()).cancelarPedido(pedidoId);
    }

    @Test
    void pedidoInexistenteDevuelve404() {
        UUID pedidoId = UUID.randomUUID();
        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> publicPedidoPagoPspService.prepararCheckout(pedidoId));
    }

    @Test
    void pedidoEnEstadoInvalidoDevuelve409() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = pedidoPublicoBase(pedidoId, UUID.randomUUID());
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));

        assertThrows(OperacionEnConflictoException.class, () -> publicPedidoPagoPspService.prepararCheckout(pedidoId));
    }

    @Test
    void pedidoNoPublicoNoExponeCheckout() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = pedidoPublicoBase(pedidoId, UUID.randomUUID());
        pedido.setOrigenPedido(OrigenPedido.OPERATIVO);
        when(pedidoRepository.findByIdForUpdate(pedidoId)).thenReturn(Optional.of(pedido));

        assertThrows(ResourceNotFoundException.class, () -> publicPedidoPagoPspService.prepararCheckout(pedidoId));
    }

    private Pedido pedidoPublicoBase(UUID pedidoId, UUID pagoId) {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setOrigenPedido(OrigenPedido.PUBLICO_DIRECTO);
        pedido.setEstadoPedido(PedidoEstado.CREADO);

        Pago pago = new Pago();
        pago.setId(pagoId);
        pago.setPedido(pedido);
        pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMetodo("PSP");
        pago.setMonto(new BigDecimal("120.00"));
        pedido.setPago(pago);

        Revendedor revendedor = new Revendedor();
        revendedor.setId(UUID.randomUUID());
        pedido.setRevendedor(revendedor);
        return pedido;
    }
}
