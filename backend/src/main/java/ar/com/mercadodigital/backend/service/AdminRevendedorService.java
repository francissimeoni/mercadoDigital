package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import java.util.List;
import java.util.Set;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.AdminRevendedorRequest;
import ar.com.mercadodigital.backend.dto.request.AdminStorefrontRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorResponse;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorMercadoPagoAccount;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import ar.com.mercadodigital.backend.repository.RolRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRevendedorService {

    private static final Pattern DOMINIO_PATTERN = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$");

    private final RevendedorRepository revendedorRepository;
    private final RevendedorStorefrontRepository revendedorStorefrontRepository;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public java.util.List<AdminRevendedorResponse> getRevendedores() {
        return revendedorRepository.findAll().stream()
                .sorted(Comparator.comparing(Revendedor::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRevendedorResponse getRevendedor(UUID id) {
        return toResponse(getRevendedorEntity(id));
    }

    @Transactional
    public AdminRevendedorResponse crear(AdminRevendedorRequest request) {
        String dominioNormalizado = normalizarDominio(request.getDominio());
        validarDominioDisponible(dominioNormalizado, null);

        Revendedor revendedor = new Revendedor();
        revendedor.setNombre(normalizarTextoObligatorio(request.getNombre(), "nombre"));
        revendedor.setDominio(dominioNormalizado);
        revendedor.setActivo(Boolean.TRUE.equals(request.getActivo()));
        asegurarUsuarioRevendedor(revendedor, request, true);

        return toResponse(revendedorRepository.save(revendedor));
    }

    @Transactional
    public AdminRevendedorResponse actualizar(UUID id, AdminRevendedorRequest request) {
        Revendedor revendedor = getRevendedorEntity(id);
        String dominioNormalizado = normalizarDominio(request.getDominio());
        validarDominioDisponible(dominioNormalizado, id);

        revendedor.setNombre(normalizarTextoObligatorio(request.getNombre(), "nombre"));
        revendedor.setDominio(dominioNormalizado);
        revendedor.setActivo(Boolean.TRUE.equals(request.getActivo()));
        asegurarUsuarioRevendedor(revendedor, request, false);

        if (!revendedor.isActivo() && revendedor.getStorefront() != null && revendedor.getStorefront().isPublicada()) {
            throw new BadRequestException("No se puede desactivar un revendedor con storefront publicada");
        }

        return toResponse(revendedorRepository.save(revendedor));
    }

    @Transactional
    public AdminRevendedorResponse upsertStorefront(UUID revendedorId, AdminStorefrontRequest request) {
        Revendedor revendedor = this.getRevendedorEntity(revendedorId);
        if (Boolean.TRUE.equals(request.getPublicada()) && !revendedor.isActivo()) {
            throw new BadRequestException("No se puede publicar storefront para un revendedor inactivo");
        }
        Optional<RevendedorStorefront> storefrontExistente = this.revendedorStorefrontRepository.findByRevendedor_Id(revendedorId);
        RevendedorStorefront storefront = storefrontExistente.orElseGet(() -> {
            RevendedorStorefront nueva = new RevendedorStorefront();
            nueva.setRevendedor(revendedor);
            return nueva;
        });
        storefront.setNombreComercial(this.normalizarTextoObligatorio(request.getNombreComercial(), "nombreComercial"));
        storefront.setTipoStorefront(TipoStorefront.REVENDEDOR);
        storefront.setProveedor(null);
        storefront.setDominioPublico(revendedor.getDominio());
        storefront.setLogoUrl(this.normalizarTextoOpcional(request.getLogoUrl()));
        storefront.setFaviconUrl(this.normalizarTextoOpcional(request.getFaviconUrl()));
        storefront.setColorPrimario(this.normalizarTextoOpcional(request.getColorPrimario()));
        storefront.setColorSecundario(this.normalizarTextoOpcional(request.getColorSecundario()));
        storefront.setHeroTitulo(this.normalizarTextoOpcional(request.getHeroTitulo()));
        storefront.setHeroSubtitulo(this.normalizarTextoOpcional(request.getHeroSubtitulo()));
        storefront.setHeroImagenUrl(this.normalizarTextoOpcional(request.getHeroImagenUrl()));
        storefront.setTelefonoComercial(this.normalizarTextoOpcional(request.getTelefonoComercial()));
        storefront.setDireccionComercial(this.normalizarTextoOpcional(request.getDireccionComercial()));
        storefront.setFooterTexto(this.normalizarTextoOpcional(request.getFooterTexto()));
        if (storefrontExistente.isEmpty()) {
            storefront.setEmailComercial(this.normalizarTextoOpcional(request.getEmailComercial()));
            storefront.setSmtpHost(this.normalizarTextoOpcional(request.getSmtpHost()));
            storefront.setSmtpPort(this.normalizarPuertoOpcional(request.getSmtpPort()));
            storefront.setSmtpUsername(this.normalizarTextoOpcional(request.getSmtpUsername()));
            storefront.setSmtpPassword(this.normalizarTextoOpcional(request.getSmtpPassword()));
            storefront.setSmtpAuth(request.getSmtpAuth() == null || Boolean.TRUE.equals(request.getSmtpAuth()));
            storefront.setSmtpStartTls(request.getSmtpStartTls() == null || Boolean.TRUE.equals(request.getSmtpStartTls()));
        }
        storefront.setPublicada(Boolean.TRUE.equals(request.getPublicada()));
        if (request.getSincronizacionPausada() != null && storefront.isSincronizacionPausada() != request.getSincronizacionPausada().booleanValue()) {
            boolean pausada = Boolean.TRUE.equals(request.getSincronizacionPausada());
            storefront.setSincronizacionPausada(pausada);
            storefront.setSincronizacionPausadaPor(pausada ? this.getActorActual() : null);
            storefront.setSincronizacionPausadaAt(pausada ? LocalDateTime.now() : null);
        }
        this.revendedorStorefrontRepository.save(storefront);
        revendedor.setStorefront(storefront);
        return this.toResponse(revendedor);
    }

    private Revendedor getRevendedorEntity(UUID id) {
        return revendedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + id));
    }

    private void validarDominioDisponible(String dominioNormalizado, UUID revendedorIdActual) {
        Optional<Revendedor> existente = revendedorRepository.findByDominioIgnoreCase(dominioNormalizado);
        if (existente.isPresent() && !existente.get().getId().equals(revendedorIdActual)) {
            throw new OperacionEnConflictoException("Ya existe un revendedor con dominio: " + dominioNormalizado);
        }
    }

    private String normalizarDominio(String dominio) {
        String normalizado = normalizarTextoObligatorio(dominio, "dominio")
                .toLowerCase()
                .replaceFirst("^https?://", "");

        int slashIndex = normalizado.indexOf('/');
        if (slashIndex >= 0) {
            normalizado = normalizado.substring(0, slashIndex);
        }

        int portIndex = normalizado.indexOf(':');
        if (portIndex >= 0) {
            normalizado = normalizado.substring(0, portIndex);
        }

        while (normalizado.endsWith(".")) {
            normalizado = normalizado.substring(0, normalizado.length() - 1);
        }

        if (!DOMINIO_PATTERN.matcher(normalizado).matches()) {
            throw new BadRequestException("dominio invalido: " + normalizado);
        }
        return normalizado;
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

    private Integer normalizarPuertoOpcional(Integer valor) {
        if (valor == null) {
            return null;
        }
        if (valor <= 0 || valor > 65535) {
            throw new BadRequestException("smtpPort invalido: " + valor);
        }
        return valor;
    }

    private void asegurarUsuarioRevendedor(Revendedor revendedor, AdminRevendedorRequest request, boolean creating) {
        String emailNormalizado = normalizarEmailOpcional(request.getUsuarioEmail());
        String passwordNormalizada = normalizarTextoOpcional(request.getUsuarioPassword());
        Usuario usuarioExistente = revendedor.getUsuario();

        if (emailNormalizado == null && usuarioExistente == null && passwordNormalizada == null) {
            return;
        }

        if (emailNormalizado == null && passwordNormalizada != null && usuarioExistente == null) {
            throw new BadRequestException("usuarioEmail es obligatorio cuando se define usuarioPassword");
        }

        Usuario usuario = usuarioExistente;
        if (usuario == null) {
            if (emailNormalizado == null) {
                return;
            }
            if (passwordNormalizada == null) {
                throw new BadRequestException("usuarioPassword es obligatorio para crear el usuario del revendedor");
            }
            usuario = new Usuario();
        }

        if (emailNormalizado != null) {
            validarEmailUsuarioDisponible(emailNormalizado, usuario.getId());
            usuario.setEmail(emailNormalizado);
        } else if (creating) {
            throw new BadRequestException("usuarioEmail es obligatorio para crear el usuario del revendedor");
        }

        if (passwordNormalizada != null) {
            usuario.setPasswordHash(passwordEncoder.encode(passwordNormalizada));
        } else if (creating && (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank())) {
            throw new BadRequestException("usuarioPassword es obligatorio para crear el usuario del revendedor");
        }

        usuario.setNombre(normalizarTextoObligatorio(request.getNombre(), "nombre"));
        usuario.setTipoUsuario(TipoUsuario.REVENDEDOR);
        usuario.setActivo(Boolean.TRUE.equals(request.getActivo()));
        usuario.setRoles(new HashSet<>(java.util.Set.of(resolveRolRevendedor())));

        revendedor.setUsuario(usuarioRepository.save(usuario));
    }

    private void validarEmailUsuarioDisponible(String emailNormalizado, UUID usuarioIdActual) {
        usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .filter(usuario -> !usuario.getId().equals(usuarioIdActual))
                .ifPresent(usuario -> {
                    throw new OperacionEnConflictoException("Ya existe un usuario con email: " + emailNormalizado);
                });
    }

    private String normalizarEmailOpcional(String email) {
        String normalizado = normalizarTextoOpcional(email);
        return normalizado != null ? normalizado.toLowerCase() : null;
    }

    private Rol resolveRolRevendedor() {
        return rolRepository.findByNombre("REVENDEDOR")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setId(UUID.randomUUID());
                    rol.setNombre("REVENDEDOR");
                    return rolRepository.save(rol);
                });
    }

    private AdminRevendedorResponse toResponse(Revendedor revendedor) {
        return AdminRevendedorResponse.builder()
                .id(revendedor.getId())
                .nombre(revendedor.getNombre())
                .dominio(revendedor.getDominio())
                .activo(revendedor.isActivo())
                .usuarioEmail(revendedor.getUsuario() != null ? revendedor.getUsuario().getEmail() : null)
                .usuarioConfigurado(revendedor.getUsuario() != null
                        && revendedor.getUsuario().getPasswordHash() != null
                        && !revendedor.getUsuario().getPasswordHash().isBlank())
                .createdAt(revendedor.getCreatedAt())
                .updatedAt(revendedor.getUpdatedAt())
                .storefront(toStorefrontResponse(revendedor.getStorefront()))
                .mercadoPago(toMercadoPagoResponse(revendedor.getMercadoPagoAccount()))
                .build();
    }

    private ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse toMercadoPagoResponse(
            RevendedorMercadoPagoAccount account) {
        return revendedorMercadoPagoService.toResponse(account);
    }

    private AdminStorefrontResponse toStorefrontResponse(RevendedorStorefront storefront) {
        if (storefront == null) {
            return null;
        }
        return AdminStorefrontResponse.builder()
                .id(storefront.getId())
                .tipoStorefront(storefront.getTipoStorefront())
                .dominioPublico(storefront.getDominioPublico())
                .nombreComercial(storefront.getNombreComercial())
                .logoUrl(storefront.getLogoUrl())
                .faviconUrl(storefront.getFaviconUrl())
                .colorPrimario(storefront.getColorPrimario())
                .colorSecundario(storefront.getColorSecundario())
                .heroTitulo(storefront.getHeroTitulo())
                .heroSubtitulo(storefront.getHeroSubtitulo())
                .heroImagenUrl(storefront.getHeroImagenUrl())
                .emailComercial(storefront.getEmailComercial())
                .telefonoComercial(storefront.getTelefonoComercial())
                .direccionComercial(storefront.getDireccionComercial())
                .footerTexto(storefront.getFooterTexto())
                .smtpHost(storefront.getSmtpHost())
                .smtpPort(storefront.getSmtpPort())
                .smtpUsername(storefront.getSmtpUsername())
                .smtpConfigured(storefront.getSmtpHost() != null && storefront.getSmtpUsername() != null
                        && storefront.getSmtpPassword() != null)
                .smtpAuth(storefront.isSmtpAuth())
                .smtpStartTls(storefront.isSmtpStartTls())
                .publicada(storefront.isPublicada())
                .sincronizacionPausada(storefront.isSincronizacionPausada())
                .sincronizacionPausadaPor(storefront.getSincronizacionPausadaPor())
                .sincronizacionPausadaAt(storefront.getSincronizacionPausadaAt())
                .createdAt(storefront.getCreatedAt())
                .updatedAt(storefront.getUpdatedAt())
                .build();
    }

    private String getActorActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "ADMIN_MD";
    }
}
