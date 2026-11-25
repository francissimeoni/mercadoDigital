package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.AsesorVenta;

public interface AsesorVentaRepository extends JpaRepository<AsesorVenta, UUID> {
    List<AsesorVenta> findByRevendedor_IdOrderByNombreAsc(UUID revendedorId);

    Optional<AsesorVenta> findByIdAndRevendedor_Id(UUID asesorId, UUID revendedorId);

    Optional<AsesorVenta> findByUsuario_IdAndRevendedor_Id(UUID usuarioId, UUID revendedorId);
}
