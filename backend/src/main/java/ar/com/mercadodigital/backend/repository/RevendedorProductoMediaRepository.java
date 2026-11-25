package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;

public interface RevendedorProductoMediaRepository extends JpaRepository<RevendedorProductoMedia, UUID> {
    Optional<RevendedorProductoMedia> findByIdAndRevendedorProducto_IdAndRevendedorProducto_Revendedor_Id(
            UUID id, UUID productoId, UUID revendedorId);
}
