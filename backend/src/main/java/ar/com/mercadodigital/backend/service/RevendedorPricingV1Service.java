package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.RevendedorMarkupOverrideRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorPricingConfigRequest;
import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorPricingConfigResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorPricingPreviewItemResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorMarkup;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import ar.com.mercadodigital.backend.repository.RevendedorMarkupRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevendedorPricingV1Service {
    private static final int DEMORA_DEFAULT = 5;

    private final RevendedorRepository revendedorRepository;
    private final RevendedorMarkupRepository revendedorMarkupRepository;
    private final ProductoRepository productoRepository;
    private final ProductoCanonicoRepository productoCanonicoRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final ProductoMediaRepository productoMediaRepository;
    private final PricingService pricingService;
    private final CostoOperativoMdService costoOperativoMdService;
    private final CostoEnvioService costoEnvioService;
    private final CatalogoModoService modoCatalogo;

    @Transactional(readOnly = true)
    public RevendedorPricingConfigResponse getConfiguracion(UUID revendedorId) {
        Revendedor revendedor = getRevendedor(revendedorId);
        return toConfigResponse(revendedor);
    }

    @Transactional
    public RevendedorPricingConfigResponse actualizarConfiguracion(UUID revendedorId, RevendedorPricingConfigRequest request) {
        modoCatalogo.rechazarPreciosAnteriores(revendedorId);
        Revendedor revendedor = getRevendedor(revendedorId);
        PricingRuleConfig normalized = normalizeConfig(
                request.getMarkupPorcentaje(),
                request.getMarkupFijo(),
                request.getComisionAsesorPorcentaje(),
                request.getComisionAsesorFija());

        revendedor.setMarkupPorcentajeDefault(normalized.markupPorcentaje());
        revendedor.setMarkupFijoDefault(normalized.markupFijo());
        revendedor.setComisionAsesorPorcentajeDefault(normalized.comisionAsesorPorcentaje());
        revendedor.setComisionAsesorFijaDefault(normalized.comisionAsesorFija());
        revendedor.setCodigoDescuento(normalizarCodigoDescuento(request.getCodigoDescuento()));
        revendedor.setDescuentoFijo(normalizeMoney(request.getDescuentoFijo(), "descuentoFijo"));
        return toConfigResponse(revendedorRepository.save(revendedor));
    }

    @Transactional
    public void desactivarOverride(UUID revendedorId, UUID productoId) {
        modoCatalogo.rechazarPreciosAnteriores(revendedorId);
        RevendedorMarkup markup = revendedorMarkupRepository
                .findByRevendedor_IdAndProducto_IdAndActivoTrue(revendedorId, productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Override de markup no encontrado para producto: " + productoId));
        markup.setActivo(false);
        revendedorMarkupRepository.save(markup);
    }

    @Transactional
    public RevendedorPricingPreviewItemResponse upsertOverride(UUID revendedorId, UUID productoId, RevendedorMarkupOverrideRequest request) {
        modoCatalogo.rechazarPreciosAnteriores(revendedorId);
        Revendedor revendedor = getRevendedor(revendedorId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

        PricingRuleConfig normalized = normalizeConfig(
                request.getMarkupPorcentaje(),
                request.getMarkupFijo(),
                request.getComisionAsesorPorcentaje(),
                request.getComisionAsesorFija());

        RevendedorMarkup markup = revendedorMarkupRepository
                .findByRevendedor_IdAndProducto_IdAndActivoTrue(revendedorId, productoId)
                .orElseGet(RevendedorMarkup::new);
        markup.setRevendedor(revendedor);
        markup.setProducto(producto);
        markup.setMarkupPorcentaje(normalized.markupPorcentaje());
        markup.setMarkupFijo(normalized.markupFijo());
        markup.setComisionAsesorPorcentaje(normalized.comisionAsesorPorcentaje());
        markup.setComisionAsesorFija(normalized.comisionAsesorFija());
        markup.setActivo(request.getActivo() == null || request.getActivo());
        revendedorMarkupRepository.save(markup);

        return construirPreviewItem(revendedorId, producto.getProductoCanonico(), producto.getId(), Optional.of(markup));
    }

    @Transactional(readOnly = true)
    public List<RevendedorPricingPreviewItemResponse> getPreview(UUID revendedorId) {
        getRevendedor(revendedorId);
        return productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc().stream()
                .map(canonico -> construirPreviewItem(revendedorId, canonico, null, Optional.empty()))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(RevendedorPricingPreviewItemResponse::getNombrePublico, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public PricingRuleResolution resolvePricingRule(UUID revendedorId, Producto producto) {
        Revendedor revendedor = getRevendedor(revendedorId);
        Optional<RevendedorMarkup> override = revendedorMarkupRepository
                .findByRevendedor_IdAndProducto_IdAndActivoTrue(revendedorId, producto.getId());

        BigDecimal markupPorcentaje = override.map(RevendedorMarkup::getMarkupPorcentaje).orElse(revendedor.getMarkupPorcentajeDefault());
        BigDecimal markupFijo = override.map(RevendedorMarkup::getMarkupFijo).orElse(revendedor.getMarkupFijoDefault());
        BigDecimal comisionAsesorPorcentaje = override.map(RevendedorMarkup::getComisionAsesorPorcentaje)
                .orElse(revendedor.getComisionAsesorPorcentajeDefault());
        BigDecimal comisionAsesorFija = override.map(RevendedorMarkup::getComisionAsesorFija)
                .orElse(revendedor.getComisionAsesorFijaDefault());

        return new PricingRuleResolution(
                nvl(markupPorcentaje),
                nvl(markupFijo),
                nvl(comisionAsesorPorcentaje),
                nvl(comisionAsesorFija),
                override.isPresent(),
                override.isPresent() ? "PRODUCTO" : "GLOBAL");
    }

    @Transactional(readOnly = true)
    public PreviewFinancials calcularPreview(BigDecimal costoOperativo, BigDecimal costoEnvio, boolean tieneSeguro,
            PricingRuleResolution resolution) {
        PricingService.PricingDetalle detalle = pricingService.calcularPrecioItem(
                costoOperativo,
                BigDecimal.ZERO,
                resolution.markupPorcentaje(),
                resolution.markupFijo(),
                BigDecimal.ZERO,
                tieneSeguro);
        BigDecimal comisionAsesorMonto = calcularComisionAsesor(
                detalle.margenRevendedor(),
                resolution.comisionAsesorPorcentaje(),
                resolution.comisionAsesorFija());
        BigDecimal gananciaNetaRevendedor = detalle.margenRevendedor().subtract(comisionAsesorMonto);
        return new PreviewFinancials(detalle, comisionAsesorMonto, gananciaNetaRevendedor);
    }

    private RevendedorPricingPreviewItemResponse construirPreviewItem(UUID revendedorId, ProductoCanonico canonico,
            UUID preferredProductoId, Optional<RevendedorMarkup> markupGuardado) {
        List<OfertaProductoResponse> ofertas = productoRepository.findByProductoCanonico_IdAndActivoTrue(canonico.getId()).stream()
                .map(producto -> buildOfertaPreview(producto, revendedorId))
                .filter(oferta -> oferta.getStockDisponible() != null && oferta.getStockDisponible() > 0)
                .sorted(Comparator
                        .comparing(OfertaProductoResponse::getPrecioFinalEstimado)
                        .thenComparing(OfertaProductoResponse::getDemoraEstimadaDias)
                        .thenComparing(OfertaProductoResponse::getStockDisponible, Comparator.reverseOrder()))
                .toList();
        if (ofertas.isEmpty()) {
            return null;
        }

        OfertaProductoResponse oferta = preferredProductoId != null
                ? ofertas.stream().filter(item -> preferredProductoId.equals(item.getProductoId())).findFirst().orElse(ofertas.get(0))
                : ofertas.get(0);
        Producto producto = productoRepository.findById(oferta.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + oferta.getProductoId()));

        BigDecimal costoOperativo = proveedorStockRepository
                .findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(producto.getProveedor().getId(), producto.getId())
                .map(stock -> stock.getPrecioCostoActual())
                .orElse(producto.getPrecioCosto());
        BigDecimal costoMd = costoOperativoMdService.getMontoParaCategoria(producto.getCategoriaPaquete(), costoOperativo);
        BigDecimal costoOperativoCompuesto = costoOperativo.add(costoMd);

        PricingRuleResolution resolution = resolvePricingRule(revendedorId, producto);
        PreviewFinancials preview = calcularPreview(costoOperativoCompuesto, BigDecimal.ZERO, false, resolution);

        RevendedorMarkup override = markupGuardado.orElse(null);
        return RevendedorPricingPreviewItemResponse.builder()
                .productoCanonicoId(canonico.getId())
                .productoBaseId(producto.getId())
                .nombrePublico(canonico.getNombrePublico())
                .costoOperativo(costoOperativoCompuesto)
                .markupPorcentaje(resolution.markupPorcentaje())
                .markupFijo(resolution.markupFijo())
                .comisionAsesorPorcentaje(resolution.comisionAsesorPorcentaje())
                .comisionAsesorFija(resolution.comisionAsesorFija())
                .markupBruto(preview.pricingDetalle().margenRevendedor())
                .comisionAsesorMonto(preview.comisionAsesorMonto())
                .gananciaNetaRevendedor(preview.gananciaNetaRevendedor())
                .precioFinalEstimado(preview.pricingDetalle().precioItemFinal())
                .demoraEstimadaDias(oferta.getDemoraEstimadaDias())
                .stockDisponible(oferta.getStockDisponible())
                .overrideActivo(override != null ? override.isActivo() : resolution.overrideActivo())
                .origenConfiguracion(resolution.origenConfiguracion())
                .imagenPrincipal(resolverImagenPrincipal(producto.getId()))
                .build();
    }

    private RevendedorPricingConfigResponse toConfigResponse(Revendedor revendedor) {
        return RevendedorPricingConfigResponse.builder()
                .revendedorId(revendedor.getId())
                .modoCatalogo(modoCatalogo.activo(revendedor.getId()))
                .markupPorcentaje(nvl(revendedor.getMarkupPorcentajeDefault()))
                .markupFijo(nvl(revendedor.getMarkupFijoDefault()))
                .comisionAsesorPorcentaje(nvl(revendedor.getComisionAsesorPorcentajeDefault()))
                .comisionAsesorFija(nvl(revendedor.getComisionAsesorFijaDefault()))
                .codigoDescuento(normalizarCodigoDescuento(revendedor.getCodigoDescuento()))
                .descuentoFijo(nvl(revendedor.getDescuentoFijo()))
                .build();
    }

    private Revendedor getRevendedor(UUID revendedorId) {
        return revendedorRepository.findById(revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId));
    }

    private String resolverImagenPrincipal(UUID productoId) {
        return productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(productoId)
                .map(media -> media.getUrl())
                .orElseGet(() -> productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId).stream()
                        .filter(media -> ar.com.mercadodigital.backend.entity.TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                        .map(media -> media.getUrl())
                        .findFirst()
                        .orElse(null));
    }

    private OfertaProductoResponse buildOfertaPreview(Producto producto, UUID revendedorId) {
        BigDecimal costoOperativo = proveedorStockRepository
                .findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(producto.getProveedor().getId(), producto.getId())
                .map(stock -> stock.getPrecioCostoActual())
                .orElse(producto.getPrecioCosto());
        BigDecimal costoMd = costoOperativoMdService.getMontoParaCategoria(producto.getCategoriaPaquete(), costoOperativo);
        CostoEnvioService.TarifaEnvio tarifa = costoEnvioService.resolveTarifa(producto.getCategoriaPaquete());
        BigDecimal costoEnvio = tarifa.monto();
        int demoraEstimadaDias = tarifa.demoraEstimadaDias() != null ? tarifa.demoraEstimadaDias() : DEMORA_DEFAULT;

        PricingRuleResolution resolution = resolvePricingRule(revendedorId, producto);
        PreviewFinancials preview = calcularPreview(costoOperativo.add(costoMd), costoEnvio, false, resolution);
        Integer stockDisponible = proveedorStockRepository
                .sumStockByProveedorIdAndProductoId(producto.getProveedor().getId(), producto.getId());

        return OfertaProductoResponse.builder()
                .productoId(producto.getId())
                .productoCanonicoId(producto.getProductoCanonico() != null ? producto.getProductoCanonico().getId() : null)
                .precioFinalEstimado(preview.pricingDetalle().precioItemFinal())
                .stockDisponible(stockDisponible != null ? stockDisponible : 0)
                .demoraEstimadaDias(demoraEstimadaDias)
                .categoriaPaquete(producto.getCategoriaPaquete())
                .build();
    }

    private PricingRuleConfig normalizeConfig(BigDecimal markupPorcentaje, BigDecimal markupFijo,
            BigDecimal comisionAsesorPorcentaje, BigDecimal comisionAsesorFija) {
        BigDecimal normalizedMarkupPorcentaje = normalizePercentage(markupPorcentaje, "markupPorcentaje");
        BigDecimal normalizedMarkupFijo = normalizeMoney(markupFijo, "markupFijo");
        BigDecimal normalizedComisionPorcentaje = normalizePercentage(comisionAsesorPorcentaje, "comisionAsesorPorcentaje");
        BigDecimal normalizedComisionFija = normalizeMoney(comisionAsesorFija, "comisionAsesorFija");

        if (normalizedMarkupPorcentaje.compareTo(BigDecimal.ZERO) > 0 && normalizedMarkupFijo.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("markupPorcentaje y markupFijo no pueden ser positivos al mismo tiempo");
        }
        if (normalizedComisionPorcentaje.compareTo(BigDecimal.ZERO) > 0 && normalizedComisionFija.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("comisionAsesorPorcentaje y comisionAsesorFija no pueden ser positivos al mismo tiempo");
        }

        return new PricingRuleConfig(
                normalizedMarkupPorcentaje,
                normalizedMarkupFijo,
                normalizedComisionPorcentaje,
                normalizedComisionFija);
    }

    private BigDecimal normalizePercentage(BigDecimal value, String fieldName) {
        BigDecimal normalized = nvl(value);
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(BigDecimal.ONE) > 0) {
            throw new BadRequestException(fieldName + " debe estar entre 0 y 1");
        }
        return normalized;
    }

    private BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
        BigDecimal normalized = nvl(value);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(fieldName + " no puede ser negativo");
        }
        return normalized;
    }

    private String normalizarCodigoDescuento(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.isBlank() ? null : normalized;
    }

    private BigDecimal calcularComisionAsesor(BigDecimal markupBruto, BigDecimal comisionPorcentaje, BigDecimal comisionFija) {
        if (markupBruto == null || markupBruto.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monto = BigDecimal.ZERO;
        if (comisionPorcentaje != null && comisionPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
            monto = markupBruto.multiply(comisionPorcentaje);
        } else if (comisionFija != null && comisionFija.compareTo(BigDecimal.ZERO) > 0) {
            monto = comisionFija;
        }
        return monto.min(markupBruto);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record PricingRuleConfig(
            BigDecimal markupPorcentaje,
            BigDecimal markupFijo,
            BigDecimal comisionAsesorPorcentaje,
            BigDecimal comisionAsesorFija) {
    }

    public record PricingRuleResolution(
            BigDecimal markupPorcentaje,
            BigDecimal markupFijo,
            BigDecimal comisionAsesorPorcentaje,
            BigDecimal comisionAsesorFija,
            boolean overrideActivo,
            String origenConfiguracion) {
    }

    public record PreviewFinancials(
            PricingService.PricingDetalle pricingDetalle,
            BigDecimal comisionAsesorMonto,
            BigDecimal gananciaNetaRevendedor) {
    }
}
