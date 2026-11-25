package ar.com.mercadodigital.backend.repository;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import ar.com.mercadodigital.backend.entity.CatalogoProductoConfiguracion;
public interface CatalogoProductoConfiguracionRepository extends JpaRepository<CatalogoProductoConfiguracion, UUID> {
    @EntityGraph(attributePaths = {"producto", "producto.revendedor", "producto.media", "producto.categoriaProducto", "categoriaEnvio", "categoriaEnvio.revendedor"})
    List<CatalogoProductoConfiguracion> findByProducto_Revendedor_IdOrderByProducto_NombreAsc(UUID revendedorId);
    @EntityGraph(attributePaths = {"producto", "categoriaEnvio"})
    Optional<CatalogoProductoConfiguracion> findByProducto_IdAndProducto_Revendedor_Id(UUID productoId, UUID revendedorId);
    boolean existsByCategoriaEnvio_Id(UUID categoriaId);
}
