package ar.com.mercadodigital.backend.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.mercadodigital.backend.dto.request.LoginRequest;
import ar.com.mercadodigital.backend.dto.response.AuthResponse;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.security.JwtService;
import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final RevendedorRepository revendedorRepository;
    private final ProveedorRepository proveedorRepository;
    private final ClienteRevendedorRepository clienteRevendedorRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = autenticarUsuario(request.getEmail(), request.getPassword());
        List<String> roles = resolverRoles(usuario);
        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(), roles);
        return AuthResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .issuedAt(LocalDateTime.now())
                .revendedorId(resolveRevendedorId(usuario.getId()))
                .proveedorId(resolveProveedorId(usuario.getId()))
                .clienteId(resolveClienteId(usuario.getId(), roles))
                .build();
    }

    Usuario autenticarUsuario(String email, String password) {
        String emailNormalizado = email != null ? email.trim() : "";
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> {
                    log.warn("Login fallido: usuario no encontrado para email={}", emailNormalizado);
                    return credencialesInvalidas();
                });
        validarUsuarioActivo(usuario);
        validarPassword(password, usuario.getPasswordHash(), usuario.getEmail());
        return usuario;
    }

    List<String> resolverRoles(Usuario usuario) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        if (usuario.getRoles() != null) {
            usuario.getRoles().stream()
                    .map(r -> r.getNombre())
                    .filter(nombre -> nombre != null && !nombre.isBlank())
                    .forEach(roles::add);
        }
        if (usuario.getTipoUsuario() != null) {
            roles.add(usuario.getTipoUsuario().name());
        }
        return List.copyOf(roles);
    }

    private void validarUsuarioActivo(Usuario usuario) {
        if (!usuario.isActivo()) {
            log.warn("Login fallido: usuario inactivo email={}", usuario.getEmail());
            throw credencialesInvalidas();
        }
    }

    private void validarPassword(String passwordPlano, String hashGuardado, String email) {
        if (hashGuardado == null || hashGuardado.isBlank() || !passwordEncoder.matches(passwordPlano, hashGuardado)) {
            log.warn("Login fallido: password invalida email={}", email);
            throw credencialesInvalidas();
        }
    }

    String resolveRevendedorId(UUID usuarioId) {
        return revendedorRepository.findByUsuario_Id(usuarioId)
                .map(revendedor -> revendedor.getId().toString())
                .orElse(null);
    }

    private String resolveProveedorId(UUID usuarioId) {
        return proveedorRepository.findByUsuario_Id(usuarioId)
                .map(proveedor -> proveedor.getId().toString())
                .orElse(null);
    }

    private String resolveClienteId(UUID usuarioId, List<String> roles) {
        if (roles.contains("CLIENTE")) {
            return usuarioId.toString();
        }
        return clienteRevendedorRepository.findByUsuario_Id(usuarioId).stream()
                .findFirst()
                .map(clienteRevendedor -> clienteRevendedor.getUsuario().getId().toString())
                .orElse(null);
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
    }
}
