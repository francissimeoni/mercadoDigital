package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.RmaAdjunto;

public interface RmaAdjuntoRepository extends JpaRepository<RmaAdjunto, UUID> {

    List<RmaAdjunto> findByRma_IdOrderByCreatedAtAsc(UUID rmaId);

    List<RmaAdjunto> findByRmaMensaje_IdOrderByCreatedAtAsc(UUID rmaMensajeId);
}
