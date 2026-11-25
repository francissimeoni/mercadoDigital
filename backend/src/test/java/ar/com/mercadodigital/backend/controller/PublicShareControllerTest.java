package ar.com.mercadodigital.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.service.PublicCatalogoService;
import ar.com.mercadodigital.backend.service.StorefrontService;

@ExtendWith(MockitoExtension.class)
class PublicShareControllerTest {

    @Mock
    private StorefrontService storefrontService;
    @Mock
    private PublicCatalogoService publicCatalogoService;
    @InjectMocks
    private PublicShareController controller;

    @Test
    void runtimePuedeLeerImagenesWebpParaLaMetadata() {
        assertTrue(ImageIO.getImageReadersByFormatName("webp").hasNext());
    }

    @Test
    void catalogoCompartePrecioFinalSinInventarPlazos() {
        UUID tienda = UUID.randomUUID(), producto = UUID.randomUUID();
        when(storefrontService.resolverPorHost("local.test.invalid")).thenReturn(StorefrontResponse.builder()
                .revendedorId(tienda).dominio("local.test.invalid").build());
        when(publicCatalogoService.getProductoDetalle(producto, tienda)).thenReturn(PublicProductoDetalleResponse.builder()
                .modoCatalogo(true).mejorPrecioFinalEstimado(new BigDecimal("128000.25"))
                .nombrePublico("Café <especial>").condicionesEntrega("Coordinar con la tienda.").build());
        var response = controller.shareProducto(producto, "local.test.invalid", null, null, "og2");
        assertTrue(response.getBody().contains("128.000,25"));
        assertTrue(response.getBody().contains("Coordinar con la tienda."));
        assertTrue(response.getBody().contains("Café &lt;especial&gt;"));
        org.junit.jupiter.api.Assertions.assertFalse(response.getBody().contains("Entrega estimada"));
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }

    @Test
    void compartirProductoPropioGeneraMetadatosParaWhatsapp() {
        UUID revendedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        StorefrontResponse storefront = StorefrontResponse.builder()
                .tipoStorefront(TipoStorefront.REVENDEDOR)
                .revendedorId(revendedorId)
                .dominio("megareconquista.com")
                .nombreComercial("Mega Reconquista")
                .publicada(true)
                .build();
        PublicProductoDetalleResponse producto = PublicProductoDetalleResponse.builder()
                .productoId(productoId)
                .nombrePublico("Producto propio")
                .mejorPrecioFinalEstimado(new BigDecimal("125000.00"))
                .demoraEstimadaDias(3)
                .stockDisponible(2)
                .imagenPrincipal("https://cdn.test/producto.jpg")
                .build();

        when(storefrontService.resolverPorHost("megareconquista.com")).thenReturn(storefront);
        when(publicCatalogoService.getProductoDetalle(productoId, revendedorId)).thenReturn(producto);

        ResponseEntity<String> response = controller.shareProducto(
                productoId, "megareconquista.com", null, null, "og2");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Producto propio"));
        assertTrue(response.getBody().contains("og:image"));
        assertTrue(response.getBody().contains("&amp;v=og2"));
        assertTrue(response.getBody().contains("/producto/" + productoId));
        verify(publicCatalogoService).getProductoDetalle(productoId, revendedorId);
    }
}
