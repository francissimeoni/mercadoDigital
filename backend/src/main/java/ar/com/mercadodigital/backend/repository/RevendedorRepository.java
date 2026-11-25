package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.Revendedor;

public interface RevendedorRepository extends JpaRepository<Revendedor, UUID> {
    Optional<Revendedor> findByDominioIgnoreCase(String dominio);

    boolean existsByDominioIgnoreCase(String dominio);

    Optional<Revendedor> findByUsuario_Id(UUID usuarioId);
}
