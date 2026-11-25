package ar.com.mercadodigital.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoMediaService {

    private final ProductoRepository productoRepository;
    private final ProductoMediaRepository productoMediaRepository;

    @Transactional
    public ProductoMediaResponse addMedia(
            UUID productoId,
            TipoMedia tipoMedia,
            String url,
            Integer orden,
            boolean esPrincipal) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

        List<ProductoMedia> mediasActuales = productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId);
        int ordenFinal = orden != null ? orden : mediasActuales.stream()
                .map(ProductoMedia::getOrden)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        if (esPrincipal) {
            mediasActuales.stream()
                    .filter(ProductoMedia::isEsPrincipal)
                    .forEach(media -> media.setEsPrincipal(false));
            productoMediaRepository.saveAll(mediasActuales);
        }

        ProductoMedia media = new ProductoMedia();
        media.setProducto(producto);
        media.setTipoMedia(tipoMedia);
        media.setUrl(url);
        media.setOrden(ordenFinal);
        media.setEsPrincipal(esPrincipal);

        ProductoMedia saved = productoMediaRepository.save(media);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductoMediaResponse> getMedia(UUID productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + productoId);
        }
        return productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMedia(UUID mediaId) {
        ProductoMedia media = productoMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media no encontrada: " + mediaId));
        productoMediaRepository.delete(media);
    }

    private ProductoMediaResponse toResponse(ProductoMedia media) {
        return ProductoMediaResponse.builder()
                .id(media.getId())
                .productoId(media.getProducto().getId())
                .tipoMedia(media.getTipoMedia().name())
                .url(media.getUrl())
                .orden(media.getOrden())
                .esPrincipal(media.isEsPrincipal())
                .build();
    }
}
