package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RevendedorOwnershipServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RevendedorRepository revendedorRepository;
    @InjectMocks
    private RevendedorOwnershipService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permiteAlRevendedorDueño() {
        UUID usuarioId = UUID.randomUUID();
        UUID revendedorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        autenticar("revendedor@test.com");
        when(usuarioRepository.findByEmailIgnoreCase("revendedor@test.com")).thenReturn(Optional.of(usuario));
        when(revendedorRepository.findByUsuario_Id(usuarioId)).thenReturn(Optional.of(revendedor));

        assertDoesNotThrow(() -> service.validarOwnership(revendedorId));
    }

    @Test
    void rechazaAccesoATenantAjeno() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Revendedor otro = new Revendedor();
        otro.setId(UUID.randomUUID());
        autenticar("revendedor@test.com");
        when(usuarioRepository.findByEmailIgnoreCase("revendedor@test.com")).thenReturn(Optional.of(usuario));
        when(revendedorRepository.findByUsuario_Id(usuarioId)).thenReturn(Optional.of(otro));

        assertThrows(AccessDeniedException.class, () -> service.validarOwnership(UUID.randomUUID()));
    }

    @Test
    void permiteAlAdministradorOperarLaTiendaSeleccionada() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@mercado-digital.com.ar",
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ADMIN_MD"))));

        assertDoesNotThrow(() -> service.validarOwnership(UUID.randomUUID()));
    }

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, java.util.List.of()));
    }
}
