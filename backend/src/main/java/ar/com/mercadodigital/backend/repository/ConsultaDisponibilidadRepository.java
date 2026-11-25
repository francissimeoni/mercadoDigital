package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ConsultaDisponibilidad;

public interface ConsultaDisponibilidadRepository extends JpaRepository<ConsultaDisponibilidad, UUID> {
    List<ConsultaDisponibilidad> findByRevendedor_IdAndRevendedorProductoIsNotNullOrderByFechaConsultaDesc(UUID revendedorId);

    Optional<ConsultaDisponibilidad> findByIdAndRevendedor_IdAndRevendedorProductoIsNotNull(UUID id, UUID revendedorId);

    Optional<ConsultaDisponibilidad> findByCheckoutToken(String checkoutToken);
}
