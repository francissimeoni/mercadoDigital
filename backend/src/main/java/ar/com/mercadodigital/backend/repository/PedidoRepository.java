package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import ar.com.mercadodigital.backend.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    @Override
    @EntityGraph(attributePaths = { "cliente", "revendedor", "items", "items.producto",
            "items.proveedor", "items.revendedorProducto", "pago", "envio" })
    Optional<Pedido> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = { "revendedor", "cliente", "pago", "envio", "envio.transportista",
            "items", "items.producto", "items.producto.productoCanonico", "items.revendedorProducto" })
    List<Pedido> findByRevendedor_Id(UUID revendedorId);

    @EntityGraph(attributePaths = {
            "cliente",
            "revendedor",
            "items",
            "items.producto",
            "items.producto.productoCanonico",
            "items.proveedor",
            "items.revendedorProducto",
            "items.revendedorProducto.media",
            "pago",
            "envio"
    })
    Optional<Pedido> findByTrackingToken(String trackingToken);


    @EntityGraph(attributePaths={"revendedor", "cliente", "pago", "envio", "items", "items.producto", "items.producto.productoCanonico", "items.revendedorProducto"})
    public List<Pedido> findTop100ByCliente_IdAndRevendedor_IdOrderByCreatedAtDesc(UUID var1, UUID var2);
}
