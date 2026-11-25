package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ConfiguracionCorreoMd;

public interface ConfiguracionCorreoMdRepository extends JpaRepository<ConfiguracionCorreoMd, UUID> {
    Optional<ConfiguracionCorreoMd> findByClave(String clave);
}
