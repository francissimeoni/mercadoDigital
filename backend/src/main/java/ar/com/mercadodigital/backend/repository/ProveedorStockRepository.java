package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.mercadodigital.backend.entity.ProveedorStock;
import jakarta.persistence.LockModeType;

public interface ProveedorStockRepository extends JpaRepository<ProveedorStock, UUID> {
    Optional<ProveedorStock> findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(UUID proveedorId, UUID productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ps
            from ProveedorStock ps
            where ps.producto.id = :productoId
            order by ps.precioCostoActual asc, ps.updatedAt desc
            """)
    List<ProveedorStock> findByProductoIdForUpdate(@Param("productoId") UUID productoId);

    @Query("select sum(ps.stock) from ProveedorStock ps where ps.producto.id = :productoId")
    Integer sumStockByProductoId(@Param("productoId") UUID productoId);

    @Query("select sum(ps.stock) from ProveedorStock ps where ps.proveedor.id = :proveedorId and ps.producto.id = :productoId")
    Integer sumStockByProveedorIdAndProductoId(@Param("proveedorId") UUID proveedorId, @Param("productoId") UUID productoId);
}
