package ar.com.mercadodigital.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.dto.request.ProductoMediaRequest;
import ar.com.mercadodigital.backend.dto.request.ProveedorProductoUpsertRequest;
import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoResponse;
import ar.com.mercadodigital.backend.entity.CategoriaProducto;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorProductoService {

    private static final String MONEDA_SOPORTADA = "ARS";
    private static final int MAX_IMAGENES_ALTA = 3;

    private final ProveedorOwnershipService proveedorOwnershipService;
    private final ProductoRepository productoRepository;
    private final ProductoCanonicoRepository productoCanonicoRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final ProductoMediaRepository productoMediaRepository;
    private final ProductoMediaService productoMediaService;
    private final CategoriaProductoService categoriaProductoService;

    @Transactional(readOnly = true)
    public List<ProveedorProductoResponse> getProductos(UUID proveedorId, Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        return productoRepository.findByProveedor_IdOrderByUpdatedAtDesc(proveedor.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProveedorProductoResponse crearProducto(
            UUID proveedorId,
            ProveedorProductoUpsertRequest request,
            Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        ProductoCanonico productoCanonico = resolveProductoCanonico(request.getProductoCanonicoId());
        CategoriaProducto categoriaProducto = categoriaProductoService.resolveCategoriaProducto(request.getCategoriaProductoId());

        Producto producto = new Producto();
        applyProductoData(producto, proveedor, productoCanonico, categoriaProducto, request);
        Producto saved = productoRepository.save(producto);
        upsertStock(proveedor, saved, request);
        addImagenesIniciales(saved.getId(), request.getImagenes());
        return toResponse(saved);
    }

    @Transactional
    public ProveedorProductoResponse actualizarProducto(
            UUID proveedorId,
            UUID productoId,
            ProveedorProductoUpsertRequest request,
            Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        Producto producto = productoRepository.findByIdAndProveedor_Id(productoId, proveedor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        ProductoCanonico productoCanonico = resolveProductoCanonico(request.getProductoCanonicoId());
        CategoriaProducto categoriaProducto = categoriaProductoService.resolveCategoriaProducto(request.getCategoriaProductoId());

        applyProductoData(producto, proveedor, productoCanonico, categoriaProducto, request);
        Producto saved = productoRepository.save(producto);
        upsertStock(proveedor, saved, request);
        return toResponse(saved);
    }

    @Transactional
    public ProveedorProductoResponse desactivarProducto(
            UUID proveedorId,
            UUID productoId,
            Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        Producto producto = getProductoProveedor(proveedor.getId(), productoId);
        producto.setActivo(false);
        Producto saved = productoRepository.save(producto);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductoMediaResponse> getMedia(UUID proveedorId, UUID productoId, Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        getProductoProveedor(proveedor.getId(), productoId);
        return productoMediaService.getMedia(productoId);
    }

    @Transactional
    public ProductoMediaResponse addMedia(
            UUID proveedorId,
            UUID productoId,
            ProductoMediaRequest request,
            Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        getProductoProveedor(proveedor.getId(), productoId);
        boolean esPrincipal = request.getEsPrincipal() != null && request.getEsPrincipal();
        return productoMediaService.addMedia(productoId, request.getTipoMedia(), request.getUrl(), request.getOrden(), esPrincipal);
    }

    @Transactional
    public void deleteMedia(UUID proveedorId, UUID productoId, UUID mediaId, Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        getProductoProveedor(proveedor.getId(), productoId);
        productoMediaRepository.findByIdAndProducto_Id(mediaId, productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Media no encontrada: " + mediaId));
        productoMediaService.deleteMedia(mediaId);
    }

    private Producto getProductoProveedor(UUID proveedorId, UUID productoId) {
        return productoRepository.findByIdAndProveedor_Id(productoId, proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
    }

    private void applyProductoData(
            Producto producto,
            Proveedor proveedor,
            ProductoCanonico productoCanonico,
            CategoriaProducto categoriaProducto,
            ProveedorProductoUpsertRequest request) {
        validateRequest(request);
        validateSkuUniqueness(producto.getId(), request.getSku());
        producto.setProveedor(proveedor);
        producto.setProductoCanonico(productoCanonico);
        producto.setCategoriaProducto(categoriaProducto);
        producto.setSku(request.getSku().trim());
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(StringUtils.hasText(request.getDescripcion()) ? request.getDescripcion().trim() : null);
        producto.setPrecioCosto(request.getPrecioCosto());
        producto.setMoneda(MONEDA_SOPORTADA);
        producto.setCategoriaPaquete(request.getCategoriaPaquete().trim().toUpperCase());
        producto.setActivo(request.getActivo() == null || request.getActivo());
    }

    private void upsertStock(Proveedor proveedor, Producto producto, ProveedorProductoUpsertRequest request) {
        ProveedorStock stock = proveedorStockRepository
                .findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(proveedor.getId(), producto.getId())
                .orElseGet(ProveedorStock::new);
        stock.setProveedor(proveedor);
        stock.setProducto(producto);
        stock.setStock(request.getStock());
        stock.setPrecioCostoActual(request.getPrecioCosto());
        proveedorStockRepository.save(stock);
    }

    private ProductoCanonico resolveProductoCanonico(UUID productoCanonicoId) {
        if (productoCanonicoId == null) {
            return null;
        }

        return productoCanonicoRepository.findByIdAndActivoTrue(productoCanonicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto canonico no encontrado: " + productoCanonicoId));
    }

    private void validateRequest(ProveedorProductoUpsertRequest request) {
        if (request.getStock() == null || request.getStock() < 0) {
            throw new BadRequestException("stock debe ser mayor o igual a 0");
        }
        if (request.getPrecioCosto() == null || request.getPrecioCosto().signum() <= 0) {
            throw new BadRequestException("precioCosto debe ser mayor a 0");
        }
        if (!StringUtils.hasText(request.getSku())) {
            throw new BadRequestException("sku es obligatorio");
        }
        if (!StringUtils.hasText(request.getNombre())) {
            throw new BadRequestException("nombre es obligatorio");
        }
        if (!StringUtils.hasText(request.getCategoriaPaquete())) {
            throw new BadRequestException("categoriaPaquete es obligatorio");
        }
        validateImagenes(request.getImagenes());
    }

    private void validateImagenes(List<String> imagenes) {
        if (imagenes == null || imagenes.isEmpty()) {
            return;
        }

        if (imagenes.size() > MAX_IMAGENES_ALTA) {
            throw new BadRequestException("imagenes permite hasta 3 URLs");
        }

        for (String url : imagenes) {
            if (!StringUtils.hasText(url)) {
                throw new BadRequestException("imagenes no permite valores vacios");
            }
            String normalized = url.trim();
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
                throw new BadRequestException("imagenes solo admite URLs http/https");
            }
        }
    }

    private void addImagenesIniciales(UUID productoId, List<String> imagenes) {
        if (imagenes == null || imagenes.isEmpty()) {
            return;
        }

        for (int index = 0; index < imagenes.size(); index++) {
            String url = imagenes.get(index);
            if (!StringUtils.hasText(url)) {
                continue;
            }
            productoMediaService.addMedia(productoId, TipoMedia.IMAGEN, url.trim(), index + 1, index == 0);
        }
    }

    private void validateSkuUniqueness(UUID productoId, String sku) {
        String normalizedSku = sku.trim();
        boolean exists = productoId == null
                ? productoRepository.existsBySkuIgnoreCase(normalizedSku)
                : productoRepository.existsBySkuIgnoreCaseAndIdNot(normalizedSku, productoId);
        if (exists) {
            throw new BadRequestException("sku ya existe: " + normalizedSku);
        }
    }

    private ProveedorProductoResponse toResponse(Producto producto) {
        Integer stockDisponible = proveedorStockRepository
                .sumStockByProveedorIdAndProductoId(producto.getProveedor().getId(), producto.getId());
        List<ProductoMedia> media = productoMediaRepository.findByProducto_IdOrderByOrdenAsc(producto.getId());
        String imagenPrincipalUrl = resolveImagenPrincipal(media);
        boolean tieneVideo = media.stream().anyMatch(item -> TipoMedia.VIDEO.equals(item.getTipoMedia()));

        return ProveedorProductoResponse.builder()
                .id(producto.getId())
                .proveedorId(producto.getProveedor().getId())
                .productoCanonicoId(producto.getProductoCanonico() != null ? producto.getProductoCanonico().getId() : null)
                .productoCanonicoNombre(producto.getProductoCanonico() != null ? producto.getProductoCanonico().getNombrePublico() : null)
                .categoriaProductoId(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getId() : null)
                .categoriaProductoNombre(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null)
                .estadoNormalizacion(producto.getProductoCanonico() != null ? "ASOCIADO" : "PENDIENTE")
                .sku(producto.getSku())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precioCosto(producto.getPrecioCosto())
                .moneda(producto.getMoneda())
                .categoriaPaquete(producto.getCategoriaPaquete())
                .stockDisponible(stockDisponible != null ? stockDisponible : 0)
                .activo(producto.isActivo())
                .cantidadMedia(media.size())
                .imagenPrincipalUrl(imagenPrincipalUrl)
                .tieneVideo(tieneVideo)
                .build();
    }

    private String resolveImagenPrincipal(List<ProductoMedia> media) {
        return media.stream()
                .filter(item -> item.isEsPrincipal() && TipoMedia.IMAGEN.equals(item.getTipoMedia()))
                .findFirst()
                .or(() -> media.stream().filter(item -> TipoMedia.IMAGEN.equals(item.getTipoMedia())).findFirst())
                .map(ProductoMedia::getUrl)
                .orElse(null);
    }
}
