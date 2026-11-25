package ar.com.mercadodigital.backend.service.postcompra;

import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompra;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.config.PostCompraEmailProperties;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BrandingPostCompraResolver {

    static final String COLOR_PRIMARIO_FALLBACK = "#0f172a";

    private final RevendedorRepository revendedorRepository;
    private final RevendedorStorefrontRepository revendedorStorefrontRepository;
    private final PostCompraEmailProperties properties;

    public BrandingPostCompra resolver(UUID revendedorId) {
        Revendedor revendedor = this.revendedorRepository.findById(revendedorId).orElse(null);
        RevendedorStorefront storefront = this.revendedorStorefrontRepository.findByRevendedor_Id(revendedorId).orElse(null);
        String nombreComercial = BrandingPostCompraResolver.firstNonBlank(storefront != null ? storefront.getNombreComercial() : null, revendedor != null ? revendedor.getNombre() : null, "Tu tienda");
        String emailComercial = BrandingPostCompraResolver.blankToNull(storefront != null ? storefront.getEmailComercial() : null);
        boolean smtpPropioConfigurado = storefront != null && StringUtils.hasText(storefront.getSmtpHost()) && StringUtils.hasText(storefront.getSmtpUsername()) && StringUtils.hasText(storefront.getSmtpPassword());
        String emailRemitente = smtpPropioConfigurado ? BrandingPostCompraResolver.firstNonBlank(storefront.getSmtpUsername(), this.properties.getFromEmail()) : this.properties.getFromEmail();
        String baseUrlPublica = this.resolverBaseUrlPublica(revendedor);
        return new BrandingPostCompra(nombreComercial, BrandingPostCompraResolver.firstNonBlank(storefront != null ? storefront.getColorPrimario() : null, COLOR_PRIMARIO_FALLBACK), this.resolverAssetUrl(storefront != null ? storefront.getLogoUrl() : null, baseUrlPublica), baseUrlPublica, emailComercial, BrandingPostCompraResolver.blankToNull(storefront != null ? storefront.getTelefonoComercial() : null), BrandingPostCompraResolver.blankToNull(storefront != null ? storefront.getDireccionComercial() : null), emailRemitente, nombreComercial, BrandingPostCompraResolver.blankToNull(storefront != null ? storefront.getSmtpHost() : null), storefront != null ? storefront.getSmtpPort() : null, BrandingPostCompraResolver.blankToNull(storefront != null ? storefront.getSmtpUsername() : null), BrandingPostCompraResolver.blankToNull(storefront != null ? storefront.getSmtpPassword() : null), storefront == null || storefront.isSmtpAuth(), storefront == null || storefront.isSmtpStartTls());
    }

    private String resolverBaseUrlPublica(Revendedor revendedor) {
        if (revendedor != null && StringUtils.hasText(revendedor.getDominio())) {
            return "https://" + normalizarHost(revendedor.getDominio());
        }
        return normalizarBaseUrl(properties.getBasePublicUrl());
    }

    static String normalizarBaseUrl(String baseUrl) {
        String candidate = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "http://localhost:5173";
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            candidate = "https://" + normalizarHost(candidate);
        }
        while (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }

    static String normalizarHost(String host) {
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
        while (normalizado.endsWith(".")) {
            normalizado = normalizado.substring(0, normalizado.length() - 1);
        }
        return normalizado;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }


    private String resolverAssetUrl(String value, String baseUrlPublica) {
        String asset = BrandingPostCompraResolver.blankToNull(value);
        if (asset == null || asset.startsWith("http://") || asset.startsWith("https://") || asset.startsWith("data:")) {
            return asset;
        }
        return baseUrlPublica + (String)(asset.startsWith("/") ? asset : "/" + asset);
    }
}
