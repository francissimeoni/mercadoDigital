package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ConfiguracionPsp;

public interface ConfiguracionPspRepository extends JpaRepository<ConfiguracionPsp, UUID> {
    Optional<ConfiguracionPsp> findByClave(String clave);
}

