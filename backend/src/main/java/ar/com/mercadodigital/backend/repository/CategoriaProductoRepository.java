package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.CategoriaProducto;

public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, UUID> {

    List<CategoriaProducto> findByActivaTrueOrderByNombreAsc();

    List<CategoriaProducto> findAllByOrderByActivaDescNombreAsc();

    Optional<CategoriaProducto> findBySlug(String slug);
}
