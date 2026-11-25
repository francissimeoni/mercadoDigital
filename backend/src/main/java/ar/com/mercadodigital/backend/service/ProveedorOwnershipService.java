package ar.com.mercadodigital.backend.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorOwnershipService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public Proveedor resolveProveedorAutorizado(UUID proveedorId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BadRequestException("Autenticacion proveedor requerida");
        }

        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId));

        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ADMIN_MD".equals(authority.getAuthority()))) {
            return proveedor;
        }

        String email = authentication.getName();
        if (proveedor.getUsuario() == null || proveedor.getUsuario().getEmail() == null
                || !proveedor.getUsuario().getEmail().equalsIgnoreCase(email)) {
            throw new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId);
        }

        return proveedor;
    }
}
