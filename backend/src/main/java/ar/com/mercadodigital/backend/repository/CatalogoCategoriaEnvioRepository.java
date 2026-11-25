package ar.com.mercadodigital.backend.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ar.com.mercadodigital.backend.entity.CatalogoCategoriaEnvio;
public interface CatalogoCategoriaEnvioRepository extends JpaRepository<CatalogoCategoriaEnvio, UUID> {
    List<CatalogoCategoriaEnvio> findByRevendedor_IdOrderByNombreAsc(UUID revendedorId);
    Optional<CatalogoCategoriaEnvio> findByIdAndRevendedor_Id(UUID id, UUID revendedorId);
    boolean existsByRevendedor_IdAndNombreIgnoreCaseAndIdNot(UUID revendedorId, String nombre, UUID id);
}
