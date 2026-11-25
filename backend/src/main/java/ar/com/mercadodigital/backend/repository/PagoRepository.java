package ar.com.mercadodigital.backend.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, UUID> {
    Optional<Pago> findByTransferenciaToken(String transferenciaToken);

}
