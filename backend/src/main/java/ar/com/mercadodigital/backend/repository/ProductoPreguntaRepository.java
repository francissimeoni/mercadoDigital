package ar.com.mercadodigital.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ProductoPregunta;

public interface ProductoPreguntaRepository extends JpaRepository<ProductoPregunta, UUID> {
    long countByRevendedor_IdAndFechaPreguntaGreaterThanEqualAndFechaPreguntaLessThan(
            UUID revendedorId, LocalDateTime desde, LocalDateTime hasta);

    List<ProductoPregunta> findByRevendedor_IdAndRevendedorProductoIsNotNullOrderByFechaPreguntaDesc(UUID revendedorId);

    Optional<ProductoPregunta> findByIdAndRevendedor_IdAndRevendedorProductoIsNotNull(UUID id, UUID revendedorId);

    List<ProductoPregunta> findByRevendedorProducto_IdAndEstadoOrderByFechaPreguntaDesc(UUID productoId, String estado);
}
