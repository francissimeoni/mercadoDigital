package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.PublicCatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicCatalogoService {

    private final StorefrontService storefrontService;
    private final CatalogoService catalogoService;
    private final RevendedorProductoRepository productoPropioRepository;
    private final CostoEnvioService costoEnvioService;
    private final PspConfigService pspConfigService;
    private final CatalogoModoService modoCatalogo;
    private final CatalogoPublicoService catalogoIndependiente;

    @Transactional(readOnly = true)
    public List<PublicCatalogoItemResponse> getCatalogo(UUID revendedorId) {
        storefrontService.validarRevendedorPublico(revendedorId);
        if (modoCatalogo.activo(revendedorId)) {
            return catalogoIndependiente.listado(revendedorId).stream().map(this::toCatalogoIndependiente).toList();
        }
        return productoPropioRepository
                .findByRevendedor_IdAndActivoTrueAndStockGreaterThanOrderByNombreAsc(revendedorId, 0)
                .stream()
                .filter(p -> p.getPrecioVenta() != null && p.getCategoriaPaquete() != null)
                .map(this::toCatalogoItemPropio)
                .sorted(Comparator.comparing(PublicCatalogoItemResponse::getNombrePublico,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicCatalogoItemResponse> getCatalogoProveedor(UUID proveedorId) {
        storefrontService.validarProveedorPublico(proveedorId);
        return catalogoService.getCatalogoPublicoProveedor(proveedorId);
    }

    @Transactional(readOnly = true)
    public PublicProductoDetalleResponse getProductoDetalle(UUID productoCanonicoId, UUID revendedorId) {
        storefrontService.validarRevendedorPublico(revendedorId);
        if (modoCatalogo.activo(revendedorId)) {
            var publicacion = catalogoIndependiente.detalle(revendedorId, productoCanonicoId);
            var producto = publicacion.producto();
            return PublicProductoDetalleResponse.builder().productoId(producto.getId()).productoCanonicoId(null)
                    .nombrePublico(producto.getNombre()).descripcion(producto.getDescripcion())
                    .mejorPrecioFinalEstimado(publicacion.precioArs())
                    .precioPublicoArs(publicacion.precioArs().toPlainString()).modoCatalogo(true)
                    .condicionesEntrega(publicacion.condicionesEntrega()).sku(producto.getSku())
                    .imagenPrincipal(resolverImagenPrincipal(producto)).imagenes(resolverImagenes(producto))
                    .origen("PROPIO_REVENDEDOR").promocion(producto.isPromocion()).novedad(producto.isNovedad()).build();
        }
        var propio = productoPropioRepository.findByIdAndRevendedor_Id(productoCanonicoId, revendedorId)
                .filter(producto -> producto.isActivo() && producto.getStock() != null && producto.getStock() > 0
                        && producto.getPrecioVenta() != null && producto.getCategoriaPaquete() != null);
        if (propio.isPresent()) {
            return toDetallePropio(propio.get());
        }
        throw new ResourceNotFoundException("Producto propio no disponible en este storefront: " + productoCanonicoId);
    }

    @Transactional(readOnly = true)
    public PublicProductoDetalleResponse getProductoDetalleProveedor(UUID productoId, UUID proveedorId) {
        storefrontService.validarProveedorPublico(proveedorId);
        return catalogoService.getProductoDetallePublicoProveedor(productoId, proveedorId);
    }

    private PublicCatalogoItemResponse toCatalogoItemPropio(RevendedorProducto producto) {
        return PublicCatalogoItemResponse.builder()
                .productoId(producto.getId())
                .productoCanonicoId(null)
                .nombrePublico(producto.getNombre())
                .categoriaProducto(producto.getCategoriaProducto() != null
                        ? producto.getCategoriaProducto().getNombre() : "Otros")
                .mejorPrecioFinalEstimado(producto.getPrecioVenta())
                .demoraEstimadaDias(costoEnvioService.resolveTarifa(producto.getCategoriaPaquete()).demoraEstimadaDias())
                .stockDisponible(producto.getStock())
                .cantidadOfertas(1)
                .imagenPrincipal(resolverImagenPrincipal(producto))
                .origen("PROPIO_REVENDEDOR")
                .promocion(producto.isPromocion())
                .novedad(producto.isNovedad())
                .build();
    }

    private PublicCatalogoItemResponse toCatalogoIndependiente(CatalogoPublicoService.Publicacion publicacion) {
        var producto = publicacion.producto();
        return PublicCatalogoItemResponse.builder().productoId(producto.getId()).nombrePublico(producto.getNombre())
                .categoriaProducto(producto.getCategoriaProducto() != null
                        ? producto.getCategoriaProducto().getNombre() : "Otros")
                .mejorPrecioFinalEstimado(publicacion.precioArs()).precioPublicoArs(publicacion.precioArs().toPlainString())
                .modoCatalogo(true).imagenPrincipal(resolverImagenPrincipal(producto)).origen("PROPIO_REVENDEDOR")
                .promocion(producto.isPromocion()).novedad(producto.isNovedad()).build();
    }

    private PublicProductoDetalleResponse toDetallePropio(RevendedorProducto producto) {
        BigDecimal seguro = producto.getPrecioVenta()
                .multiply(pspConfigService.getSeguroTransporteFactor())
                .setScale(2, RoundingMode.HALF_UP);
        List<String> imagenes = resolverImagenes(producto);
        return PublicProductoDetalleResponse.builder()
                .productoId(producto.getId())
                .productoCanonicoId(null)
                .nombrePublico(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .mejorPrecioFinalEstimado(producto.getPrecioVenta())
                .seguroTransporteEstimado(seguro)
                .totalConSeguroEstimado(producto.getPrecioVenta().add(seguro))
                .demoraEstimadaDias(costoEnvioService.resolveTarifa(producto.getCategoriaPaquete()).demoraEstimadaDias())
                .stockDisponible(producto.getStock())
                .imagenPrincipal(resolverImagenPrincipal(producto))
                .imagenes(imagenes)
                .origen("PROPIO_REVENDEDOR")
                .promocion(producto.isPromocion())
                .novedad(producto.isNovedad())
                .build();
    }

    private String resolverImagenPrincipal(RevendedorProducto producto) {
        return producto.getMedia().stream()
                .filter(media -> media.isEsPrincipal() && TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                .map(RevendedorProductoMedia::getUrl)
                .findFirst()
                .orElseGet(() -> producto.getMedia().stream()
                        .filter(media -> TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                        .sorted(Comparator.comparing(RevendedorProductoMedia::getOrden))
                        .map(RevendedorProductoMedia::getUrl)
                        .findFirst()
                        .orElse(null));
    }

    private List<String> resolverImagenes(RevendedorProducto producto) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        String principal = resolverImagenPrincipal(producto);
        if (principal != null && !principal.isBlank()) {
            urls.add(principal);
        }
        producto.getMedia().stream()
                .filter(media -> TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                .sorted(Comparator.comparing(RevendedorProductoMedia::getOrden))
                .map(RevendedorProductoMedia::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .forEach(urls::add);
        return urls.stream().toList();
    }
}
