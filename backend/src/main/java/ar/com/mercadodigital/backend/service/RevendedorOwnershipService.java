package ar.com.mercadodigital.backend.service;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevendedorOwnershipService {

    private final UsuarioRepository usuarioRepository;
    private final RevendedorRepository revendedorRepository;

    public void validarOwnership(UUID revendedorId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Revendedor no autenticado");
        }

        boolean esAdminMd = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ADMIN_MD".equals(authority.getAuthority()));
        if (esAdminMd) {
            return;
        }

        UUID usuarioId = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .map(usuario -> usuario.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado"));

        boolean autorizado = revendedorRepository.findByUsuario_Id(usuarioId)
                .map(revendedor -> revendedor.getId().equals(revendedorId))
                .orElse(false);
        if (!autorizado) {
            throw new AccessDeniedException("El revendedor autenticado no puede operar este recurso");
        }
    }
}
