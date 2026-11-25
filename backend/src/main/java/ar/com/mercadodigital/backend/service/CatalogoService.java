package ar.com.mercadodigital.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.CatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoDetalleResponse;
import ar.com.mercadodigital.backend.dto.response.PublicCatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProductoCanonicoRepository productoCanonicoRepository;
    private final OfertaProductoCanonicoService ofertaProductoCanonicoService;
    private final ProductoRepository productoRepository;
    private final ProductoMediaRepository productoMediaRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final CostoOperativoMdService costoOperativoMdService;
    private final CostoEnvioService costoEnvioService;
    private final PspConfigService pspConfigService;

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> getCatalogo(UUID revendedorId) {
        return productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc().stream()
                .map(canonico -> toCatalogoItemInterno(canonico, revendedorId))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(CatalogoItemResponse::getNombrePublico))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDetalleResponse getProductoDetalle(UUID productoCanonicoId, UUID revendedorId) {
        ProductoCanonico canonico = productoCanonicoRepository.findByIdAndActivoTrue(productoCanonicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto canonico no encontrado: " + productoCanonicoId));

        List<OfertaProductoResponse> ofertasConStock = ofertaProductoCanonicoService
                .getOfertasInternasConStock(productoCanonicoId, revendedorId);
        if (ofertasConStock.isEmpty()) {
            throw new ResourceNotFoundException("Producto sin oferta disponible para revendedor: " + productoCanonicoId);
        }

        OfertaProductoResponse mejorOferta = ofertasConStock.get(0);
        String descripcion = productoRepository.findById(mejorOferta.getProductoId())
                .map(producto -> producto.getDescripcion())
                .orElse(null);

        return ProductoDetalleResponse.builder()
                .productoCanonicoId(canonico.getId())
                .nombrePublico(canonico.getNombrePublico())
                .descripcion(descripcion)
                .mejorCostoOperativoEstimado(mejorOferta.getPrecioFinalEstimado())
                .demoraEstimadaDias(mejorOferta.getDemoraEstimadaDias())
                .stockDisponible(mejorOferta.getStockDisponible())
                .imagenPrincipal(resolverImagenPrincipal(mejorOferta.getProductoId()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<PublicCatalogoItemResponse> getCatalogoPublico(UUID revendedorId) {
        return productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc().stream()
                .map(canonico -> toCatalogoItemPublico(canonico, revendedorId))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(PublicCatalogoItemResponse::getNombrePublico))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicCatalogoItemResponse> getCatalogoPublicoProveedor(UUID proveedorId) {
        return productoRepository.findByProveedor_IdOrderByUpdatedAtDesc(proveedorId).stream()
                .filter(ar.com.mercadodigital.backend.entity.Producto::isActivo)
                .map(this::toCatalogoItemPublicoProveedor)
                .filter(item -> item != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicProductoDetalleResponse getProductoDetallePublico(UUID productoCanonicoId, UUID revendedorId) {
        ProductoCanonico canonico = productoCanonicoRepository.findByIdAndActivoTrue(productoCanonicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto canonico no encontrado: " + productoCanonicoId));

        List<OfertaProductoResponse> ofertasConStock = ofertaProductoCanonicoService
                .getOfertasConStock(productoCanonicoId, revendedorId);
        if (ofertasConStock.isEmpty()) {
            throw new ResourceNotFoundException("Producto sin oferta disponible para revendedor: " + productoCanonicoId);
        }

        List<OfertaProductoResponse> ofertasConSeguro = ofertaProductoCanonicoService
                .getOfertasConStock(productoCanonicoId, revendedorId, true);

        OfertaProductoResponse mejorOferta = ofertasConStock.get(0);
        OfertaProductoResponse mejorOfertaConSeguro = ofertasConSeguro.isEmpty() ? mejorOferta : ofertasConSeguro.get(0);
        String descripcion = productoRepository.findById(mejorOferta.getProductoId())
                .map(producto -> producto.getDescripcion())
                .orElse(null);
        BigDecimal seguroTransporteEstimado = nvl(mejorOfertaConSeguro.getMontoSeguroEstimado());
        BigDecimal totalConSeguroEstimado = nvl(mejorOfertaConSeguro.getPrecioFinalEstimado()).add(seguroTransporteEstimado);
        List<String> imagenes = resolverImagenes(mejorOferta.getProductoId());

        return PublicProductoDetalleResponse.builder()
                .productoId(mejorOferta.getProductoId())
                .productoCanonicoId(canonico.getId())
                .nombrePublico(canonico.getNombrePublico())
                .descripcion(descripcion)
                .mejorPrecioFinalEstimado(mejorOferta.getPrecioFinalEstimado())
                .seguroTransporteEstimado(seguroTransporteEstimado)
                .totalConSeguroEstimado(totalConSeguroEstimado)
                .demoraEstimadaDias(mejorOferta.getDemoraEstimadaDias())
                .stockDisponible(mejorOferta.getStockDisponible())
                .imagenPrincipal(resolverImagenPrincipal(mejorOferta.getProductoId()))
                .imagenes(imagenes)
                .origen("CANONICO_PROVEEDOR")
                .build();
    }

    @Transactional(readOnly = true)
    public PublicProductoDetalleResponse getProductoDetallePublicoProveedor(UUID productoId, UUID proveedorId) {
        var producto = productoRepository.findByIdAndProveedor_Id(productoId, proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no disponible para proveedor: " + productoId));
        if (!producto.isActivo()) {
            throw new ResourceNotFoundException("Producto no disponible para proveedor: " + productoId);
        }

        Integer stockDisponible = proveedorStockRepository.sumStockByProveedorIdAndProductoId(proveedorId, productoId);
        if (stockDisponible == null || stockDisponible <= 0) {
            throw new ResourceNotFoundException("Producto sin stock para proveedor: " + productoId);
        }

        BigDecimal precioFinal = calcularPrecioPublicoProveedor(producto);
        BigDecimal seguroTransporteEstimado = nvl(producto.getPrecioCosto()).multiply(nvl(pspConfigService.getSeguroTransporteFactor()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalConSeguroEstimado = precioFinal.add(seguroTransporteEstimado);
        List<String> imagenes = resolverImagenes(productoId);

        return PublicProductoDetalleResponse.builder()
                .productoId(productoId)
                .productoCanonicoId(producto.getProductoCanonico() != null ? producto.getProductoCanonico().getId() : null)
                .nombrePublico(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .mejorPrecioFinalEstimado(precioFinal)
                .seguroTransporteEstimado(seguroTransporteEstimado)
                .totalConSeguroEstimado(totalConSeguroEstimado)
                .demoraEstimadaDias(costoEnvioService.resolveTarifa(producto.getCategoriaPaquete()).demoraEstimadaDias())
                .stockDisponible(stockDisponible)
                .imagenPrincipal(resolverImagenPrincipal(productoId))
                .imagenes(imagenes)
                .origen("PROVEEDOR")
                .build();
    }

    private CatalogoItemResponse toCatalogoItemInterno(ProductoCanonico canonico, UUID revendedorId) {
        List<OfertaProductoResponse> ofertasConStock = ofertaProductoCanonicoService
                .getOfertasInternasConStock(canonico.getId(), revendedorId);
        if (ofertasConStock.isEmpty()) {
            return null;
        }

        OfertaProductoResponse mejorOferta = ofertasConStock.get(0);
        String imagenPrincipal = resolverImagenPrincipal(mejorOferta.getProductoId());
        return CatalogoItemResponse.builder()
                .productoCanonicoId(canonico.getId())
                .nombrePublico(canonico.getNombrePublico())
                .mejorCostoOperativoEstimado(mejorOferta.getPrecioFinalEstimado())
                .demoraEstimadaDias(mejorOferta.getDemoraEstimadaDias())
                .stockDisponible(mejorOferta.getStockDisponible())
                .cantidadOfertas(ofertasConStock.size())
                .imagenPrincipal(imagenPrincipal)
                .build();
    }

    private PublicCatalogoItemResponse toCatalogoItemPublico(ProductoCanonico canonico, UUID revendedorId) {
        List<OfertaProductoResponse> ofertasConStock = ofertaProductoCanonicoService
                .getOfertasConStock(canonico.getId(), revendedorId);
        if (ofertasConStock.isEmpty()) {
            return null;
        }

        OfertaProductoResponse mejorOferta = ofertasConStock.get(0);
        String imagenPrincipal = resolverImagenPrincipal(mejorOferta.getProductoId());
        String categoriaProducto = resolverCategoriaProductoPublica(mejorOferta.getProductoId());
        return PublicCatalogoItemResponse.builder()
                .productoId(mejorOferta.getProductoId())
                .productoCanonicoId(canonico.getId())
                .nombrePublico(canonico.getNombrePublico())
                .categoriaProducto(categoriaProducto)
                .mejorPrecioFinalEstimado(mejorOferta.getPrecioFinalEstimado())
                .demoraEstimadaDias(mejorOferta.getDemoraEstimadaDias())
                .stockDisponible(mejorOferta.getStockDisponible())
                .cantidadOfertas(ofertasConStock.size())
                .imagenPrincipal(imagenPrincipal)
                .origen("CANONICO_PROVEEDOR")
                .build();
    }

    private PublicCatalogoItemResponse toCatalogoItemPublicoProveedor(ar.com.mercadodigital.backend.entity.Producto producto) {
        Integer stockDisponible = proveedorStockRepository
                .sumStockByProveedorIdAndProductoId(producto.getProveedor().getId(), producto.getId());
        if (stockDisponible == null || stockDisponible <= 0) {
            return null;
        }
        BigDecimal precioFinal = calcularPrecioPublicoProveedor(producto);
        return PublicCatalogoItemResponse.builder()
                .productoId(producto.getId())
                .productoCanonicoId(producto.getProductoCanonico() != null ? producto.getProductoCanonico().getId() : null)
                .nombrePublico(producto.getNombre())
                .categoriaProducto(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : producto.getCategoriaPaquete())
                .mejorPrecioFinalEstimado(precioFinal)
                .demoraEstimadaDias(costoEnvioService.resolveTarifa(producto.getCategoriaPaquete()).demoraEstimadaDias())
                .stockDisponible(stockDisponible)
                .cantidadOfertas(1)
                .imagenPrincipal(resolverImagenPrincipal(producto.getId()))
                .origen("PROVEEDOR")
                .build();
    }

    private String resolverImagenPrincipal(UUID productoId) {
        Optional<ProductoMedia> principal = productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(productoId);
        if (principal.isPresent()) {
            return principal.get().getUrl();
        }
        return productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId).stream()
                .filter(media -> TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                .map(ProductoMedia::getUrl)
                .findFirst()
                .orElse(null);
    }

    private List<String> resolverImagenes(UUID productoId) {
        List<String> orderedImages = productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId).stream()
                .filter(media -> TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                .map(ProductoMedia::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        String principal = resolverImagenPrincipal(productoId);
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        if (principal != null && !principal.isBlank()) {
            dedup.add(principal);
        }
        dedup.addAll(orderedImages);
        return dedup.stream().toList();
    }

    private String resolverCategoriaProductoPublica(UUID productoId) {
        return productoRepository.findById(productoId)
                .map(producto -> {
                    if (producto.getCategoriaProducto() != null && producto.getCategoriaProducto().getNombre() != null) {
                        return producto.getCategoriaProducto().getNombre();
                    }
                    return producto.getCategoriaPaquete();
                })
                .orElse(null);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calcularPrecioPublicoProveedor(ar.com.mercadodigital.backend.entity.Producto producto) {
        BigDecimal costoProveedor = nvl(producto.getPrecioCosto());
        BigDecimal costoMd = costoOperativoMdService.getMontoParaCategoria(producto.getCategoriaPaquete(), costoProveedor);
        BigDecimal pspFee = nvl(pspConfigService.getPspFeeFactor());
        BigDecimal subtotalSinPsp = costoProveedor.add(costoMd);
        BigDecimal divisor = BigDecimal.ONE.subtract(pspFee);
        BigDecimal calculo = subtotalSinPsp.divide(divisor, 10, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.round(calculo.doubleValue()));
    }
}
