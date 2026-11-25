package ar.com.mercadodigital.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ConfiguracionMercadoPagoMd;

public interface ConfiguracionMercadoPagoMdRepository extends JpaRepository<ConfiguracionMercadoPagoMd, java.util.UUID> {
    Optional<ConfiguracionMercadoPagoMd> findByClave(String clave);
}
