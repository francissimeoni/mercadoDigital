package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.StorefrontCredencial;

public interface StorefrontCredencialRepository extends JpaRepository<StorefrontCredencial, UUID> {
    Optional<StorefrontCredencial> findByRevendedor_IdAndEmailLoginIgnoreCase(UUID revendedorId, String emailLogin);

    Optional<StorefrontCredencial> findByRevendedor_IdAndUsuario_Id(UUID revendedorId, UUID usuarioId);
}
