package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceRolesTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private ClienteRevendedorRepository clienteRevendedorRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthService authService;

    @Test
    void conservaElTipoAdministradorAunqueTambienTengaRolCliente() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.ADMIN_MD);
        Rol cliente = new Rol();
        cliente.setNombre("CLIENTE");
        usuario.setRoles(new HashSet<>(List.of(cliente)));

        assertEquals(List.of("CLIENTE", "ADMIN_MD"), authService.resolverRoles(usuario));
    }
}
