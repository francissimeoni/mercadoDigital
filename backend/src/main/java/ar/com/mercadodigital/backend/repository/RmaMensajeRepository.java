package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.RmaMensaje;

public interface RmaMensajeRepository extends JpaRepository<RmaMensaje, UUID> {
    List<RmaMensaje> findByRma_IdOrderByCreatedAtAsc(UUID rmaId);
}
