package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.dto.request.RevendedorAjusteMasivoPreciosRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorPrecioMasivoItemRequest;
import ar.com.mercadodigital.backend.entity.BaseEntity;
import ar.com.mercadodigital.backend.entity.CategoriaProducto;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.service.RevendedorOwnershipService;
import java.math.RoundingMode;
import java.util.LinkedHashSet;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.RevendedorProductoMediaRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorProductoRequest;
import ar.com.mercadodigital.backend.dto.response.RevendedorProductoMediaResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoResponse;
import ar.com.mercadodigital.backend.entity.CategoriaPaquete;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.CategoriaProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevendedorProductoService {

    private final RevendedorOwnershipService ownershipService;
    private final RevendedorRepository revendedorRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final RevendedorProductoRepository productoRepository;
    private final RevendedorProductoMediaRepository mediaRepository;
    private final CatalogoModoService modoCatalogo;

    @Transactional(readOnly = true)
    public List<RevendedorProductoResponse> listar(UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return productoRepository.findByRevendedor_IdOrderByUpdatedAtDesc(revendedorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProveedorCategoriaProductoResponse> listarCategorias(UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return categoriaProductoRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(categoria -> ProveedorCategoriaProductoResponse.builder()
                        .id(categoria.getId())
                        .nombre(categoria.getNombre())
                        .build())
                .toList();
    }

    @Transactional
    public RevendedorProductoResponse crear(UUID revendedorId, RevendedorProductoRequest request) {
        ownershipService.validarOwnership(revendedorId);
        String sku = request.getSku().trim();
        if (productoRepository.existsByRevendedor_IdAndSkuIgnoreCase(revendedorId, sku)) {
            throw new OperacionEnConflictoException("Ya existe un producto propio con SKU: " + sku);
        }
        RevendedorProducto producto = new RevendedorProducto();
        producto.setRevendedor(revendedorRepository.findById(revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId)));
        apply(producto, request);
        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public RevendedorProductoResponse actualizar(
            UUID revendedorId, UUID productoId, RevendedorProductoRequest request) {
        ownershipService.validarOwnership(revendedorId);
        RevendedorProducto producto = getProducto(revendedorId, productoId);
        if (producto.getPrecioVenta() == null) throw new OperacionEnConflictoException(
                "Este producto se administra desde la ficha y precio del catálogo.");
        String sku = request.getSku().trim();
        if (productoRepository.existsByRevendedor_IdAndSkuIgnoreCaseAndIdNot(revendedorId, sku, productoId)) {
            throw new OperacionEnConflictoException("Ya existe un producto propio con SKU: " + sku);
        }
        apply(producto, request);
        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public RevendedorProductoMediaResponse agregarMedia(
            UUID revendedorId, UUID productoId, RevendedorProductoMediaRequest request) {
        ownershipService.validarOwnership(revendedorId);
        RevendedorProducto producto = getProducto(revendedorId, productoId);
        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            producto.getMedia().forEach(media -> media.setEsPrincipal(false));
        }
        RevendedorProductoMedia media = new RevendedorProductoMedia();
        media.setRevendedorProducto(producto);
        media.setTipoMedia(request.getTipoMedia());
        media.setUrl(request.getUrl().trim());
        media.setOrden(request.getOrden());
        media.setEsPrincipal(Boolean.TRUE.equals(request.getEsPrincipal()));
        producto.getMedia().add(media);
        producto.setUpdatedAt(java.time.LocalDateTime.now());
        return toMediaResponse(mediaRepository.save(media));
    }

    @Transactional
    public RevendedorProductoMediaResponse actualizarMedia(
            UUID revendedorId, UUID productoId, UUID mediaId, RevendedorProductoMediaRequest request) {
        ownershipService.validarOwnership(revendedorId);
        RevendedorProducto producto = getProducto(revendedorId, productoId);
        RevendedorProductoMedia media = mediaRepository
                .findByIdAndRevendedorProducto_IdAndRevendedorProducto_Revendedor_Id(
                        mediaId, productoId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Media de producto propio no encontrada: " + mediaId));
        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            producto.getMedia().forEach(item -> item.setEsPrincipal(item.getId().equals(mediaId)));
        }
        media.setTipoMedia(request.getTipoMedia());
        media.setUrl(request.getUrl().trim());
        media.setOrden(request.getOrden());
        media.setEsPrincipal(Boolean.TRUE.equals(request.getEsPrincipal()));
        producto.setUpdatedAt(java.time.LocalDateTime.now());
        return toMediaResponse(mediaRepository.save(media));
    }

    @Transactional
    public void eliminarMedia(UUID revendedorId, UUID productoId, UUID mediaId) {
        ownershipService.validarOwnership(revendedorId);
        RevendedorProductoMedia media = mediaRepository
                .findByIdAndRevendedorProducto_IdAndRevendedorProducto_Revendedor_Id(
                        mediaId, productoId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Media de producto propio no encontrada: " + mediaId));
        boolean eraPrincipal = media.isEsPrincipal();
        RevendedorProducto producto = media.getRevendedorProducto();
        producto.setUpdatedAt(java.time.LocalDateTime.now());
        mediaRepository.delete(media);
        if (eraPrincipal) {
            producto.getMedia().stream()
                    .filter(item -> !item.getId().equals(mediaId) && TipoMedia.IMAGEN.equals(item.getTipoMedia()))
                    .sorted(Comparator.comparing(RevendedorProductoMedia::getOrden))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setEsPrincipal(true);
                        mediaRepository.save(item);
                    });
        }
    }

    private RevendedorProducto getProducto(UUID revendedorId, UUID productoId) {
        return productoRepository.findByIdAndRevendedor_Id(productoId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto propio no encontrado: " + productoId));
    }

    private void apply(RevendedorProducto producto, RevendedorProductoRequest request) {
        boolean catalogo = modoCatalogo.activo(producto.getRevendedor().getId());
        if (!catalogo && request.getStock() == null) throw new BadRequestException("El stock es obligatorio en el modo de venta anterior.");
        producto.setSku(request.getSku().trim());
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(normalizarOpcional(request.getDescripcion()));
        producto.setPrecioVenta(request.getPrecioVenta());
        // En catálogo no se modifica el stock histórico ni se inventa stock para fichas nuevas.
        if (!catalogo) producto.setStock(request.getStock());
        producto.setCategoriaPaquete(normalizarCategoriaPaquete(request.getCategoriaPaquete()));
        producto.setCategoriaProducto(request.getCategoriaProductoId() == null ? null
                : categoriaProductoRepository.findById(request.getCategoriaProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Categoria de producto no encontrada: " + request.getCategoriaProductoId())));
        producto.setActivo(Boolean.TRUE.equals(request.getActivo()));
        producto.setPromocion(Boolean.TRUE.equals(request.getPromocion()));
        producto.setNovedad(Boolean.TRUE.equals(request.getNovedad()));
        producto.setMoneda("ARS");
    }

    private String normalizarCategoriaPaquete(String value) {
        try {
            return CategoriaPaquete.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("categoriaPaquete invalida: " + value);
        }
    }

    private String normalizarOpcional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public RevendedorProductoResponse toResponse(RevendedorProducto producto) {
        List<RevendedorProductoMediaResponse> media = producto.getMedia().stream()
                .sorted(Comparator.comparing(RevendedorProductoMedia::getOrden)
                        .thenComparing(RevendedorProductoMedia::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toMediaResponse)
                .toList();
        String imagenPrincipal = producto.getMedia().stream()
                .filter(item -> item.isEsPrincipal() && TipoMedia.IMAGEN.equals(item.getTipoMedia()))
                .map(RevendedorProductoMedia::getUrl)
                .findFirst()
                .orElseGet(() -> producto.getMedia().stream()
                        .filter(item -> TipoMedia.IMAGEN.equals(item.getTipoMedia()))
                        .sorted(Comparator.comparing(RevendedorProductoMedia::getOrden))
                        .map(RevendedorProductoMedia::getUrl)
                        .findFirst()
                        .orElse(null));
        return RevendedorProductoResponse.builder()
                .id(producto.getId())
                .sku(producto.getSku())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precioVenta(producto.getPrecioVenta())
                .moneda(producto.getMoneda())
                .stock(producto.getStock())
                .categoriaPaquete(producto.getCategoriaPaquete())
                .categoriaProductoId(producto.getCategoriaProducto() != null
                        ? producto.getCategoriaProducto().getId() : null)
                .categoriaProductoNombre(producto.getCategoriaProducto() != null
                        ? producto.getCategoriaProducto().getNombre() : null)
                .activo(producto.isActivo())
                .promocion(producto.isPromocion())
                .novedad(producto.isNovedad())
                .media(media)
                .imagenPrincipalUrl(imagenPrincipal)
                .createdAt(producto.getCreatedAt())
                .updatedAt(producto.getUpdatedAt())
                .build();
    }

    private RevendedorProductoMediaResponse toMediaResponse(RevendedorProductoMedia media) {
        return RevendedorProductoMediaResponse.builder()
                .id(media.getId())
                .tipoMedia(media.getTipoMedia())
                .url(media.getUrl())
                .orden(media.getOrden())
                .esPrincipal(media.isEsPrincipal())
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .build();
    }


    @Transactional
    public List<RevendedorProductoResponse> ajustarPreciosMasivamente(UUID revendedorId, RevendedorAjusteMasivoPreciosRequest request) {
        this.ownershipService.validarOwnership(revendedorId);
        modoCatalogo.rechazarPreciosAnteriores(revendedorId);
        LinkedHashSet<UUID> productoIds = new LinkedHashSet<>();
        request.getItems().forEach(item -> {
            if (!productoIds.add(item.getProductoId())) {
                throw new BadRequestException("El producto no puede repetirse en la actualizacion masiva: " + String.valueOf(item.getProductoId()));
            }
        });
        return request.getItems().stream().map(item -> this.actualizarPrecioMasivo(revendedorId, (RevendedorPrecioMasivoItemRequest)item)).map(this.productoRepository::save).map(this::toResponse).toList();
    }

    private RevendedorProducto actualizarPrecioMasivo(UUID revendedorId, RevendedorPrecioMasivoItemRequest item) {
        RevendedorProducto producto = this.productoRepository.findForUpdate(item.getProductoId(), revendedorId).orElseThrow(() -> new ResourceNotFoundException("Producto propio no encontrado: " + String.valueOf(item.getProductoId())));
        if (producto.getPrecioVenta() == null || !"ARS".equals(producto.getMoneda())) {
            throw new BadRequestException("El ajuste masivo actual solo admite precios en ARS. Los precios del nuevo catálogo se configuran por separado.");
        }
        producto.setPrecioVenta(item.getPrecioFinal().setScale(2, RoundingMode.HALF_UP));
        return producto;
    }
}
