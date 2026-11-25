package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoMediaServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProductoMediaRepository productoMediaRepository;

    @InjectMocks
    private ProductoMediaService productoMediaService;

    @Test
    void addMediaPrincipalDesmarcaPrincipalAnterior() {
        UUID productoId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(productoId);

        ProductoMedia anteriorPrincipal = new ProductoMedia();
        anteriorPrincipal.setId(UUID.randomUUID());
        anteriorPrincipal.setProducto(producto);
        anteriorPrincipal.setEsPrincipal(true);
        anteriorPrincipal.setOrden(1);
        anteriorPrincipal.setTipoMedia(TipoMedia.IMAGEN);
        anteriorPrincipal.setUrl("https://cdn/old.jpg");

        when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)).thenReturn(List.of(anteriorPrincipal));
        when(productoMediaRepository.save(any(ProductoMedia.class))).thenAnswer(invocation -> {
            ProductoMedia media = invocation.getArgument(0);
            media.setId(UUID.randomUUID());
            return media;
        });

        ProductoMediaResponse response = productoMediaService.addMedia(
                productoId,
                TipoMedia.IMAGEN,
                "https://cdn/new.jpg",
                2,
                true);

        assertEquals("IMAGEN", response.getTipoMedia());
        assertEquals("https://cdn/new.jpg", response.getUrl());
        assertEquals(2, response.getOrden());
        assertEquals(true, response.isEsPrincipal());
        assertEquals(false, anteriorPrincipal.isEsPrincipal());
    }

    @Test
    void getMediaProductoInexistenteDevuelve404() {
        UUID productoId = UUID.randomUUID();
        when(productoRepository.existsById(productoId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productoMediaService.getMedia(productoId));
        verify(productoMediaRepository, never()).findByProducto_IdOrderByOrdenAsc(any());
    }

    @Test
    void deleteMediaInexistenteDevuelve404() {
        UUID mediaId = UUID.randomUUID();
        when(productoMediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoMediaService.deleteMedia(mediaId));
    }
}
