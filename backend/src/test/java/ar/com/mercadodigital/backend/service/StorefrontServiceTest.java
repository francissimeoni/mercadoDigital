package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;

@ExtendWith(MockitoExtension.class)
class StorefrontServiceTest {
    @Mock private CatalogoModoService modoCatalogo;

    @Mock
    private RevendedorRepository revendedorRepository;

    @Mock
    private RevendedorStorefrontRepository revendedorStorefrontRepository;

    @Mock
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @Mock
    private PspConfigService pspConfigService;

    @InjectMocks
    private StorefrontService storefrontService;

    @Test
    void resuelveStorefrontPublicadoPorHost() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setDominio("mitienda.com");
        revendedor.setActivo(true);

        RevendedorStorefront storefront = new RevendedorStorefront();
        storefront.setRevendedor(revendedor);
        storefront.setNombreComercial("Mi Tienda");
        storefront.setLogoUrl("https://cdn/logo.png");
        storefront.setPublicada(true);

        when(revendedorRepository.findByDominioIgnoreCase("mitienda.com")).thenReturn(Optional.of(revendedor));
        when(revendedorStorefrontRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.of(storefront));

        StorefrontResponse response = storefrontService.resolverPorHost("https://MiTienda.com:443/catalogo");

        assertEquals(revendedorId, response.getRevendedorId());
        assertEquals("mitienda.com", response.getDominio());
        assertEquals("Mi Tienda", response.getNombreComercial());
        assertEquals("https://cdn/logo.png", response.getLogoUrl());
    }

    @Test
    void resuelveStorefrontPorDominioAlternativoSinWww() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setActivo(true);

        RevendedorStorefront storefront = new RevendedorStorefront();
        storefront.setRevendedor(revendedor);
        storefront.setTipoStorefront(TipoStorefront.REVENDEDOR);
        storefront.setDominioPublico("www.infonel.com.ar");
        storefront.setNombreComercial("INFONEL");
        storefront.setPublicada(true);

        when(revendedorStorefrontRepository.findByDominioPublicoIgnoreCase("infonel.com.ar"))
                .thenReturn(Optional.empty());
        when(revendedorStorefrontRepository.findByDominioPublicoIgnoreCase("www.infonel.com.ar"))
                .thenReturn(Optional.of(storefront));

        StorefrontResponse response = storefrontService.resolverPorHost("infonel.com.ar");

        assertEquals("INFONEL", response.getNombreComercial());
        assertEquals("www.infonel.com.ar", response.getDominio());
    }

    @Test
    void dominioInexistenteDevuelveNotFound() {
        when(revendedorRepository.findByDominioIgnoreCase("inexistente.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> storefrontService.resolverPorHost("inexistente.com"));

        assertEquals("Storefront no disponible para host: inexistente.com", ex.getMessage());
    }

    @Test
    void storefrontNoPublicadoDevuelveNotFound() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setDominio("mitienda.com");
        revendedor.setActivo(true);

        RevendedorStorefront storefront = new RevendedorStorefront();
        storefront.setRevendedor(revendedor);
        storefront.setNombreComercial("Mi Tienda");
        storefront.setPublicada(false);

        when(revendedorRepository.findByDominioIgnoreCase("mitienda.com")).thenReturn(Optional.of(revendedor));
        when(revendedorStorefrontRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.of(storefront));

        assertThrows(ResourceNotFoundException.class, () -> storefrontService.resolverPorHost("mitienda.com"));
    }

    @Test
    void revendedorInactivoDevuelveNotFound() {
        Revendedor revendedor = new Revendedor();
        revendedor.setId(UUID.randomUUID());
        revendedor.setDominio("mitienda.com");
        revendedor.setActivo(false);

        when(revendedorRepository.findByDominioIgnoreCase("mitienda.com")).thenReturn(Optional.of(revendedor));

        assertThrows(ResourceNotFoundException.class, () -> storefrontService.resolverPorHost("mitienda.com"));
    }

    @Test
    void hostVacioDevuelveBadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> storefrontService.resolverPorHost("   "));

        assertEquals("host es obligatorio", ex.getMessage());
    }
}
