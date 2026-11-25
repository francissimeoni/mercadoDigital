package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.mercadodigital.backend.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @Query("""
            select distinct u
            from Usuario u
            join u.roles r
            where u.activo = true
              and r.nombre = :rolNombre
            """)
    List<Usuario> findActivosByRolNombre(@Param("rolNombre") String rolNombre);
}
