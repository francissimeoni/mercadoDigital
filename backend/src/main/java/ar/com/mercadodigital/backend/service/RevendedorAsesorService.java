package ar.com.mercadodigital.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.com.mercadodigital.backend.dto.request.RevendedorAsesorRequest;
import ar.com.mercadodigital.backend.dto.response.RevendedorAsesorResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorStorefrontHostResponse;
import ar.com.mercadodigital.backend.entity.AsesorVenta;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.StorefrontCredencial;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AsesorVentaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RolRepository;
import ar.com.mercadodigital.backend.repository.StorefrontCredencialRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevendedorAsesorService {

    private final RevendedorRepository revendedorRepository;
    private final AsesorVentaRepository asesorVentaRepository;
    private final RevendedorOwnershipService ownershipService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final StorefrontCredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorefrontAssetStorageService assetStorage;

    @Transactional
    public RevendedorAsesorResponse guardarConFoto(UUID tienda, UUID asesorId, RevendedorAsesorRequest request,
            org.springframework.web.multipart.MultipartFile foto, String baseUrl) {
        ownershipService.validarOwnership(tienda);
        // Se comprueba pertenencia antes de escribir el archivo.
        if (asesorId != null && asesorVentaRepository.findByIdAndRevendedor_Id(asesorId, tienda).isEmpty())
            throw new ResourceNotFoundException("Asesor de venta no encontrado");
        if (foto != null && !foto.isEmpty()) {
            request.setFotoUrl(java.net.URI.create(assetStorage.storeAdvisorTransactional(foto, tienda, baseUrl).getUrl()).getPath());
            request.setQuitarFoto(false);
        }
        return asesorId == null ? crear(tienda, request) : actualizar(tienda, asesorId, request);
    }

    @Transactional(readOnly = true)
    public List<RevendedorAsesorResponse> listar(UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        getRevendedor(revendedorId);
        return asesorVentaRepository.findByRevendedor_IdOrderByNombreAsc(revendedorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RevendedorStorefrontHostResponse getStorefrontHost(UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        Revendedor revendedor = getRevendedor(revendedorId);
        return RevendedorStorefrontHostResponse.builder()
                .revendedorId(revendedor.getId().toString())
                .storefrontHost(revendedor.getDominio())
                .build();
    }

    @Transactional
    public RevendedorAsesorResponse crear(UUID revendedorId, RevendedorAsesorRequest request) {
        ownershipService.validarOwnership(revendedorId);
        Revendedor revendedor = getRevendedor(revendedorId);
        AsesorVenta asesor = new AsesorVenta();
        asesor.setRevendedor(revendedor);
        applyChanges(asesor, request, true);
        configurarAccesoNuevo(asesor, revendedor, request);
        AsesorVenta guardado = asesorVentaRepository.save(asesor);
        crearCredencialSiCorresponde(guardado, request);
        return toResponse(guardado);
    }

    @Transactional
    public RevendedorAsesorResponse actualizar(UUID revendedorId, UUID asesorId, RevendedorAsesorRequest request) {
        ownershipService.validarOwnership(revendedorId);
        getRevendedor(revendedorId);
        AsesorVenta asesor = asesorVentaRepository.findByIdAndRevendedor_Id(asesorId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Asesor de venta no encontrado: " + asesorId));
        applyChanges(asesor, request, false);
        actualizarAcceso(asesor, request);
        AsesorVenta guardado = asesorVentaRepository.save(asesor);
        sincronizarCredencial(guardado, request);
        return toResponse(guardado);
    }

    private void applyChanges(AsesorVenta asesor, RevendedorAsesorRequest request, boolean isCreate) {
        asesor.setNombre(normalizarNombre(request.getNombre()));
        if (Boolean.TRUE.equals(request.getQuitarFoto())) {
            asesor.setFotoUrl(null);
        } else if (isCreate || request.getFotoUrl() != null) {
            String foto = normalizarTextoOpcional(request.getFotoUrl());
            if (foto != null && !foto.equals(asesor.getFotoUrl())) validarFotoPropia(asesor.getRevendedor().getId(), foto);
            asesor.setFotoUrl(foto);
        }
        if (request.getProvincia() != null) asesor.setProvincia(textoAcotado(request.getProvincia(), 80));
        if (request.getLocalidad() != null) asesor.setLocalidad(textoAcotado(request.getLocalidad(), 120));
        if (request.getZonaAtencion() != null) asesor.setZonaAtencion(textoAcotado(request.getZonaAtencion(), 200));
        // Omitir el campo conserva el contacto; una cadena vacía permite quitarlo explícitamente.
        if (request.getWhatsappComercial() != null && !java.util.Objects.equals(request.getWhatsappComercial(), asesor.getWhatsappComercial())) {
            asesor.setWhatsappComercial(WhatsAppComercial.normalizar(request.getWhatsappComercial(), request.getPaisTelefono(), request.getTelefonoMovil()));
        }
        if (request.getActivo() != null) {
            asesor.setActivo(request.getActivo());
        } else if (isCreate) {
            asesor.setActivo(true);
        }
    }

    private Revendedor getRevendedor(UUID revendedorId) {
        return revendedorRepository.findById(revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId));
    }

    private void configurarAccesoNuevo(
            AsesorVenta asesor, Revendedor revendedor, RevendedorAsesorRequest request) {
        String email = normalizarEmail(request.getEmail());
        String password = normalizarTextoOpcional(request.getPassword());
        if (email == null && password == null) {
            return;
        }
        if (email == null || password == null) {
            throw new BadRequestException("email y password deben informarse juntos para habilitar el acceso");
        }
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new OperacionEnConflictoException("Ya existe un usuario con ese email");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(asesor.getNombre());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setTipoUsuario(TipoUsuario.ASESOR_VENTA);
        usuario.setActivo(asesor.isActivo());
        usuario.setRoles(new HashSet<>(java.util.Set.of(resolveRol("ASESOR_VENTA"))));
        asesor.setUsuario(usuarioRepository.save(usuario));
    }

    private void crearCredencialSiCorresponde(AsesorVenta asesor, RevendedorAsesorRequest request) {
        if (asesor.getUsuario() == null) {
            return;
        }
        StorefrontCredencial credencial = new StorefrontCredencial();
        credencial.setRevendedor(asesor.getRevendedor());
        credencial.setUsuario(asesor.getUsuario());
        credencial.setTipoAcceso("ASESOR_VENTA");
        credencial.setEmailLogin(asesor.getUsuario().getEmail());
        credencial.setPasswordHash(asesor.getUsuario().getPasswordHash());
        credencial.setActivo(asesor.isActivo());
        credencialRepository.save(credencial);
    }

    private void actualizarAcceso(AsesorVenta asesor, RevendedorAsesorRequest request) {
        Usuario usuario = asesor.getUsuario();
        String email = normalizarEmail(request.getEmail());
        String password = normalizarTextoOpcional(request.getPassword());
        if (usuario == null) {
            if (email != null || password != null) {
                configurarAccesoNuevo(asesor, asesor.getRevendedor(), request);
            }
            return;
        }
        if (email != null && !email.equalsIgnoreCase(usuario.getEmail())) {
            usuarioRepository.findByEmailIgnoreCase(email)
                    .filter(otro -> !otro.getId().equals(usuario.getId()))
                    .ifPresent(otro -> { throw new OperacionEnConflictoException("Ya existe un usuario con ese email"); });
            usuario.setEmail(email);
        }
        if (password != null) {
            usuario.setPasswordHash(passwordEncoder.encode(password));
        }
        usuario.setNombre(asesor.getNombre());
        usuario.setActivo(asesor.isActivo());
        usuarioRepository.save(usuario);
    }

    private void sincronizarCredencial(AsesorVenta asesor, RevendedorAsesorRequest request) {
        if (asesor.getUsuario() == null) {
            return;
        }
        StorefrontCredencial credencial = credencialRepository
                .findByRevendedor_IdAndUsuario_Id(asesor.getRevendedor().getId(), asesor.getUsuario().getId())
                .orElseGet(StorefrontCredencial::new);
        credencial.setRevendedor(asesor.getRevendedor());
        credencial.setUsuario(asesor.getUsuario());
        credencial.setTipoAcceso("ASESOR_VENTA");
        credencial.setEmailLogin(asesor.getUsuario().getEmail());
        credencial.setPasswordHash(asesor.getUsuario().getPasswordHash());
        credencial.setActivo(asesor.isActivo());
        credencialRepository.save(credencial);
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

    private String normalizarEmail(String value) {
        String normalized = normalizarTextoOpcional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String textoAcotado(String value, int max) {
        String result = normalizarTextoOpcional(value);
        if (result != null && (result.length() > max || result.chars().anyMatch(Character::isISOControl)))
            throw new BadRequestException("La zona de atención contiene un valor inválido o demasiado largo.");
        return result;
    }

    private void validarFotoPropia(UUID tienda, String foto) {
        try {
            var uri = java.net.URI.create(foto);
            if (uri.isAbsolute() || uri.getHost() != null || uri.getQuery() != null || uri.getFragment() != null)
                throw new IllegalArgumentException();
            String prefix = "/public/assets/storefront/asesor-" + tienda + "-";
            if (!uri.getPath().startsWith(prefix) || !uri.getPath().substring(prefix.length()).matches("[a-f0-9-]+\\.jpg"))
                throw new IllegalArgumentException();
            String filename = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
            if (assetStorage == null || !java.nio.file.Files.isRegularFile(assetStorage.getBaseDir().resolve(filename)))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) { throw new BadRequestException("Cargá la foto desde la ficha de este asesor."); }
    }

    private String normalizarNombre(String value) {
        String normalized = normalizarTextoOpcional(value);
        if (normalized == null) {
            throw new BadRequestException("nombre es obligatorio");
        }
        if (normalized.length() > 120) {
            throw new BadRequestException("nombre no puede superar 120 caracteres");
        }
        return normalized;
    }

    private String normalizarTextoOpcional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private RevendedorAsesorResponse toResponse(AsesorVenta asesor) {
        return RevendedorAsesorResponse.builder()
                .id(asesor.getId())
                .revendedorId(asesor.getRevendedor().getId())
                .nombre(asesor.getNombre())
                .email(asesor.getUsuario() != null ? asesor.getUsuario().getEmail() : null)
                .fotoUrl(asesor.getFotoUrl())
                .provincia(asesor.getProvincia()).localidad(asesor.getLocalidad()).zonaAtencion(asesor.getZonaAtencion())
                .whatsappComercial(asesor.getWhatsappComercial())
                .activo(asesor.isActivo())
                .createdAt(asesor.getCreatedAt())
                .updatedAt(asesor.getUpdatedAt())
                .build();
    }
}
