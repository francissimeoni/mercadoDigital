package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import jakarta.persistence.LockModeType;

public interface RevendedorProductoRepository extends JpaRepository<RevendedorProducto, UUID> {

    @EntityGraph(attributePaths = { "media" })
    List<RevendedorProducto> findByRevendedor_IdAndIdIn(UUID revendedorId, java.util.Collection<UUID> ids);

    Optional<RevendedorProducto> findByRevendedor_IdAndCatalogoAltaToken(UUID revendedorId, String token);

    @EntityGraph(attributePaths = { "categoriaProducto", "media" })
    List<RevendedorProducto> findByRevendedor_IdOrderByUpdatedAtDesc(UUID revendedorId);

    @EntityGraph(attributePaths = { "categoriaProducto", "media" })
    Optional<RevendedorProducto> findByIdAndRevendedor_Id(UUID id, UUID revendedorId);

    @EntityGraph(attributePaths = { "categoriaProducto", "media" })
    Optional<RevendedorProducto> findByRevendedor_IdAndSkuIgnoreCase(UUID revendedorId, String sku);

    @EntityGraph(attributePaths = { "categoriaProducto", "media" })
    List<RevendedorProducto> findByRevendedor_IdAndActivoTrueAndStockGreaterThanOrderByNombreAsc(
            UUID revendedorId, Integer stock);

    boolean existsByRevendedor_IdAndSkuIgnoreCaseAndIdNot(UUID revendedorId, String sku, UUID id);

    boolean existsByRevendedor_IdAndSkuIgnoreCase(UUID revendedorId, String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from RevendedorProducto p where p.id = :id and p.revendedor.id = :revendedorId")
    Optional<RevendedorProducto> findForUpdate(
            @Param("id") UUID id,
            @Param("revendedorId") UUID revendedorId);
}
