package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    @Override
    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    List<Producto> findAll();

    @Override
    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    Optional<Producto> findById(UUID id);

    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    List<Producto> findByProductoCanonico_IdAndActivoTrue(UUID productoCanonicoId);

    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    List<Producto> findByProveedor_IdOrderByUpdatedAtDesc(UUID proveedorId);

    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    List<Producto> findByProductoCanonicoIsNullAndActivoTrueOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    List<Producto> findByProductoCanonicoIsNotNullAndActivoTrueOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    Optional<Producto> findByIdAndProveedor_Id(UUID id, UUID proveedorId);

    @EntityGraph(attributePaths = { "proveedor", "productoCanonico" })
    Optional<Producto> findByProveedor_IdAndSkuIgnoreCase(UUID proveedorId, String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);
}
