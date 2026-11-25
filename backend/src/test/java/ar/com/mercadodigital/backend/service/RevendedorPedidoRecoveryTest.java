package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;

class RevendedorPedidoRecoveryTest {
    private final PedidoRepository orders = mock(PedidoRepository.class);
    private final RevendedorProductoRepository products = mock(RevendedorProductoRepository.class);
    private final RevendedorPedidoService service = new RevendedorPedidoService(orders, products);

    @Test
    void cancellationRestoresStockOnceAndCancelsPaymentAndShipping() {
        Pedido order = order(PagoEstado.PENDIENTE);
        var product = new RevendedorProducto(); product.setId(UUID.randomUUID()); product.setStock(7);
        when(products.findForUpdate(product.getId(), order.getRevendedor().getId())).thenReturn(Optional.of(product));
        var item = new PedidoItem(); item.setCantidad(3); item.setRevendedorProducto(product);
        order.setItems(List.of(item));
        service.cancelarImpago(order.getRevendedor().getId(), order.getId());
        service.cancelarImpago(order.getRevendedor().getId(), order.getId());
        assertEquals(10, product.getStock());
        assertEquals(PedidoEstado.CANCELADO, order.getEstadoPedido());
        assertEquals(PagoEstado.CANCELADO, order.getPago().getEstadoPago());
        assertEquals(EnvioEstado.CANCELADO, order.getEnvio().getEstadoEnvio());
        verify(products, times(1)).save(product); verify(orders, times(1)).save(order);
    }

    @Test
    void confirmedPaymentCannotBeCancelledAsUnpaid() {
        Pedido order = order(PagoEstado.CONFIRMADO);
        assertThrows(OperacionEnConflictoException.class, () -> service.cancelarImpago(order.getRevendedor().getId(), order.getId()));
        verifyNoInteractions(products); verify(orders, never()).save(order);
    }

    @Test
    void resellerCannotCancelAnotherStoresOrder() {
        Pedido order = order(PagoEstado.PENDIENTE);
        assertThrows(ResourceNotFoundException.class, () -> service.cancelarImpago(UUID.randomUUID(), order.getId()));
        verifyNoInteractions(products); verify(orders, never()).save(order);
    }

    private Pedido order(PagoEstado state) {
        var reseller = new Revendedor(); reseller.setId(UUID.randomUUID());
        var order = new Pedido(); order.setId(UUID.randomUUID()); order.setRevendedor(reseller); order.setEstadoPedido(PedidoEstado.CREADO);
        var payment = new Pago(); payment.setEstadoPago(state); order.setPago(payment);
        var shipping = new Envio(); shipping.setEstadoEnvio(EnvioEstado.PENDIENTE_PAGO); order.setEnvio(shipping);
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order)); return order;
    }
}
