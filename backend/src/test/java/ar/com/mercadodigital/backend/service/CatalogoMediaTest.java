package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.CatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoMediaTest {

    @Mock
    private ProductoCanonicoRepository productoCanonicoRepository;
    @Mock
    private OfertaProductoCanonicoService ofertaProductoCanonicoService;
    @Mock
    private ProductoMediaRepository productoMediaRepository;

    @InjectMocks
    private CatalogoService catalogoService;

    @Test
    void usaImagenPrincipalSiExiste() {
        UUID revendedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        ProductoCanonico canonico = canonico("Canonico A");

        when(productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc()).thenReturn(List.of(canonico));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonico.getId(), revendedorId))
                .thenReturn(List.of(oferta(productoId, canonico.getId())));
        when(productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(productoId))
                .thenReturn(Optional.of(media(TipoMedia.IMAGEN, "https://cdn/principal.jpg", 1, true)));

        List<CatalogoItemResponse> result = catalogoService.getCatalogo(revendedorId);
        assertEquals(1, result.size());
        assertEquals("https://cdn/principal.jpg", result.get(0).getImagenPrincipal());
    }

    @Test
    void usaPrimeraImagenPorOrdenSiNoHayPrincipal() {
        UUID revendedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        ProductoCanonico canonico = canonico("Canonico B");

        when(productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc()).thenReturn(List.of(canonico));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonico.getId(), revendedorId))
                .thenReturn(List.of(oferta(productoId, canonico.getId())));
        when(productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(productoId)).thenReturn(Optional.empty());
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)).thenReturn(List.of(
                media(TipoMedia.VIDEO, "https://cdn/video.mp4", 1, false),
                media(TipoMedia.IMAGEN, "https://cdn/segunda.jpg", 2, false),
                media(TipoMedia.IMAGEN, "https://cdn/tercera.jpg", 3, false)));

        List<CatalogoItemResponse> result = catalogoService.getCatalogo(revendedorId);
        assertEquals(1, result.size());
        assertEquals("https://cdn/segunda.jpg", result.get(0).getImagenPrincipal());
    }

    @Test
    void imagenPrincipalQuedaNullSiNoHayImagenes() {
        UUID revendedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        ProductoCanonico canonico = canonico("Canonico C");

        when(productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc()).thenReturn(List.of(canonico));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonico.getId(), revendedorId))
                .thenReturn(List.of(oferta(productoId, canonico.getId())));
        when(productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(productoId)).thenReturn(Optional.empty());
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)).thenReturn(List.of());

        List<CatalogoItemResponse> result = catalogoService.getCatalogo(revendedorId);
        assertEquals(1, result.size());
        assertNull(result.get(0).getImagenPrincipal());
    }

    private ProductoCanonico canonico(String nombre) {
        ProductoCanonico canonico = new ProductoCanonico();
        canonico.setId(UUID.randomUUID());
        canonico.setActivo(true);
        canonico.setNombrePublico(nombre);
        canonico.setMarca("Marca");
        canonico.setModelo("Modelo");
        return canonico;
    }

    private OfertaProductoResponse oferta(UUID productoId, UUID canonicoId) {
        return OfertaProductoResponse.builder()
                .productoId(productoId)
                .productoCanonicoId(canonicoId)
                .precioFinalEstimado(new BigDecimal("1000"))
                .demoraEstimadaDias(2)
                .stockDisponible(10)
                .categoriaPaquete("CHICO")
                .build();
    }

    private ProductoMedia media(TipoMedia tipoMedia, String url, int orden, boolean principal) {
        ProductoMedia media = new ProductoMedia();
        media.setTipoMedia(tipoMedia);
        media.setUrl(url);
        media.setOrden(orden);
        media.setEsPrincipal(principal);
        return media;
    }
}
