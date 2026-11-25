package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.service.PspConfigService;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import java.math.BigDecimal;

import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontGaleriaItemResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontPortadaResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorStorefrontContenidoResponse;
import ar.com.mercadodigital.backend.dto.request.RevendedorStorefrontContenidoRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontGaleriaItemRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontPortadaRequest;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class StorefrontService {

    private final RevendedorRepository revendedorRepository;
    private final ProveedorRepository proveedorRepository;
    private final RevendedorStorefrontRepository revendedorStorefrontRepository;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final PspConfigService pspConfigService;
    private final ObjectMapper objectMapper;
    private final CatalogoModoService modoCatalogo;

    @Transactional(readOnly = true)
    public RevendedorStorefrontContenidoResponse getContenidoRevendedor(UUID revendedorId) {
        return toContenidoResponse(getStorefrontRevendedor(revendedorId));
    }

    @Transactional
    public RevendedorStorefrontContenidoResponse actualizarContenidoRevendedor(UUID revendedorId, RevendedorStorefrontContenidoRequest request) {
        RevendedorStorefront storefront = getStorefrontRevendedor(revendedorId);
        String nombreComercial = clean(request.getNombreComercial());
        if (nombreComercial == null) {
            throw new BadRequestException("El nombre comercial es obligatorio");
        }
        storefront.setNombreComercial(nombreComercial);
        storefront.setLogoUrl(clean(request.getLogoUrl()));
        storefront.setFaviconUrl(clean(request.getFaviconUrl()));
        storefront.setColorPrimario(clean(request.getColorPrimario()));
        storefront.setColorSecundario(clean(request.getColorSecundario()));
        storefront.setHeroTitulo(clean(request.getHeroTitulo()));
        storefront.setHeroSubtitulo(clean(request.getHeroSubtitulo()));
        storefront.setHeroImagenUrl(clean(request.getHeroImagenUrl()));
        storefront.setFooterTexto(clean(request.getFooterTexto()));
        storefront.setQuienesTitulo(clean(request.getQuienesTitulo()));
        storefront.setQuienesTexto(clean(request.getQuienesTexto()));
        storefront.setEmailComercial(clean(request.getEmailComercial()));
        storefront.setTelefonoComercial(clean(request.getTelefonoComercial()));
        storefront.setDireccionComercial(clean(request.getDireccionComercial()));
        storefront.setInstagramUrl(clean(request.getInstagramUrl()));
        storefront.setFacebookUrl(clean(request.getFacebookUrl()));
        storefront.setTiktokUrl(clean(request.getTiktokUrl()));
        storefront.setYoutubeUrl(clean(request.getYoutubeUrl()));
        storefront.setTransferenciaHabilitada(request.isTransferenciaHabilitada());
        storefront.setTransferenciaAlias(clean(request.getTransferenciaAlias()));
        storefront.setTransferenciaTitular(clean(request.getTransferenciaTitular()));
        if (storefront.isTransferenciaHabilitada() && storefront.getTransferenciaAlias() == null) {
            throw new BadRequestException("El alias es obligatorio para habilitar transferencias");
        }

        String smtpHost = clean(request.getSmtpHost());
        String smtpUsername = clean(request.getSmtpUsername());
        String smtpPassword = clean(request.getSmtpPassword());
        storefront.setSmtpHost(smtpHost);
        storefront.setSmtpPort(request.getSmtpPort());
        storefront.setSmtpUsername(smtpUsername);
        if (smtpHost == null && smtpUsername == null) {
            storefront.setSmtpPassword(null);
            storefront.setSmtpPort(null);
        } else if (smtpPassword != null) {
            storefront.setSmtpPassword(smtpPassword);
        }
        storefront.setSmtpAuth(request.getSmtpAuth() == null || Boolean.TRUE.equals(request.getSmtpAuth()));
        storefront.setSmtpStartTls(request.getSmtpStartTls() == null || Boolean.TRUE.equals(request.getSmtpStartTls()));
        if ((smtpHost != null || smtpUsername != null)
                && (smtpHost == null || smtpUsername == null || storefront.getSmtpPassword() == null)) {
            throw new BadRequestException("Completa servidor, usuario y clave SMTP para activar los correos");
        }

        List<StorefrontGaleriaItemResponse> galeria = request.getQuienesGaleria() == null ? List.of()
                : request.getQuienesGaleria().stream()
                        .map(item -> StorefrontGaleriaItemResponse.builder()
                                .id(item.getId() != null ? item.getId() : UUID.randomUUID())
                                .imagenUrl(clean(item.getImagenUrl()))
                                .titulo(clean(item.getTitulo()))
                                .comentario(clean(item.getComentario()))
                                .build())
                        .toList();
        try {
            storefront.setQuienesGaleriaJson(objectMapper.writeValueAsString(galeria));
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo guardar la galeria");
        }

        List<StorefrontPortadaResponse> portadas = request.getPortadas() == null ? List.of()
                : request.getPortadas().stream()
                        .map(item -> StorefrontPortadaResponse.builder()
                                .id(item.getId() != null ? item.getId() : UUID.randomUUID())
                                .imagenUrl(clean(item.getImagenUrl()))
                                .titulo(clean(item.getTitulo()))
                                .subtitulo(clean(item.getSubtitulo()))
                                .build())
                        .toList();
        try {
            storefront.setHeroPortadasJson(objectMapper.writeValueAsString(portadas));
        } catch (Exception exception) {
            throw new BadRequestException("No se pudieron guardar las portadas");
        }
        if (!portadas.isEmpty()) {
            storefront.setHeroImagenUrl(portadas.get(0).getImagenUrl());
        }
        return toContenidoResponse(revendedorStorefrontRepository.save(storefront));
    }

    @Transactional(readOnly = true)
    public StorefrontResponse resolverPorHost(String host) {
        String hostNormalizado = normalizarHost(host);

        RevendedorStorefront storefront = revendedorStorefrontRepository.findByDominioPublicoIgnoreCase(hostNormalizado)
                .or(() -> buscarStorefrontPorHostAlternativo(hostNormalizado))
                .orElseGet(() -> {
                    Revendedor revendedorLegacy = revendedorRepository.findByDominioIgnoreCase(hostNormalizado)
                            .or(() -> buscarRevendedorPorHostAlternativo(hostNormalizado))
                            .orElseThrow(() -> storefrontNoDisponible(hostNormalizado));
                    return validarDisponibilidadPublicaRevendedor(revendedorLegacy, hostNormalizado);
                });

        if (TipoStorefront.PROVEEDOR.equals(storefront.getTipoStorefront())) {
            return toResponseProveedor(storefront);
        }
        return toResponseRevendedor(storefront);
    }

    @Transactional(readOnly = true)
    public void validarRevendedorPublico(UUID revendedorId) {
        Revendedor revendedor = revendedorRepository.findById(revendedorId)
                .orElseThrow(() -> storefrontNoDisponible(revendedorId.toString()));

        validarDisponibilidadPublicaRevendedor(revendedor, revendedorId.toString());
    }

    @Transactional(readOnly = true)
    public void validarProveedorPublico(UUID proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> storefrontNoDisponible(proveedorId.toString()));

        if (!proveedor.isActivo()) {
            throw storefrontNoDisponible(proveedorId.toString());
        }

        RevendedorStorefront storefront = revendedorStorefrontRepository.findByProveedor_Id(proveedorId)
                .orElseThrow(() -> storefrontNoDisponible(proveedorId.toString()));

        if (!storefront.isPublicada() || !TipoStorefront.PROVEEDOR.equals(storefront.getTipoStorefront())) {
            throw storefrontNoDisponible(proveedorId.toString());
        }
    }

    static String normalizarHost(String host) {
        if (host == null || host.isBlank()) {
            throw new BadRequestException("host es obligatorio");
        }

        String normalizado = host.trim().toLowerCase(Locale.ROOT);

        if (normalizado.startsWith("http://")) {
            normalizado = normalizado.substring("http://".length());
        } else if (normalizado.startsWith("https://")) {
            normalizado = normalizado.substring("https://".length());
        }

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
            throw new BadRequestException("host es obligatorio");
        }

        return normalizado;
    }

    static String hostAlternativoWww(String hostNormalizado) {
        if (hostNormalizado == null || hostNormalizado.isBlank()) {
            return null;
        }

        if (hostNormalizado.startsWith("www.")) {
            String sinWww = hostNormalizado.substring("www.".length());
            return sinWww.isBlank() ? null : sinWww;
        }

        return "www." + hostNormalizado;
    }

    private Optional<RevendedorStorefront> buscarStorefrontPorHostAlternativo(String hostNormalizado) {
        String hostAlternativo = hostAlternativoWww(hostNormalizado);
        if (hostAlternativo == null) {
            return Optional.empty();
        }

        return revendedorStorefrontRepository.findByDominioPublicoIgnoreCase(hostAlternativo);
    }

    private Optional<Revendedor> buscarRevendedorPorHostAlternativo(String hostNormalizado) {
        String hostAlternativo = hostAlternativoWww(hostNormalizado);
        if (hostAlternativo == null) {
            return Optional.empty();
        }

        return revendedorRepository.findByDominioIgnoreCase(hostAlternativo);
    }

    private ResourceNotFoundException storefrontNoDisponible(String hostNormalizado) {
        return new ResourceNotFoundException("Storefront no disponible para host: " + hostNormalizado);
    }

    private RevendedorStorefront validarDisponibilidadPublicaRevendedor(Revendedor revendedor, String identificadorPublico) {
        if (!revendedor.isActivo()) {
            throw storefrontNoDisponible(identificadorPublico);
        }

        RevendedorStorefront storefront = revendedorStorefrontRepository.findByRevendedor_Id(revendedor.getId())
                .orElseThrow(() -> storefrontNoDisponible(identificadorPublico));

        if (!storefront.isPublicada() || !TipoStorefront.REVENDEDOR.equals(storefront.getTipoStorefront())) {
            throw storefrontNoDisponible(identificadorPublico);
        }

        return storefront;
    }

    private StorefrontResponse toResponseRevendedor(RevendedorStorefront storefront) {
        Revendedor revendedor = storefront.getRevendedor();
        if (revendedor == null) {
            throw this.storefrontNoDisponible(storefront.getDominioPublico());
        }
        RevendedorMercadoPagoService.RevendedorMercadoPagoResolution mercadoPago = this.revendedorMercadoPagoService.resolveForRevendedor(revendedor.getId());
        boolean catalogoActivo = modoCatalogo.activo(revendedor.getId());
        boolean mercadoPagoHabilitado = !catalogoActivo && mercadoPago != null && mercadoPago.readyForCheckout();
        return StorefrontResponse.builder().tipoStorefront(TipoStorefront.REVENDEDOR).modoCatalogo(catalogoActivo).revendedorId(revendedor.getId()).proveedorId(null).dominio(storefront.getDominioPublico() != null ? storefront.getDominioPublico() : revendedor.getDominio()).nombreComercial(storefront.getNombreComercial()).logoUrl(storefront.getLogoUrl()).faviconUrl(storefront.getFaviconUrl()).colorPrimario(storefront.getColorPrimario()).colorSecundario(storefront.getColorSecundario()).heroTitulo(storefront.getHeroTitulo()).heroSubtitulo(storefront.getHeroSubtitulo()).heroImagenUrl(storefront.getHeroImagenUrl()).portadas(this.readPortadas(storefront.getHeroPortadasJson())).emailComercial(storefront.getEmailComercial()).telefonoComercial(storefront.getTelefonoComercial()).direccionComercial(storefront.getDireccionComercial()).footerTexto(storefront.getFooterTexto()).quienesTitulo(storefront.getQuienesTitulo()).quienesTexto(storefront.getQuienesTexto()).quienesGaleria(this.readGaleria(storefront.getQuienesGaleriaJson())).instagramUrl(storefront.getInstagramUrl()).facebookUrl(storefront.getFacebookUrl()).tiktokUrl(storefront.getTiktokUrl()).youtubeUrl(storefront.getYoutubeUrl()).transferenciaHabilitada(storefront.isTransferenciaHabilitada()).transferenciaAlias(storefront.getTransferenciaAlias()).transferenciaTitular(storefront.getTransferenciaTitular()).mercadoPagoHabilitado(mercadoPagoHabilitado).mercadoPagoRecargoPorcentaje(mercadoPagoHabilitado ? this.pspConfigService.getPspFeeFactor().multiply(BigDecimal.valueOf(100L)) : BigDecimal.ZERO).publicada(storefront.isPublicada()).build();
    }

    private StorefrontResponse toResponseProveedor(RevendedorStorefront storefront) {
        Proveedor proveedor = storefront.getProveedor();
        if (proveedor == null) {
            throw this.storefrontNoDisponible(storefront.getDominioPublico());
        }
        return StorefrontResponse.builder().tipoStorefront(TipoStorefront.PROVEEDOR).revendedorId(storefront.getRevendedor() != null ? storefront.getRevendedor().getId() : null).proveedorId(proveedor.getId()).dominio(storefront.getDominioPublico()).nombreComercial(storefront.getNombreComercial()).logoUrl(storefront.getLogoUrl()).faviconUrl(storefront.getFaviconUrl()).colorPrimario(storefront.getColorPrimario()).colorSecundario(storefront.getColorSecundario()).heroTitulo(storefront.getHeroTitulo()).heroSubtitulo(storefront.getHeroSubtitulo()).heroImagenUrl(storefront.getHeroImagenUrl()).portadas(this.readPortadas(storefront.getHeroPortadasJson())).emailComercial(storefront.getEmailComercial()).telefonoComercial(storefront.getTelefonoComercial()).direccionComercial(storefront.getDireccionComercial()).footerTexto(storefront.getFooterTexto()).quienesTitulo(storefront.getQuienesTitulo()).quienesTexto(storefront.getQuienesTexto()).quienesGaleria(this.readGaleria(storefront.getQuienesGaleriaJson())).instagramUrl(storefront.getInstagramUrl()).facebookUrl(storefront.getFacebookUrl()).tiktokUrl(storefront.getTiktokUrl()).youtubeUrl(storefront.getYoutubeUrl()).transferenciaHabilitada(false).transferenciaAlias(null).transferenciaTitular(null).mercadoPagoHabilitado(false).mercadoPagoRecargoPorcentaje(BigDecimal.ZERO).publicada(storefront.isPublicada()).build();
    }

    private RevendedorStorefrontContenidoResponse toContenidoResponse(RevendedorStorefront storefront) {
        return RevendedorStorefrontContenidoResponse.builder()
                .nombreComercial(storefront.getNombreComercial())
                .logoUrl(storefront.getLogoUrl())
                .faviconUrl(storefront.getFaviconUrl())
                .colorPrimario(storefront.getColorPrimario())
                .colorSecundario(storefront.getColorSecundario())
                .heroTitulo(storefront.getHeroTitulo())
                .heroSubtitulo(storefront.getHeroSubtitulo())
                .heroImagenUrl(storefront.getHeroImagenUrl())
                .portadas(readPortadas(storefront.getHeroPortadasJson()))
                .footerTexto(storefront.getFooterTexto())
                .quienesTitulo(storefront.getQuienesTitulo())
                .quienesTexto(storefront.getQuienesTexto())
                .quienesGaleria(readGaleria(storefront.getQuienesGaleriaJson()))
                .emailComercial(storefront.getEmailComercial())
                .telefonoComercial(storefront.getTelefonoComercial())
                .direccionComercial(storefront.getDireccionComercial())
                .instagramUrl(storefront.getInstagramUrl())
                .facebookUrl(storefront.getFacebookUrl())
                .tiktokUrl(storefront.getTiktokUrl())
                .youtubeUrl(storefront.getYoutubeUrl())
                .transferenciaHabilitada(storefront.isTransferenciaHabilitada())
                .transferenciaAlias(storefront.getTransferenciaAlias())
                .transferenciaTitular(storefront.getTransferenciaTitular())
                .smtpHost(storefront.getSmtpHost())
                .smtpPort(storefront.getSmtpPort())
                .smtpUsername(storefront.getSmtpUsername())
                .smtpConfigured(storefront.getSmtpHost() != null && storefront.getSmtpUsername() != null
                        && storefront.getSmtpPassword() != null)
                .smtpAuth(storefront.isSmtpAuth())
                .smtpStartTls(storefront.isSmtpStartTls())
                .build();
    }

    private RevendedorStorefront getStorefrontRevendedor(UUID revendedorId) {
        RevendedorStorefront storefront = revendedorStorefrontRepository.findByRevendedor_Id(revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront no encontrado"));
        if (!TipoStorefront.REVENDEDOR.equals(storefront.getTipoStorefront())) {
            throw new BadRequestException("El storefront no pertenece a un revendedor");
        }
        return storefront;
    }

    private List<StorefrontGaleriaItemResponse> readGaleria(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<StorefrontGaleriaItemRequest> items = objectMapper.readValue(
                    json, new TypeReference<List<StorefrontGaleriaItemRequest>>() { });
            return items.stream()
                    .map(item -> StorefrontGaleriaItemResponse.builder()
                            .id(item.getId())
                            .imagenUrl(item.getImagenUrl())
                            .titulo(item.getTitulo())
                            .comentario(item.getComentario())
                            .build())
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<StorefrontPortadaResponse> readPortadas(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<StorefrontPortadaRequest> items = objectMapper.readValue(
                    json, new TypeReference<List<StorefrontPortadaRequest>>() { });
            return items.stream()
                    .map(item -> StorefrontPortadaResponse.builder()
                            .id(item.getId())
                            .imagenUrl(item.getImagenUrl())
                            .titulo(item.getTitulo())
                            .subtitulo(item.getSubtitulo())
                            .build())
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }


    static boolean hostsEquivalentes(String left, String right) {
        String rightNormalizado;
        String leftNormalizado = StorefrontService.normalizarHost(left);
        return leftNormalizado.equals(rightNormalizado = StorefrontService.normalizarHost(right)) || rightNormalizado.equals(StorefrontService.hostAlternativoWww(leftNormalizado));
    }
}
