package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.SplitPayoutEjecucion;

public interface SplitPayoutEjecucionRepository extends JpaRepository<SplitPayoutEjecucion, UUID> {
    Optional<SplitPayoutEjecucion> findByPedidoId(UUID pedidoId);
}
