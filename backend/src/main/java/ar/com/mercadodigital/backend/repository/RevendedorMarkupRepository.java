package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.RevendedorMarkup;

public interface RevendedorMarkupRepository extends JpaRepository<RevendedorMarkup, UUID> {
    Optional<RevendedorMarkup> findFirstByProducto_IdAndActivoTrueOrderByUpdatedAtDesc(UUID productoId);

    Optional<RevendedorMarkup> findByRevendedor_IdAndProducto_IdAndActivoTrue(UUID revendedorId, UUID productoId);
}
