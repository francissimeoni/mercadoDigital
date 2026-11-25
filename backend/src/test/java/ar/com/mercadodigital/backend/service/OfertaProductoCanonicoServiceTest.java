package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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

import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;

@ExtendWith(MockitoExtension.class)
class OfertaProductoCanonicoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProveedorStockRepository proveedorStockRepository;
    @Mock
    private PricingService pricingService;
    @Mock
    private RevendedorPricingV1Service revendedorPricingV1Service;
    @Mock
    private CostoOperativoMdService costoOperativoMdService;
    @Mock
    private CostoEnvioService costoEnvioService;

    @InjectMocks
    private OfertaProductoCanonicoService ofertaProductoCanonicoService;

    @Test
    void ordenaPorPrecioDemoraYStock() {
        UUID canonicoId = UUID.randomUUID();
        UUID revendedorId = UUID.randomUUID();

        Producto ofertaA = buildProducto(canonicoId, new BigDecimal("1000"), "CHICO");
        Producto ofertaB = buildProducto(canonicoId, new BigDecimal("2000"), "MEDIANO");
        Producto ofertaC = buildProducto(canonicoId, new BigDecimal("3000"), "VOLUMINOSO");

        when(productoRepository.findByProductoCanonico_IdAndActivoTrue(canonicoId))
                .thenReturn(List.of(ofertaA, ofertaB, ofertaC));

        when(proveedorStockRepository.findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(revendedorPricingV1Service.resolvePricingRule(any(), any()))
                .thenReturn(new RevendedorPricingV1Service.PricingRuleResolution(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, "GLOBAL"));
        when(costoOperativoMdService.getMontoParaCategoria(anyString(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);

        when(costoEnvioService.resolveTarifa("CHICO"))
                .thenReturn(new CostoEnvioService.TarifaEnvio("CHICO", new BigDecimal("0"), 2));
        when(costoEnvioService.resolveTarifa("MEDIANO"))
                .thenReturn(new CostoEnvioService.TarifaEnvio("MEDIANO", new BigDecimal("0"), 1));
        when(costoEnvioService.resolveTarifa("VOLUMINOSO"))
                .thenReturn(new CostoEnvioService.TarifaEnvio("VOLUMINOSO", new BigDecimal("0"), 4));

        when(pricingService.calcularPrecioItem(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0"), false))
                .thenReturn(new PricingService.PricingDetalle(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("1000")));
        when(pricingService.calcularPrecioItem(new BigDecimal("2000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0"), false))
                .thenReturn(new PricingService.PricingDetalle(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("2000")));
        when(pricingService.calcularPrecioItem(new BigDecimal("3000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0"), false))
                .thenReturn(new PricingService.PricingDetalle(new BigDecimal("900"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("3000")));

        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(ofertaA.getProveedor().getId(), ofertaA.getId()))
                .thenReturn(5);
        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(ofertaB.getProveedor().getId(), ofertaB.getId()))
                .thenReturn(30);
        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(ofertaC.getProveedor().getId(), ofertaC.getId()))
                .thenReturn(2);

        List<OfertaProductoResponse> ofertas = ofertaProductoCanonicoService.getOfertas(canonicoId, revendedorId);

        assertEquals(ofertaC.getId(), ofertas.get(0).getProductoId());
        assertEquals(ofertaB.getId(), ofertas.get(1).getProductoId());
        assertEquals(ofertaA.getId(), ofertas.get(2).getProductoId());
    }

    @Test
    void usaDemoraDefaultCincoSiNoHayTarifa() {
        UUID canonicoId = UUID.randomUUID();
        UUID revendedorId = UUID.randomUUID();
        Producto oferta = buildProducto(canonicoId, new BigDecimal("1000"), "CHICO");

        when(productoRepository.findByProductoCanonico_IdAndActivoTrue(canonicoId))
                .thenReturn(List.of(oferta));
        when(proveedorStockRepository.findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(revendedorPricingV1Service.resolvePricingRule(any(), any()))
                .thenReturn(new RevendedorPricingV1Service.PricingRuleResolution(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, "GLOBAL"));
        when(costoOperativoMdService.getMontoParaCategoria(anyString(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
        when(costoEnvioService.resolveTarifa(anyString()))
                .thenReturn(new CostoEnvioService.TarifaEnvio("CHICO", BigDecimal.ZERO, 5));
        when(pricingService.calcularPrecioItem(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new PricingService.PricingDetalle(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("1000")));
        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(oferta.getProveedor().getId(), oferta.getId()))
                .thenReturn(0);

        List<OfertaProductoResponse> ofertas = ofertaProductoCanonicoService.getOfertas(canonicoId, revendedorId);

        assertEquals(5, ofertas.get(0).getDemoraEstimadaDias());
    }

    private Producto buildProducto(UUID canonicoId, BigDecimal costo, String categoria) {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Producto");
        producto.setMoneda("ARS");
        producto.setPrecioCosto(costo);
        producto.setCategoriaPaquete(categoria);
        producto.setActivo(true);

        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        proveedor.setNombre("Proveedor");
        proveedor.setActivo(true);
        producto.setProveedor(proveedor);

        ProductoCanonico canonico = new ProductoCanonico();
        canonico.setId(canonicoId);
        canonico.setActivo(true);
        canonico.setMarca("Marca");
        canonico.setModelo("Modelo");
        canonico.setNombrePublico("Nombre");
        producto.setProductoCanonico(canonico);
        return producto;
    }
}
