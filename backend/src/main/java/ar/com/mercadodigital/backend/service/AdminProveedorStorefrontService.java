package ar.com.mercadodigital.backend.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.AdminStorefrontRequest;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontResponse;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminProveedorStorefrontService {

    private final ProveedorRepository proveedorRepository;
    private final RevendedorRepository revendedorRepository;
    private final RevendedorStorefrontRepository revendedorStorefrontRepository;

    @Transactional(readOnly = true)
    public AdminStorefrontResponse getStorefront(UUID proveedorId) {
        Proveedor proveedor = getProveedor(proveedorId);
        RevendedorStorefront storefront = revendedorStorefrontRepository.findByProveedor_Id(proveedorId)
                .orElse(null);
        return toResponse(storefront, proveedor);
    }

    @Transactional
    public AdminStorefrontResponse upsertStorefront(UUID proveedorId, AdminStorefrontRequest request) {
        Proveedor proveedor = getProveedor(proveedorId);
        String dominioPublico = normalizarDominioObligatorio(request.getDominioPublico());

        revendedorStorefrontRepository.findByDominioPublicoIgnoreCase(dominioPublico)
                .filter(existing -> existing.getProveedor() == null || !existing.getProveedor().getId().equals(proveedorId))
                .ifPresent(existing -> {
                    throw new OperacionEnConflictoException("Ya existe un storefront con dominio: " + dominioPublico);
                });

        RevendedorStorefront storefront = revendedorStorefrontRepository.findByProveedor_Id(proveedorId)
                .orElseGet(RevendedorStorefront::new);
        Revendedor revendedorShadow = resolveOrCreateRevendedorShadow(proveedor, storefront.getRevendedor());

        storefront.setTipoStorefront(TipoStorefront.PROVEEDOR);
        storefront.setProveedor(proveedor);
        storefront.setRevendedor(revendedorShadow);
        storefront.setDominioPublico(dominioPublico);
        storefront.setNombreComercial(normalizarTextoObligatorio(request.getNombreComercial(), "nombreComercial"));
        storefront.setLogoUrl(normalizarTextoOpcional(request.getLogoUrl()));
        storefront.setFaviconUrl(normalizarTextoOpcional(request.getFaviconUrl()));
        storefront.setColorPrimario(normalizarTextoOpcional(request.getColorPrimario()));
        storefront.setColorSecundario(normalizarTextoOpcional(request.getColorSecundario()));
        storefront.setHeroTitulo(normalizarTextoOpcional(request.getHeroTitulo()));
        storefront.setHeroSubtitulo(normalizarTextoOpcional(request.getHeroSubtitulo()));
        storefront.setHeroImagenUrl(normalizarTextoOpcional(request.getHeroImagenUrl()));
        storefront.setEmailComercial(normalizarTextoOpcional(request.getEmailComercial()));
        storefront.setTelefonoComercial(normalizarTextoOpcional(request.getTelefonoComercial()));
        storefront.setDireccionComercial(normalizarTextoOpcional(request.getDireccionComercial()));
        storefront.setFooterTexto(normalizarTextoOpcional(request.getFooterTexto()));
        storefront.setSmtpHost(normalizarTextoOpcional(request.getSmtpHost()));
        storefront.setSmtpPort(normalizarPuertoOpcional(request.getSmtpPort()));
        storefront.setSmtpUsername(normalizarTextoOpcional(request.getSmtpUsername()));
        String smtpPassword = normalizarTextoOpcional(request.getSmtpPassword());
        if (smtpPassword != null) {
            storefront.setSmtpPassword(smtpPassword);
        } else if (storefront.getSmtpHost() == null && storefront.getSmtpUsername() == null) {
            storefront.setSmtpPassword(null);
        }
        storefront.setSmtpAuth(request.getSmtpAuth() == null || Boolean.TRUE.equals(request.getSmtpAuth()));
        storefront.setSmtpStartTls(request.getSmtpStartTls() == null || Boolean.TRUE.equals(request.getSmtpStartTls()));
        storefront.setPublicada(Boolean.TRUE.equals(request.getPublicada()));

        RevendedorStorefront saved = revendedorStorefrontRepository.save(storefront);
        return toResponse(saved, proveedor);
    }

    private Proveedor getProveedor(UUID proveedorId) {
        return proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId));
    }

    private AdminStorefrontResponse toResponse(RevendedorStorefront storefront, Proveedor proveedor) {
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
                .createdAt(storefront.getCreatedAt())
                .updatedAt(storefront.getUpdatedAt())
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

    private String normalizarDominioObligatorio(String dominio) {
        String normalizado = normalizarTextoObligatorio(dominio, "dominioPublico")
                .toLowerCase(Locale.ROOT)
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
        if (normalizado.isBlank()) {
            throw new BadRequestException("dominioPublico es obligatorio");
        }
        return normalizado;
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

    private Revendedor resolveOrCreateRevendedorShadow(Proveedor proveedor, Revendedor current) {
        if (current != null) {
            return current;
        }

        Revendedor revendedor = new Revendedor();
        revendedor.setNombre(proveedor.getNombre());
        revendedor.setActivo(true);
        revendedor.setUsuario(proveedor.getUsuario());
        revendedor.setDominio(generarDominioShadow(proveedor.getId()));
        return revendedorRepository.save(revendedor);
    }

    private String generarDominioShadow(UUID proveedorId) {
        String base = "shadow-prov-" + proveedorId.toString().substring(0, 8) + ".md.local";
        if (!revendedorRepository.existsByDominioIgnoreCase(base)) {
            return base;
        }
        return "shadow-prov-" + proveedorId + ".md.local";
    }
}
