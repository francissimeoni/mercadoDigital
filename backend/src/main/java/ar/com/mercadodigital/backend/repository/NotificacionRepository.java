package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    @EntityGraph(attributePaths = { "usuario" })
    List<Notificacion> findByUsuario_IdOrderByCreatedAtDesc(UUID usuarioId);
}
