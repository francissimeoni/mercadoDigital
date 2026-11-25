package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ClienteRevendedor;

public interface ClienteRevendedorRepository extends JpaRepository<ClienteRevendedor, UUID> {

    @EntityGraph(attributePaths = { "usuario", "revendedor" })
    Optional<ClienteRevendedor> findByUsuario_IdAndRevendedor_Id(UUID usuarioId, UUID revendedorId);

    @EntityGraph(attributePaths = { "usuario", "revendedor" })
    List<ClienteRevendedor> findByUsuario_Id(UUID usuarioId);

    @EntityGraph(attributePaths = { "usuario", "revendedor" })
    List<ClienteRevendedor> findByRevendedor_Id(UUID revendedorId);
}
