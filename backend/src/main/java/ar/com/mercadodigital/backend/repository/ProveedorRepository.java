package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    @Override
    @EntityGraph(attributePaths = { "usuario" })
    Optional<Proveedor> findById(UUID id);

    @EntityGraph(attributePaths = { "usuario" })
    Optional<Proveedor> findByUsuario_Id(UUID usuarioId);
}
