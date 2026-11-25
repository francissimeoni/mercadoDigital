/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevendedorPedidoService {
    private final PedidoRepository pedidoRepository;
    private final RevendedorProductoRepository revendedorProductoRepository;

    @Transactional
    public void cancelarImpago(UUID revendedorId, UUID pedidoId) {
        Pedido pedido = this.pedidoRepository.findByIdForUpdate(pedidoId).filter(item -> item.getRevendedor().getId().equals(revendedorId)).orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (PedidoEstado.CANCELADO.equals((Object)pedido.getEstadoPedido())) {
            return;
        }
        if (!PedidoEstado.CREADO.equals((Object)pedido.getEstadoPedido()) || pedido.getPago() == null || !PagoEstado.PENDIENTE.equals((Object)pedido.getPago().getEstadoPago()) && !PagoEstado.RECHAZADO.equals((Object)pedido.getPago().getEstadoPago())) {
            throw new OperacionEnConflictoException("Solo se pueden cancelar pedidos pendientes de pago");
        }
        pedido.getItems().forEach(item -> {
            RevendedorProducto producto = item.getRevendedorProducto();
            if (producto != null) {
                RevendedorProducto bloqueado = this.revendedorProductoRepository
                        .findForUpdate(producto.getId(), revendedorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto del pedido no encontrado"));
                bloqueado.setStock((bloqueado.getStock() != null ? bloqueado.getStock() : 0) + item.getCantidad());
                this.revendedorProductoRepository.save(bloqueado);
            }
        });
        pedido.setEstadoPedido(PedidoEstado.CANCELADO);
        pedido.getPago().setEstadoPago(PagoEstado.CANCELADO);
        if (pedido.getEnvio() != null) {
            pedido.getEnvio().setEstadoEnvio(EnvioEstado.CANCELADO);
        }
        this.pedidoRepository.save(pedido);
    }

    public RevendedorPedidoService(PedidoRepository pedidoRepository, RevendedorProductoRepository revendedorProductoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.revendedorProductoRepository = revendedorProductoRepository;
    }
}

