package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorOwnershipServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ProveedorOwnershipService proveedorOwnershipService;

    @Test
    void proveedorDuenoPuedeResolverSuPropioContexto() {
        UUID proveedorId = UUID.randomUUID();
        Proveedor proveedor = buildProveedor(proveedorId, "proveedor@test.com");
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));

        Proveedor result = proveedorOwnershipService.resolveProveedorAutorizado(
                proveedorId,
                new UsernamePasswordAuthenticationToken(
                        "proveedor@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("PROVEEDOR"))));

        assertEquals(proveedorId, result.getId());
    }

    @Test
    void proveedorNoPuedeResolverOtroProveedor() {
        UUID proveedorId = UUID.randomUUID();
        Proveedor proveedor = buildProveedor(proveedorId, "otro@test.com");
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));

        assertThrows(ResourceNotFoundException.class, () -> proveedorOwnershipService.resolveProveedorAutorizado(
                proveedorId,
                new UsernamePasswordAuthenticationToken(
                        "proveedor@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("PROVEEDOR")))));
    }

    @Test
    void adminPuedeResolverCualquierProveedor() {
        UUID proveedorId = UUID.randomUUID();
        Proveedor proveedor = buildProveedor(proveedorId, "otro@test.com");
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));

        Proveedor result = proveedorOwnershipService.resolveProveedorAutorizado(
                proveedorId,
                new UsernamePasswordAuthenticationToken(
                        "admin@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ADMIN_MD"))));

        assertEquals(proveedorId, result.getId());
    }

    private Proveedor buildProveedor(UUID proveedorId, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail(email);

        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        proveedor.setUsuario(usuario);
        return proveedor;
    }
}
