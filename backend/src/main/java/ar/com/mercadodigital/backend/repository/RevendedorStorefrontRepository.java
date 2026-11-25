package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.RevendedorStorefront;

public interface RevendedorStorefrontRepository extends JpaRepository<RevendedorStorefront, UUID> {

    // Serializa cambios de preparación de catálogo de una misma tienda, también la primera alta.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from RevendedorStorefront s where s.revendedor.id = :id")
    Optional<RevendedorStorefront> findForCatalogoUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    @EntityGraph(attributePaths = { "revendedor", "proveedor" })
    Optional<RevendedorStorefront> findByRevendedor_Id(UUID revendedorId);

    @EntityGraph(attributePaths = { "revendedor", "proveedor" })
    Optional<RevendedorStorefront> findByProveedor_Id(UUID proveedorId);

    @EntityGraph(attributePaths = { "revendedor", "proveedor" })
    Optional<RevendedorStorefront> findByDominioPublicoIgnoreCase(String dominioPublico);
}
