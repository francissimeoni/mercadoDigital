package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ProductoCanonico;

public interface ProductoCanonicoRepository extends JpaRepository<ProductoCanonico, UUID> {
    List<ProductoCanonico> findByActivoTrueOrderByNombrePublicoAsc();
    Optional<ProductoCanonico> findByIdAndActivoTrue(UUID id);
}
