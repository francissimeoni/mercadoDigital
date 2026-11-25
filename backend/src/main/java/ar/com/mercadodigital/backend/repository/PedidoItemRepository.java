package ar.com.mercadodigital.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, UUID> {
    @Override
    @EntityGraph(attributePaths = { "pedido", "pedido.envio", "pedido.cliente", "producto", "proveedor" })
    Optional<PedidoItem> findById(UUID id);

    @EntityGraph(attributePaths = { "pedido", "pedido.envio", "producto", "proveedor" })
    List<PedidoItem> findByProveedor_IdAndPedido_Envio_EstadoEnvioIn(UUID proveedorId, Collection<EnvioEstado> estados);

    @EntityGraph(attributePaths = { "pedido", "pedido.envio", "producto", "proveedor" })
    List<PedidoItem> findByPedido_Envio_EstadoEnvio(EnvioEstado estadoEnvio);
}
