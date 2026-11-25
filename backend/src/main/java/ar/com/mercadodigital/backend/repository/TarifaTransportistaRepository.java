package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.TarifaTransportista;

public interface TarifaTransportistaRepository extends JpaRepository<TarifaTransportista, UUID> {
    Optional<TarifaTransportista> findFirstByCategoriaPaqueteOrderByDemoraEstimadaDiasAsc(String categoriaPaquete);
}
