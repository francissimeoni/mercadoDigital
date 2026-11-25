package ar.com.mercadodigital.backend.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ar.com.mercadodigital.backend.entity.CatalogoConfiguracion;
public interface CatalogoConfiguracionRepository extends JpaRepository<CatalogoConfiguracion, UUID> {
    Optional<CatalogoConfiguracion> findByRevendedor_Id(UUID revendedorId);
}
