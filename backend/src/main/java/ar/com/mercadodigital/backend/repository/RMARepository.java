package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.RMA;

public interface RMARepository extends JpaRepository<RMA, UUID> {
    @Override
    @EntityGraph(attributePaths = { "pedidoItem", "pedidoItem.producto", "pedidoItem.producto.productoCanonico",
            "pedidoItem.proveedor", "pedidoItem.proveedor.usuario", "pedidoItem.pedido", "pedidoItem.pedido.envio",
            "pedidoItem.pedido.envio.transportista", "pedidoItem.pedido.cliente", "pedidoItem.pedido.revendedor",
            "pedidoItem.pedido.revendedor.storefront", "creadoPor" })
    Optional<RMA> findById(UUID id);

    @EntityGraph(attributePaths = { "pedidoItem", "pedidoItem.producto", "pedidoItem.producto.productoCanonico",
            "pedidoItem.proveedor", "pedidoItem.proveedor.usuario", "pedidoItem.pedido", "pedidoItem.pedido.envio",
            "pedidoItem.pedido.envio.transportista", "pedidoItem.pedido.cliente", "pedidoItem.pedido.revendedor",
            "pedidoItem.pedido.revendedor.storefront", "creadoPor" })
    List<RMA> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = { "pedidoItem", "pedidoItem.producto", "pedidoItem.producto.productoCanonico",
            "pedidoItem.proveedor", "pedidoItem.proveedor.usuario", "pedidoItem.pedido", "pedidoItem.pedido.envio",
            "pedidoItem.pedido.envio.transportista", "pedidoItem.pedido.cliente", "pedidoItem.pedido.revendedor",
            "pedidoItem.pedido.revendedor.storefront", "creadoPor" })
    List<RMA> findByPedidoItem_Pedido_IdOrderByCreatedAtDesc(UUID pedidoId);

    @EntityGraph(attributePaths = { "pedidoItem", "pedidoItem.producto", "pedidoItem.producto.productoCanonico",
            "pedidoItem.proveedor", "pedidoItem.proveedor.usuario", "pedidoItem.pedido", "pedidoItem.pedido.envio",
            "pedidoItem.pedido.envio.transportista", "pedidoItem.pedido.cliente", "pedidoItem.pedido.revendedor",
            "pedidoItem.pedido.revendedor.storefront", "creadoPor" })
    List<RMA> findByCreadoPor_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = { "pedidoItem", "pedidoItem.producto", "pedidoItem.producto.productoCanonico",
            "pedidoItem.proveedor", "pedidoItem.proveedor.usuario", "pedidoItem.pedido", "pedidoItem.pedido.envio",
            "pedidoItem.pedido.envio.transportista", "pedidoItem.pedido.cliente", "pedidoItem.pedido.revendedor",
            "pedidoItem.pedido.revendedor.storefront", "creadoPor" })
    List<RMA> findByCreadoPor_EmailIgnoreCaseAndPedidoItem_Pedido_IdOrderByCreatedAtDesc(String email, UUID pedidoId);

    @EntityGraph(attributePaths = { "pedidoItem", "pedidoItem.producto", "pedidoItem.producto.productoCanonico",
            "pedidoItem.proveedor", "pedidoItem.proveedor.usuario", "pedidoItem.pedido", "pedidoItem.pedido.envio",
            "pedidoItem.pedido.envio.transportista", "pedidoItem.pedido.cliente", "pedidoItem.pedido.revendedor",
            "pedidoItem.pedido.revendedor.storefront", "creadoPor" })
    Optional<RMA> findByIdAndCreadoPor_EmailIgnoreCase(UUID id, String email);

    boolean existsByPedidoItem_IdAndEstadoRmaIn(UUID pedidoItemId, List<ar.com.mercadodigital.backend.entity.RmaEstado> estados);
}
