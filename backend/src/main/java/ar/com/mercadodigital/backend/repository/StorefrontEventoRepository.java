package ar.com.mercadodigital.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.mercadodigital.backend.entity.StorefrontEvento;
import ar.com.mercadodigital.backend.entity.StorefrontEventoTipo;

public interface StorefrontEventoRepository extends JpaRepository<StorefrontEvento, UUID> {

    long countByRevendedor_IdAndTipoEventoAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID revendedorId, StorefrontEventoTipo tipo, LocalDateTime desde, LocalDateTime hasta);

    long countDistinctVisitanteHashByRevendedor_IdAndTipoEventoAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID revendedorId, StorefrontEventoTipo tipo, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
            select e.rubroNombre as nombre, count(e) as total
            from StorefrontEvento e
            where e.revendedor.id = :revendedorId and e.tipoEvento = :tipo
              and e.createdAt >= :desde and e.createdAt < :hasta and e.rubroNombre is not null
            group by e.rubroNombre
            order by count(e) desc
            """)
    List<RankingRubro> rankingRubros(@Param("revendedorId") UUID revendedorId,
            @Param("tipo") StorefrontEventoTipo tipo, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta, Pageable pageable);

    @Query("""
            select e.productoId as productoId, e.productoNombre as nombre, count(e) as total
            from StorefrontEvento e
            where e.revendedor.id = :revendedorId and e.tipoEvento = :tipo
              and e.createdAt >= :desde and e.createdAt < :hasta and e.productoId is not null
            group by e.productoId, e.productoNombre
            order by count(e) desc
            """)
    List<RankingProducto> rankingProductos(@Param("revendedorId") UUID revendedorId,
            @Param("tipo") StorefrontEventoTipo tipo, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta, Pageable pageable);

    interface RankingRubro {
        String getNombre();
        long getTotal();
    }

    interface RankingProducto {
        UUID getProductoId();
        String getNombre();
        long getTotal();
    }
}
