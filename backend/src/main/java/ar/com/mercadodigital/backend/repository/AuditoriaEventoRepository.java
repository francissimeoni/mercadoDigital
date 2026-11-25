package ar.com.mercadodigital.backend.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.AuditoriaEvento;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, UUID> {

    boolean existsByTipoEventoAndReferenciaId(String tipoEvento, UUID referenciaId);

    Optional<AuditoriaEvento> findTopByTipoEventoAndReferenciaIdOrderByFechaEventoAsc(String tipoEvento, UUID referenciaId);

    Optional<AuditoriaEvento> findTopByTipoEventoAndReferenciaIdOrderByFechaEventoDesc(String tipoEvento, UUID referenciaId);
}
