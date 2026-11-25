package ar.com.mercadodigital.backend.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.AdminUsuarioCreateRequest;
import ar.com.mercadodigital.backend.dto.request.AdminUsuarioUpdateRequest;
import ar.com.mercadodigital.backend.dto.response.AdminUsuarioResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RolRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUsuarioService {

    private static final Pattern DOMINIO_PATTERN = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ProveedorRepository proveedorRepository;
    private final RevendedorRepository revendedorRepository;
    private final ProveedorMercadoPagoService proveedorMercadoPagoService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUsuarioResponse crear(AdminUsuarioCreateRequest request) {
        String email = normalizarEmailObligatorio(request.getEmail());
        validarEmailDisponible(email);

        TipoUsuario tipoUsuario = request.getTipoUsuario();
        if (tipoUsuario == null) {
            throw new BadRequestException("tipoUsuario es obligatorio");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(normalizarTextoObligatorio(request.getNombre(), "nombre"));
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(normalizarTextoObligatorio(request.getPassword(), "password")));
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setActivo(Boolean.TRUE.equals(request.getActivo()));
        usuario.setRoles(new HashSet<>(java.util.Set.of(resolveRol(tipoUsuario.name()))));
        Usuario guardado = usuarioRepository.save(usuario);

        if (tipoUsuario == TipoUsuario.PROVEEDOR) {
            crearProveedor(guardado, request);
        } else if (tipoUsuario == TipoUsuario.REVENDEDOR) {
            crearRevendedor(guardado, request);
        }

        return toResponse(guardado);
    }

    @Transactional
    public AdminUsuarioResponse actualizar(UUID usuarioId, AdminUsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado: " + usuarioId));

        String email = normalizarEmailObligatorio(request.getEmail());
        validarEmailDisponibleParaOtro(email, usuarioId);

        TipoUsuario tipoUsuario = request.getTipoUsuario();
        if (tipoUsuario == null) {
            throw new BadRequestException("tipoUsuario es obligatorio");
        }

        usuario.setNombre(normalizarTextoObligatorio(request.getNombre(), "nombre"));
        usuario.setEmail(email);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setActivo(Boolean.TRUE.equals(request.getActivo()));
        usuario.setRoles(new HashSet<>(java.util.Set.of(resolveRol(tipoUsuario.name()))));
        Usuario guardado = usuarioRepository.save(usuario);

        if (tipoUsuario == TipoUsuario.PROVEEDOR) {
            upsertProveedor(guardado, request);
        } else if (tipoUsuario == TipoUsuario.REVENDEDOR) {
            upsertRevendedor(guardado, request);
        }

        return toResponse(guardado);
    }

    private void crearProveedor(Usuario usuario, AdminUsuarioCreateRequest request) {
        Proveedor proveedor = new Proveedor();
        proveedor.setUsuario(usuario);
        proveedor.setActivo(usuario.isActivo());
        proveedor.setDespachaMercaderia(Boolean.TRUE.equals(request.getProveedorDespachante()));
        proveedor.setNombre(normalizarTextoOpcional(request.getNombreEntidad()) != null
                ? normalizarTextoOpcional(request.getNombreEntidad())
                : usuario.getNombre());
        proveedorRepository.save(proveedor);
    }

    private void crearRevendedor(Usuario usuario, AdminUsuarioCreateRequest request) {
        Revendedor revendedor = new Revendedor();
        revendedor.setUsuario(usuario);
        revendedor.setActivo(usuario.isActivo());
        revendedor.setNombre(normalizarTextoOpcional(request.getNombreEntidad()) != null
                ? normalizarTextoOpcional(request.getNombreEntidad())
                : usuario.getNombre());

        String dominio = normalizarDominioOpcional(request.getDominioRevendedor());
        if (dominio != null && revendedorRepository.existsByDominioIgnoreCase(dominio)) {
            throw new OperacionEnConflictoException("Ya existe un revendedor con dominio: " + dominio);
        }
        revendedor.setDominio(dominio);
        revendedorRepository.save(revendedor);
    }

    private void upsertProveedor(Usuario usuario, AdminUsuarioUpdateRequest request) {
        Proveedor proveedor = proveedorRepository.findByUsuario_Id(usuario.getId()).orElseGet(Proveedor::new);
        proveedor.setUsuario(usuario);
        proveedor.setActivo(usuario.isActivo());
        proveedor.setDespachaMercaderia(Boolean.TRUE.equals(request.getProveedorDespachante()));
        proveedor.setNombre(normalizarTextoOpcional(request.getNombreEntidad()) != null
                ? normalizarTextoOpcional(request.getNombreEntidad())
                : usuario.getNombre());
        proveedorRepository.save(proveedor);
    }

    private void upsertRevendedor(Usuario usuario, AdminUsuarioUpdateRequest request) {
        Revendedor revendedor = revendedorRepository.findByUsuario_Id(usuario.getId()).orElseGet(Revendedor::new);
        revendedor.setUsuario(usuario);
        revendedor.setActivo(usuario.isActivo());
        revendedor.setNombre(normalizarTextoOpcional(request.getNombreEntidad()) != null
                ? normalizarTextoOpcional(request.getNombreEntidad())
                : usuario.getNombre());

        String dominio = normalizarDominioOpcional(request.getDominioRevendedor());
        if (dominio != null && revendedorRepository.existsByDominioIgnoreCase(dominio)
                && (revendedor.getDominio() == null || !dominio.equalsIgnoreCase(revendedor.getDominio()))) {
            throw new OperacionEnConflictoException("Ya existe un revendedor con dominio: " + dominio);
        }
        revendedor.setDominio(dominio);
        revendedorRepository.save(revendedor);
    }

    private Rol resolveRol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setId(UUID.randomUUID());
                    rol.setNombre(nombre);
                    return rolRepository.save(rol);
                });
    }

    private AdminUsuarioResponse toResponse(Usuario usuario) {
        UUID proveedorId = proveedorRepository.findByUsuario_Id(usuario.getId()).map(Proveedor::getId).orElse(null);
        UUID revendedorId = revendedorRepository.findByUsuario_Id(usuario.getId()).map(Revendedor::getId).orElse(null);
        AdminRevendedorMercadoPagoResponse mercadoPago = proveedorId != null
                ? proveedorMercadoPagoService.getEstado(proveedorId)
                : null;

        return AdminUsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .tipoUsuario(usuario.getTipoUsuario())
                .roles(usuario.getRoles() != null && !usuario.getRoles().isEmpty()
                        ? usuario.getRoles().stream().map(Rol::getNombre).sorted().toList()
                        : List.of(usuario.getTipoUsuario().name()))
                .activo(usuario.isActivo())
                .proveedorId(proveedorId)
                .proveedorDespachante(proveedorId != null
                        ? proveedorRepository.findByUsuario_Id(usuario.getId()).map(Proveedor::isDespachaMercaderia).orElse(false)
                        : null)
                .revendedorId(revendedorId)
                .mercadoPago(mercadoPago)
                .createdAt(usuario.getCreatedAt())
                .updatedAt(usuario.getUpdatedAt())
                .build();
    }

    private String normalizarTextoObligatorio(String valor, String campo) {
        String normalizado = normalizarTextoOpcional(valor);
        if (normalizado == null) {
            throw new BadRequestException(campo + " es obligatorio");
        }
        return normalizado;
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isBlank() ? null : normalizado;
    }

    private String normalizarEmailObligatorio(String email) {
        String normalizado = normalizarTextoObligatorio(email, "email");
        return normalizado.toLowerCase();
    }

    private void validarEmailDisponible(String email) {
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            throw new OperacionEnConflictoException("Ya existe un usuario con email: " + email);
        });
    }

    private void validarEmailDisponibleParaOtro(String email, UUID usuarioIdActual) {
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(usuario -> !usuario.getId().equals(usuarioIdActual))
                .ifPresent(usuario -> {
                    throw new OperacionEnConflictoException("Ya existe un usuario con email: " + email);
                });
    }

    private String normalizarDominioOpcional(String dominio) {
        String normalizado = normalizarTextoOpcional(dominio);
        if (normalizado == null) {
            return null;
        }

        String sinProtocolo = normalizado.toLowerCase().replaceFirst("^https?://", "");
        int slashIndex = sinProtocolo.indexOf('/');
        if (slashIndex >= 0) {
            sinProtocolo = sinProtocolo.substring(0, slashIndex);
        }
        int portIndex = sinProtocolo.indexOf(':');
        if (portIndex >= 0) {
            sinProtocolo = sinProtocolo.substring(0, portIndex);
        }
        while (sinProtocolo.endsWith(".")) {
            sinProtocolo = sinProtocolo.substring(0, sinProtocolo.length() - 1);
        }

        if (!DOMINIO_PATTERN.matcher(sinProtocolo).matches()) {
            throw new BadRequestException("dominioRevendedor invalido: " + sinProtocolo);
        }
        return sinProtocolo;
    }
}
