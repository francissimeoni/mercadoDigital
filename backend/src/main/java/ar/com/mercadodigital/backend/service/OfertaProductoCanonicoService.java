package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OfertaProductoCanonicoService {

    private static final int DEMORA_DEFAULT = 5;
    private static final String MONEDA_SOPORTADA = "ARS";

    private final ProductoRepository productoRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final PricingService pricingService;
    private final RevendedorPricingV1Service revendedorPricingV1Service;
    private final CostoOperativoMdService costoOperativoMdService;
    private final CostoEnvioService costoEnvioService;

    public enum ModoOferta {
        INTERNO_COSTO,
        PUBLICO_COMERCIAL
    }

    @Transactional(readOnly = true)
    public List<OfertaProductoResponse> getOfertas(UUID productoCanonicoId, UUID revendedorId) {
        return getOfertasOrdenadas(productoCanonicoId, revendedorId, ModoOferta.PUBLICO_COMERCIAL, false);
    }

    @Transactional(readOnly = true)
    public List<OfertaProductoResponse> getOfertasConStock(UUID productoCanonicoId, UUID revendedorId) {
        return getOfertasConStock(productoCanonicoId, revendedorId, false);
    }

    @Transactional(readOnly = true)
    public List<OfertaProductoResponse> getOfertasConStock(UUID productoCanonicoId, UUID revendedorId, boolean tieneSeguro) {
        return getOfertasOrdenadas(productoCanonicoId, revendedorId, ModoOferta.PUBLICO_COMERCIAL, tieneSeguro).stream()
                .filter(oferta -> oferta.getStockDisponible() != null && oferta.getStockDisponible() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfertaProductoResponse> getOfertasInternasConStock(UUID productoCanonicoId, UUID revendedorId) {
        return getOfertasOrdenadas(productoCanonicoId, revendedorId, ModoOferta.INTERNO_COSTO, false).stream()
                .filter(oferta -> oferta.getStockDisponible() != null && oferta.getStockDisponible() > 0)
                .toList();
    }

    private List<OfertaProductoResponse> getOfertasOrdenadas(UUID productoCanonicoId, UUID revendedorId, ModoOferta modoOferta, boolean tieneSeguro) {
        return productoRepository.findByProductoCanonico_IdAndActivoTrue(productoCanonicoId).stream()
                .map(producto -> buildOferta(producto, revendedorId, modoOferta, tieneSeguro))
                .sorted(Comparator
                        .comparing(OfertaProductoResponse::getPrecioFinalEstimado)
                        .thenComparing(OfertaProductoResponse::getDemoraEstimadaDias)
                        .thenComparing(OfertaProductoResponse::getStockDisponible, Comparator.reverseOrder()))
                .toList();
    }

    private OfertaProductoResponse buildOferta(Producto producto, UUID revendedorId, ModoOferta modoOferta, boolean tieneSeguro) {
        validarMoneda(producto.getMoneda());

        ProveedorStock stockActual = proveedorStockRepository
                .findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(producto.getProveedor().getId(), producto.getId())
                .orElse(null);
        CostoEnvioService.TarifaEnvio tarifa = costoEnvioService.resolveTarifa(producto.getCategoriaPaquete());

        BigDecimal costoProveedor = stockActual != null ? stockActual.getPrecioCostoActual() : producto.getPrecioCosto();
        RevendedorPricingV1Service.PricingRuleResolution pricingRule = revendedorPricingV1Service
                .resolvePricingRule(revendedorId, producto);
        BigDecimal costoEnvio = tarifa.monto();
        BigDecimal costoMd = costoOperativoMdService.getMontoParaCategoria(producto.getCategoriaPaquete(), costoProveedor);

        PricingService.PricingDetalle detalle = ModoOferta.INTERNO_COSTO.equals(modoOferta)
                ? pricingService.calcularCostoOperativoItem(costoProveedor, costoMd, BigDecimal.ZERO, tieneSeguro)
                : pricingService.calcularPrecioItem(
                        costoProveedor,
                        costoMd,
                        pricingRule.markupPorcentaje(),
                        pricingRule.markupFijo(),
                        BigDecimal.ZERO,
                        tieneSeguro);

        Integer stockDisponible = proveedorStockRepository
                .sumStockByProveedorIdAndProductoId(producto.getProveedor().getId(), producto.getId());
        int demoraEstimadaDias = tarifa.demoraEstimadaDias() != null ? tarifa.demoraEstimadaDias() : DEMORA_DEFAULT;

        return OfertaProductoResponse.builder()
                .productoId(producto.getId())
                .productoCanonicoId(producto.getProductoCanonico() != null ? producto.getProductoCanonico().getId() : null)
                .precioFinalEstimado(detalle.precioItemFinal())
                .montoSeguroEstimado(detalle.montoSeguro())
                .stockDisponible(stockDisponible != null ? stockDisponible : 0)
                .demoraEstimadaDias(demoraEstimadaDias)
                .categoriaPaquete(producto.getCategoriaPaquete())
                .build();
    }

    private void validarMoneda(String moneda) {
        if (moneda == null || !MONEDA_SOPORTADA.equalsIgnoreCase(moneda)) {
            throw new BadRequestException("Moneda no soportada. Actualmente solo ARS.");
        }
    }
}
