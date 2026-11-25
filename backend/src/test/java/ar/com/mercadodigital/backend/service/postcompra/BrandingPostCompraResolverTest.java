package ar.com.mercadodigital.backend.service.postcompra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.config.PostCompraEmailProperties;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;

@ExtendWith(MockitoExtension.class)
class BrandingPostCompraResolverTest {

    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private RevendedorStorefrontRepository revendedorStorefrontRepository;

    @InjectMocks
    private BrandingPostCompraResolver resolver;

    @Test
    void usaBrandingPublicadoCuandoExiste() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setNombre("Revendedor Base");
        revendedor.setDominio("mi-tienda.com");

        RevendedorStorefront storefront = new RevendedorStorefront();
        storefront.setNombreComercial("Mi Tienda");
        storefront.setColorPrimario("#123456");
        storefront.setLogoUrl("https://cdn/logo.png");
        storefront.setEmailComercial("ventas@mi-tienda.com");
        storefront.setSmtpHost("smtp.mi-tienda.com");
        storefront.setSmtpPort(587);
        storefront.setSmtpUsername("mailer@mi-tienda.com");
        storefront.setSmtpPassword("secret");

        PostCompraEmailProperties properties = new PostCompraEmailProperties();
        properties.setFromEmail("no-reply@test.com");
        properties.setFromName("Fallback");
        properties.setBasePublicUrl("http://localhost:5173");
        resolver = new BrandingPostCompraResolver(revendedorRepository, revendedorStorefrontRepository, properties);

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(revendedorStorefrontRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.of(storefront));

        BrandingPostCompra branding = resolver.resolver(revendedorId);

        assertEquals("Mi Tienda", branding.nombreComercial());
        assertEquals("#123456", branding.colorPrimario());
        assertEquals("https://cdn/logo.png", branding.logoUrl());
        assertEquals("https://mi-tienda.com", branding.baseUrlPublica());
        assertEquals("ventas@mi-tienda.com", branding.emailComercial());
        // Production authenticates the sender with the store SMTP account.
        assertEquals("mailer@mi-tienda.com", branding.emailRemitente());
        assertEquals("Mi Tienda", branding.nombreRemitente());
        assertEquals("smtp.mi-tienda.com", branding.smtpHost());
        assertEquals(587, branding.smtpPort());
        assertEquals("mailer@mi-tienda.com", branding.smtpUsername());
        assertEquals("secret", branding.smtpPassword());
    }

    @Test
    void aplicaFallbackSeguroCuandoFaltaStorefront() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setNombre("Revendedor Fallback");

        PostCompraEmailProperties properties = new PostCompraEmailProperties();
        properties.setFromEmail("no-reply@test.com");
        properties.setFromName("Mercado Digital");
        properties.setBasePublicUrl("http://localhost:5173/");
        resolver = new BrandingPostCompraResolver(revendedorRepository, revendedorStorefrontRepository, properties);

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(revendedorStorefrontRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.empty());

        BrandingPostCompra branding = resolver.resolver(revendedorId);

        assertEquals("Revendedor Fallback", branding.nombreComercial());
        assertEquals(BrandingPostCompraResolver.COLOR_PRIMARIO_FALLBACK, branding.colorPrimario());
        assertNull(branding.logoUrl());
        assertEquals("http://localhost:5173", branding.baseUrlPublica());
        // Commercial contact stays unset when the store has not configured one.
        assertNull(branding.emailComercial());
        assertEquals("no-reply@test.com", branding.emailRemitente());
        assertEquals("Revendedor Fallback", branding.nombreRemitente());
        assertNull(branding.smtpHost());
    }
}
