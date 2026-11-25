package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.ProductoResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.entity.RevendedorMarkup;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.mapper.ProductoMapper;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorMarkupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private static final int DEMORA_DEFAULT = 5;
    private static final String MONEDA_SOPORTADA = "ARS";

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final ProveedorStockRepository proveedorStockRepository;
    private final RevendedorMarkupRepository revendedorMarkupRepository;
    private final PricingService pricingService;
    private final CostoOperativoMdService costoOperativoMdService;
    private final CostoEnvioService costoEnvioService;

    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductos() {
        return productoRepository.findAll().stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        return toPublicResponse(producto);
    }

    @Transactional(readOnly = true)
    public Producto getEntity(UUID id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private ProductoResponse toPublicResponse(Producto producto) {
        validarMoneda(producto.getMoneda());

        ProveedorStock stockActual = proveedorStockRepository
                .findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(producto.getProveedor().getId(), producto.getId())
                .orElse(null);
        RevendedorMarkup markup = revendedorMarkupRepository
                .findFirstByProducto_IdAndActivoTrueOrderByUpdatedAtDesc(producto.getId())
                .orElse(null);
        CostoEnvioService.TarifaEnvio tarifa = costoEnvioService.resolveTarifa(producto.getCategoriaPaquete());

        BigDecimal costoProveedor = stockActual != null ? stockActual.getPrecioCostoActual() : producto.getPrecioCosto();
        BigDecimal markupPorcentaje = markup != null ? markup.getMarkupPorcentaje() : null;
        BigDecimal markupFijo = markup != null ? markup.getMarkupFijo() : null;
        BigDecimal costoEnvio = tarifa.monto();
        BigDecimal costoMd = costoOperativoMdService.getMontoParaCategoria(producto.getCategoriaPaquete(), costoProveedor);

        PricingService.PricingDetalle detalle = pricingService.calcularPrecioItem(
                costoProveedor,
                costoMd,
                markupPorcentaje,
                markupFijo,
                BigDecimal.ZERO,
                false);

        Integer stockDisponible = proveedorStockRepository.sumStockByProductoId(producto.getId());
        Integer demoraEstimadaDias = tarifa.demoraEstimadaDias() != null ? tarifa.demoraEstimadaDias() : DEMORA_DEFAULT;

        return productoMapper.toResponse(
                producto,
                detalle.precioItemFinal(),
                stockDisponible != null ? stockDisponible : 0,
                demoraEstimadaDias);
    }

    private void validarMoneda(String moneda) {
        if (moneda == null || !MONEDA_SOPORTADA.equalsIgnoreCase(moneda)) {
            throw new BadRequestException("Moneda no soportada. Actualmente solo ARS.");
        }
    }
}
