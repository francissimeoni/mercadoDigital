package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.PublicCatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;

@ExtendWith(MockitoExtension.class)
class PublicCatalogoServiceTest {
    @Mock private CatalogoPublicoService catalogoIndependiente;
    @Mock private CatalogoModoService modoCatalogo;

    @Mock
    private StorefrontService storefrontService;

    @Mock
    private CatalogoService catalogoService;
    @Mock
    private RevendedorProductoRepository productoPropioRepository;
    @Mock
    private CostoEnvioService costoEnvioService;
    @Mock
    private PspConfigService pspConfigService;

    @InjectMocks
    private PublicCatalogoService publicCatalogoService;

    @Test
    void listadoPublicoExponeSoloCatalogoPropio() {
        UUID revendedorId = UUID.randomUUID();
        RevendedorProducto propio = productoPropio("Producto propio");
        doNothing().when(storefrontService).validarRevendedorPublico(revendedorId);
        when(productoPropioRepository
                .findByRevendedor_IdAndActivoTrueAndStockGreaterThanOrderByNombreAsc(revendedorId, 0))
                .thenReturn(List.of(propio));
        when(costoEnvioService.resolveTarifa("MEDIANO"))
                .thenReturn(new CostoEnvioService.TarifaEnvio("MEDIANO", BigDecimal.ZERO, 5));

        List<PublicCatalogoItemResponse> result = publicCatalogoService.getCatalogo(revendedorId);

        assertEquals(1, result.size());
        assertEquals("Producto propio", result.get(0).getNombrePublico());
        assertEquals("Otros", result.get(0).getCategoriaProducto());
        assertEquals(new BigDecimal("2500"), result.get(0).getMejorPrecioFinalEstimado());
        verify(catalogoService, org.mockito.Mockito.never()).getCatalogoPublico(revendedorId);
        verify(storefrontService).validarRevendedorPublico(revendedorId);
    }

    @Test
    void detallePublicoExponeProductoPropio() {
        UUID revendedorId = UUID.randomUUID();
        RevendedorProducto propio = productoPropio("Producto propio");
        doNothing().when(storefrontService).validarRevendedorPublico(revendedorId);
        when(productoPropioRepository.findByIdAndRevendedor_Id(propio.getId(), revendedorId))
                .thenReturn(java.util.Optional.of(propio));
        when(pspConfigService.getSeguroTransporteFactor()).thenReturn(new BigDecimal("0.01"));
        when(costoEnvioService.resolveTarifa("MEDIANO"))
                .thenReturn(new CostoEnvioService.TarifaEnvio("MEDIANO", BigDecimal.ZERO, 5));

        PublicProductoDetalleResponse result = publicCatalogoService.getProductoDetalle(propio.getId(), revendedorId);

        assertEquals(propio.getId(), result.getProductoId());
        assertEquals("Descripcion propia", result.getDescripcion());
        assertEquals(new BigDecimal("25.00"), result.getSeguroTransporteEstimado());
        verify(storefrontService).validarRevendedorPublico(revendedorId);
    }

    @Test
    void revendedorNoDisponiblePropagaNotFound() {
        UUID revendedorId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Storefront no disponible para host: " + revendedorId))
                .when(storefrontService).validarRevendedorPublico(revendedorId);

        assertThrows(ResourceNotFoundException.class, () -> publicCatalogoService.getCatalogo(revendedorId));
    }

    @Test
    void productoNoVisiblePropagaNotFound() {
        UUID revendedorId = UUID.randomUUID();
        UUID productoCanonicoId = UUID.randomUUID();
        doNothing().when(storefrontService).validarRevendedorPublico(revendedorId);
        when(productoPropioRepository.findByIdAndRevendedor_Id(productoCanonicoId, revendedorId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> publicCatalogoService.getProductoDetalle(productoCanonicoId, revendedorId));
    }

    @Test
    void sincronizacionPausadaExponeExclusivamenteProductosPropios() {
        UUID revendedorId = UUID.randomUUID();
        RevendedorProducto propio = productoPropio("Producto propio");

        doNothing().when(storefrontService).validarRevendedorPublico(revendedorId);
        when(productoPropioRepository
                .findByRevendedor_IdAndActivoTrueAndStockGreaterThanOrderByNombreAsc(revendedorId, 0))
                .thenReturn(List.of(propio));
        when(costoEnvioService.resolveTarifa("MEDIANO"))
                .thenReturn(new CostoEnvioService.TarifaEnvio("MEDIANO", BigDecimal.ZERO, 5));

        List<PublicCatalogoItemResponse> result = publicCatalogoService.getCatalogo(revendedorId);

        assertEquals(1, result.size());
        assertEquals("PROPIO_REVENDEDOR", result.get(0).getOrigen());
        assertEquals(propio.getId(), result.get(0).getProductoId());
        verify(catalogoService, org.mockito.Mockito.never()).getCatalogoPublico(revendedorId);
    }

    @Test
    void catalogoIndependienteNoPublicaCategoriaLogisticaNiModificaImportes() {
        UUID revendedorId = UUID.randomUUID();
        RevendedorProducto producto = productoPropio("Sin categoría comercial");
        when(modoCatalogo.activo(revendedorId)).thenReturn(true);
        when(catalogoIndependiente.listado(revendedorId)).thenReturn(List.of(
                new CatalogoPublicoService.Publicacion(producto, new BigDecimal("123456.78"), "Coordinar")));
        var respuesta = publicCatalogoService.getCatalogo(revendedorId).get(0);
        assertEquals("Otros", respuesta.getCategoriaProducto());
        assertEquals("123456.78", respuesta.getPrecioPublicoArs());
        assertEquals("MEDIANO", producto.getCategoriaPaquete());
        org.mockito.Mockito.verifyNoInteractions(costoEnvioService);
    }

    @Test
    void catalogoIndependienteConservaCategoriaComercialConfigurada() {
        UUID revendedorId = UUID.randomUUID();
        RevendedorProducto producto = productoPropio("Con categoría comercial");
        var categoria = new ar.com.mercadodigital.backend.entity.CategoriaProducto();
        categoria.setNombre("Telefonía");
        producto.setCategoriaProducto(categoria);
        when(modoCatalogo.activo(revendedorId)).thenReturn(true);
        when(catalogoIndependiente.listado(revendedorId)).thenReturn(List.of(
                new CatalogoPublicoService.Publicacion(producto, new BigDecimal("2500.00"), null)));
        assertEquals("Telefonía", publicCatalogoService.getCatalogo(revendedorId).get(0).getCategoriaProducto());
    }

    private RevendedorProducto productoPropio(String nombre) {
        RevendedorProducto propio = new RevendedorProducto();
        propio.setId(UUID.randomUUID());
        propio.setNombre(nombre);
        propio.setDescripcion("Descripcion propia");
        propio.setPrecioVenta(new BigDecimal("2500"));
        propio.setStock(3);
        propio.setCategoriaPaquete("MEDIANO");
        propio.setActivo(true);
        return propio;
    }
}
