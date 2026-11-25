package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ProductoMedia;

public interface ProductoMediaRepository extends JpaRepository<ProductoMedia, UUID> {

    @EntityGraph(attributePaths = { "producto" })
    List<ProductoMedia> findByProducto_IdOrderByOrdenAsc(UUID productoId);

    @EntityGraph(attributePaths = { "producto" })
    Optional<ProductoMedia> findByProducto_IdAndEsPrincipalTrue(UUID productoId);

    @EntityGraph(attributePaths = { "producto" })
    Optional<ProductoMedia> findByIdAndProducto_Id(UUID id, UUID productoId);
}
