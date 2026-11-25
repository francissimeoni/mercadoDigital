package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ConfiguracionCostoMdCategoria;

public interface ConfiguracionCostoMdCategoriaRepository extends JpaRepository<ConfiguracionCostoMdCategoria, UUID> {

    Optional<ConfiguracionCostoMdCategoria> findByCategoriaPaquete(String categoriaPaquete);
}
