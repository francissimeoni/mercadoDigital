package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static ar.com.mercadodigital.backend.dto.CatalogoAdminDto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.exception.*;

@ExtendWith(MockitoExtension.class)
class CatalogoAdminServiceTest {
    @Mock RevendedorOwnershipService ownership;
    @Mock RevendedorStorefrontRepository storefronts;
    @Mock RevendedorProductoRepository productos;
    @Mock CatalogoConfiguracionRepository configuraciones;
    @Mock CatalogoCategoriaEnvioRepository categorias;
    @Mock CatalogoProductoConfiguracionRepository precios;
    @Mock AuditoriaEventoRepository auditoria;
    CatalogoAdminService service;
    UUID tienda = UUID.randomUUID(), productoId = UUID.randomUUID();
    Revendedor revendedor;
    RevendedorStorefront storefront;
    RevendedorProducto producto;
    CatalogoConfiguracion config;
    List<CatalogoProductoConfiguracion> preparados;

    @BeforeEach void setup() {
        service = new CatalogoAdminService(ownership, storefronts, productos, configuraciones, categorias,
                precios, new CatalogoPrecioCalculator(), auditoria, new ObjectMapper().findAndRegisterModules());
        revendedor = new Revendedor(); revendedor.setId(tienda);
        storefront = new RevendedorStorefront(); storefront.setRevendedor(revendedor);
        storefront.setTelefonoComercial("5491112345678");
        producto = new RevendedorProducto(); producto.setId(productoId); producto.setRevendedor(revendedor);
        producto.setNombre("Producto"); producto.setSku("SKU-1"); producto.setStock(0); producto.setPrecioVenta(new BigDecimal("1000"));
        config = new CatalogoConfiguracion(); config.setRevendedor(revendedor); config.setId(UUID.randomUUID()); config.setCotizacionUsd(new BigDecimal("1200"));
        preparados = new ArrayList<>();
        lenient().when(storefronts.findByRevendedor_Id(tienda)).thenReturn(Optional.of(storefront));
        lenient().when(storefronts.findForCatalogoUpdate(tienda)).thenReturn(Optional.of(storefront));
        lenient().when(productos.findByIdAndRevendedor_Id(productoId, tienda)).thenReturn(Optional.of(producto));
        lenient().when(configuraciones.findByRevendedor_Id(tienda)).thenReturn(Optional.of(config));
        lenient().when(precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(tienda)).thenReturn(preparados);
        lenient().when(precios.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(configuraciones.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(categorias.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ProductoInput input(String moneda, boolean incluido, UUID categoria) {
        return new ProductoInput(moneda, new BigDecimal("100"), incluido, categoria, -1L, null);
    }
    private ProductoInput confirmado(ProductoInput input) {
        return new ProductoInput(input.moneda(), input.precioOriginal(), input.incluyeEnvio(), input.categoriaEnvioId(), input.version(),
                service.previewProducto(tienda, productoId, input).token());
    }
    private CatalogoProductoConfiguracion preparado(String moneda, boolean incluido, CatalogoCategoriaEnvio categoria) {
        var p = new CatalogoProductoConfiguracion(); p.setProducto(producto); p.setMoneda(moneda);
        p.setPrecioOriginal(new BigDecimal("100")); p.setIncluyeEnvio(incluido); p.setCategoriaEnvio(categoria); preparados.add(p); return p;
    }
    private CatalogoCategoriaEnvio categoria() {
        var c = new CatalogoCategoriaEnvio(); c.setId(UUID.randomUUID()); c.setRevendedor(revendedor); c.setNombre("Mediano"); c.setCostoArs(new BigDecimal("8000"));
        lenient().when(categorias.findByIdAndRevendedor_Id(c.getId(), tienda)).thenReturn(Optional.of(c)); return c;
    }

    @Test void incrementoDeCategoriaSeReflejaAlPublicarSoloLosProductosVinculadosDeEsaTienda() {
        var envioA = categoria();
        var conEnvio = preparado("USD", false, envioA);
        var incluido = preparado("USD", true, null);
        var otroProducto = new RevendedorProducto(); otroProducto.setId(UUID.randomUUID()); otroProducto.setRevendedor(revendedor);
        otroProducto.setNombre("Incluido"); incluido.setProducto(otroProducto);
        var otraTienda = new Revendedor(); otraTienda.setId(UUID.randomUUID());
        var envioB = new CatalogoCategoriaEnvio(); envioB.setId(UUID.randomUUID()); envioB.setNombre(envioA.getNombre());
        envioB.setRevendedor(otraTienda); envioB.setCostoArs(new BigDecimal("8000"));
        var otro = new RevendedorProducto(); otro.setId(UUID.randomUUID()); otro.setRevendedor(otraTienda);
        var precioB = new CatalogoProductoConfiguracion(); precioB.setProducto(otro); precioB.setMoneda("USD");
        precioB.setPrecioOriginal(new BigDecimal("100")); precioB.setCategoriaEnvio(envioB); precioB.setIncluyeEnvio(false);
        var configB = new CatalogoConfiguracion(); configB.setCotizacionUsd(new BigDecimal("1200"));
        when(configuraciones.findByRevendedor_Id(otraTienda.getId())).thenReturn(Optional.of(configB));
        when(precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(otraTienda.getId())).thenReturn(List.of(precioB));
        var publico = new CatalogoPublicoService(configuraciones, precios, storefronts, mock(AsesorVentaRepository.class), new CatalogoPrecioCalculator());
        assertEquals(new BigDecimal("128000.00"), publico.listado(tienda).get(0).precioArs());
        var cambio = new CategoriaInput(envioA.getNombre(), new BigDecimal("10000"), 0L, null);
        var preview = service.previewCategoria(tienda, envioA.getId(), cambio);
        assertEquals(1, preview.productos().size()); assertEquals(new BigDecimal("130000.00"), preview.productos().get(0).propuesto().precioPublicoArs());
        service.actualizarCategoria(tienda, envioA.getId(), new CategoriaInput(cambio.nombre(), cambio.costoArs(), cambio.version(), preview.token()));
        assertEquals(new BigDecimal("130000.00"), publico.listado(tienda).get(0).precioArs());
        assertEquals(new BigDecimal("120000.00"), publico.listado(tienda).get(1).precioArs());
        assertEquals(new BigDecimal("128000.00"), publico.listado(otraTienda.getId()).get(0).precioArs());
        assertEquals(new BigDecimal("100"), conEnvio.getPrecioOriginal()); assertEquals(new BigDecimal("1200"), config.getCotizacionUsd());
        assertEquals(new BigDecimal("1000"), producto.getPrecioVenta()); verify(productos, never()).save(any());
    }

    @Test void guardaUsdSinModificarPrecioLegadoNiStock() {
        var result = service.guardarProducto(tienda, productoId, confirmado(input("USD", true, null)));
        assertEquals(new BigDecimal("120000.00"), result.calculo().precioPublicoArs());
        assertEquals(new BigDecimal("1000"), producto.getPrecioVenta()); assertEquals("ARS", producto.getMoneda()); assertEquals(0, producto.getStock());
        verify(productos, never()).save(any()); verify(auditoria).save(any());
    }
    @Test void incluidoDescartaCategoriaAnteriorInclusoSiLlegaEnLaPeticion() {
        service.guardarProducto(tienda, productoId, confirmado(input("ARS", true, UUID.randomUUID())));
        ArgumentCaptor<CatalogoProductoConfiguracion> saved = ArgumentCaptor.forClass(CatalogoProductoConfiguracion.class);
        verify(precios).saveAndFlush(saved.capture()); assertNull(saved.getValue().getCategoriaEnvio());
        verifyNoInteractions(categorias);
    }
    @Test void cambioDeDolarEntrePreviewYGuardarExigeRevisar() {
        var input = confirmado(input("USD", true, null)); config.setCotizacionUsd(new BigDecimal("1300"));
        assertThrows(OperacionEnConflictoException.class, () -> service.guardarProducto(tienda, productoId, input));
        verify(precios, never()).saveAndFlush(any());
    }
    @Test void rechazaEnvioFaltante() {
        assertThrows(BadRequestException.class, () -> service.guardarProducto(tienda, productoId, input("ARS", false, null)));
        verify(precios, never()).saveAndFlush(any());
    }
    @Test void rechazaCategoriaDeOtraTienda() {
        assertThrows(ResourceNotFoundException.class, () -> service.previewProducto(tienda, productoId, input("ARS", false, UUID.randomUUID())));
    }
    @Test void rechazaProductoDeOtraTienda() {
        assertThrows(ResourceNotFoundException.class, () -> service.previewProducto(tienda, UUID.randomUUID(), input("ARS", true, null)));
    }
    @Test void ownershipSeVerificaAntesDeConsultar() {
        doThrow(new AccessDeniedException("otra tienda")).when(ownership).validarOwnership(tienda);
        assertThrows(AccessDeniedException.class, () -> service.productos(tienda));
        assertThrows(AccessDeniedException.class, () -> service.guardarProducto(tienda, productoId, input("ARS", true, null)));
        verifyNoInteractions(storefronts, productos, configuraciones, precios, categorias, auditoria);
    }
    @Test void listaProductoSinStockConConfiguracionPendiente() {
        when(productos.findByRevendedor_IdOrderByUpdatedAtDesc(tienda)).thenReturn(List.of(producto));
        var result = service.productos(tienda).get(0);
        assertEquals("PENDIENTE", result.calculo().estado()); assertNull(result.incluyeEnvio());
        assertNull(result.precioOriginal()); assertEquals(-1, result.version());
        verifyNoInteractions(auditoria); verify(precios, never()).saveAndFlush(any());
    }
    @Test void previewNoEscribeNada() {
        service.previewProducto(tienda, productoId, input("USD", true, null));
        verify(precios, never()).saveAndFlush(any()); verify(productos, never()).save(any());
        verify(storefronts, never()).save(any()); verifyNoInteractions(auditoria);
        assertEquals(0, producto.getStock());
    }
    @Test void cotizacionSoloAfectaProductosUsdDeLaTienda() {
        preparado("USD", true, null); preparado("ARS", true, null);
        var input = new ConfigInput(new BigDecimal("1300"), "Zona local", "+54 9 11 1234 5678", 0L, null);
        var impacto = service.previewConfig(tienda, input);
        assertEquals(1, impacto.productos().size()); assertEquals(new BigDecimal("130000.00"), impacto.productos().get(0).propuesto().precioPublicoArs());
        service.guardarConfig(tienda, new ConfigInput(input.cotizacionUsd(), input.condicionesEntrega(), input.telefonoComercial(), 0L, impacto.token()));
        assertEquals(new BigDecimal("1300"), config.getCotizacionUsd());
        assertEquals(new BigDecimal("100"), preparados.get(0).getPrecioOriginal()); assertEquals("+5491112345678", storefront.getTelefonoComercial());
        verify(precios, never()).saveAndFlush(any()); verify(productos, never()).save(any());
    }
    @Test void noPermiteQuitarDolarUsado() {
        preparado("USD", true, null);
        assertThrows(BadRequestException.class, () -> service.previewConfig(tienda, new ConfigInput(null, "", "", 0L, null)));
    }
    @Test void categoriaActualizaSoloVinculadosSinEnvioIncluido() {
        var categoria = categoria(); preparado("ARS", false, categoria); preparado("ARS", true, categoria);
        var input = new CategoriaInput("Mediano", new BigDecimal("9000"), 0L, null);
        var impact = service.previewCategoria(tienda, categoria.getId(), input);
        assertEquals(1, impact.productos().size()); assertEquals(new BigDecimal("9100.00"), impact.productos().get(0).propuesto().precioPublicoArs());
        service.actualizarCategoria(tienda, categoria.getId(), new CategoriaInput(input.nombre(), input.costoArs(), 0L, impact.token()));
        assertEquals(new BigDecimal("9000"), categoria.getCostoArs()); verify(precios, never()).saveAndFlush(any());
    }
    @Test void bloqueaEliminarCategoriaEnUso() {
        var categoria = categoria(); when(precios.existsByCategoriaEnvio_Id(categoria.getId())).thenReturn(true);
        assertThrows(OperacionEnConflictoException.class, () -> service.eliminarCategoria(tienda, categoria.getId(), 0));
        verify(categorias, never()).delete(any());
    }
    @Test void tokenDeImpactoDetectaNuevoProductoAfectado() {
        var input = new ConfigInput(new BigDecimal("1300"), "", "", 0L, null);
        String token = service.previewConfig(tienda, input).token(); preparado("USD", true, null);
        assertThrows(OperacionEnConflictoException.class, () -> service.guardarConfig(tienda, new ConfigInput(input.cotizacionUsd(), "", "", 0L, token)));
    }
    @Test void versionObsoletaNoSobrescribe() {
        assertThrows(OperacionEnConflictoException.class, () -> service.previewConfig(tienda, new ConfigInput(BigDecimal.ONE, "", "", 2L, null)));
    }
    @Test void tiendaSinConfigMantienePendientesSinInventarCotizacion() {
        when(configuraciones.findByRevendedor_Id(tienda)).thenReturn(Optional.empty()); storefront.setTelefonoComercial(null);
        var result = service.configuracion(tienda);
        assertNull(result.cotizacionUsd()); assertNull(result.telefonoComercial()); assertEquals(-1, result.version());
        assertEquals("PREPARACION", result.modo()); assertEquals(3, result.pendientes().size());
    }
}
