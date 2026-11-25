package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.Transportista;

public interface TransportistaRepository extends JpaRepository<Transportista, UUID> {
    Optional<Transportista> findByNombre(String nombre);
    Optional<Transportista> findByNombreIgnoreCase(String nombre);
}
