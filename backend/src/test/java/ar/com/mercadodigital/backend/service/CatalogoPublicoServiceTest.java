package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

class CatalogoPublicoServiceTest {
    UUID tienda = UUID.randomUUID();
    CatalogoConfiguracionRepository configuraciones = mock(CatalogoConfiguracionRepository.class);
    CatalogoProductoConfiguracionRepository precios = mock(CatalogoProductoConfiguracionRepository.class);
    RevendedorStorefrontRepository storefronts = mock(RevendedorStorefrontRepository.class);
    AsesorVentaRepository asesores = mock(AsesorVentaRepository.class);
    CatalogoPublicoService service = new CatalogoPublicoService(configuraciones, precios, storefronts, asesores, new CatalogoPrecioCalculator());
    CatalogoProductoConfiguracion precio;
    CatalogoConfiguracion config;
    Revendedor revendedor;

    @BeforeEach void preparar() {
        revendedor = new Revendedor(); revendedor.setId(tienda);
        var producto = new RevendedorProducto(); producto.setId(UUID.randomUUID()); producto.setRevendedor(revendedor);
        producto.setNombre("Cafetera & café"); producto.setSku("C-01"); producto.setActivo(true); producto.setStock(0);
        producto.setPrecioVenta(new BigDecimal("999"));
        precio = new CatalogoProductoConfiguracion(); precio.setProducto(producto); precio.setMoneda("ARS");
        precio.setPrecioOriginal(new BigDecimal("100000")); precio.setIncluyeEnvio(true);
        config = new CatalogoConfiguracion(); config.setRevendedor(revendedor); config.setCotizacionUsd(new BigDecimal("1200"));
        config.setCondicionesEntrega("Coordinar entrega con la tienda.");
        when(configuraciones.findByRevendedor_Id(tienda)).thenReturn(Optional.of(config));
        when(precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(tienda)).thenReturn(List.of(precio));
        when(precios.findByProducto_IdAndProducto_Revendedor_Id(precio.getProducto().getId(), tienda)).thenReturn(Optional.of(precio));
    }

    @ParameterizedTest @CsvSource({"ARS,100000,true,100000.00", "ARS,100000,false,108000.00", "USD,100,true,120000.00", "USD,100,false,128000.00"})
    void listadoYDetalleUsanLaMismaFormulaSinStock(String moneda, String original, boolean incluido, String esperado) {
        precio.setMoneda(moneda); precio.setPrecioOriginal(new BigDecimal(original)); precio.setIncluyeEnvio(incluido);
        var categoria = new CatalogoCategoriaEnvio(); categoria.setRevendedor(revendedor); categoria.setCostoArs(new BigDecimal("8000")); precio.setCategoriaEnvio(categoria);
        assertEquals(new BigDecimal(esperado), service.listado(tienda).get(0).precioArs());
        assertEquals(new BigDecimal(esperado), service.detalle(tienda, precio.getProducto().getId()).precioArs());
        assertEquals(0, precio.getProducto().getStock()); assertEquals(new BigDecimal("999"), precio.getProducto().getPrecioVenta());
        verifyNoInteractions(storefronts, asesores);
    }

    @Test void stockNoInformadoNoImpidePublicacion() {
        precio.getProducto().setStock(null); assertEquals(1, service.listado(tienda).size());
    }
    @Test void unResultadoQueRedondeaACeroNoSePublica() {
        precio.setMoneda("USD"); precio.setPrecioOriginal(new BigDecimal("0.01")); config.setCotizacionUsd(new BigDecimal("0.000001"));
        assertTrue(service.listado(tienda).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.detalle(tienda, precio.getProducto().getId()));
    }
    @Test void productoInactivoNoSePublicaNiContacta() {
        precio.getProducto().setActivo(false);
        assertTrue(service.listado(tienda).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.contacto(tienda, precio.getProducto().getId(), "local.test.invalid"));
        verifyNoInteractions(storefronts, asesores);
    }
    @Test void sinConfiguracionNoHayPrecioLegadoDeReemplazo() {
        when(precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(tienda)).thenReturn(List.of());
        when(precios.findByProducto_IdAndProducto_Revendedor_Id(precio.getProducto().getId(), tienda)).thenReturn(Optional.empty());
        assertTrue(service.listado(tienda).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.detalle(tienda, precio.getProducto().getId()));
    }
    @Test void sinCotizacionUsdSeOcultaPeroArsSigueCalculandose() {
        config.setCotizacionUsd(null); precio.setMoneda("USD");
        assertTrue(service.listado(tienda).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.detalle(tienda, precio.getProducto().getId()));
        precio.setMoneda("ARS"); assertEquals(1, service.listado(tienda).size());
    }
    @Test void categoriaAjenaNoSeUsaComoCosto() {
        var categoria = new CatalogoCategoriaEnvio(); var otro = new Revendedor(); otro.setId(UUID.randomUUID());
        categoria.setRevendedor(otro); categoria.setCostoArs(BigDecimal.ONE);
        precio.setIncluyeEnvio(false); precio.setCategoriaEnvio(categoria);
        assertTrue(service.listado(tienda).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.detalle(tienda, precio.getProducto().getId()));
    }
    @Test void productoAjenoNoSeExponeAunqueUnRepositorioLoDevuelva() {
        var otro = new Revendedor(); otro.setId(UUID.randomUUID()); precio.getProducto().setRevendedor(otro);
        assertTrue(service.listado(tienda).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.detalle(tienda, precio.getProducto().getId()));
    }
    @Test void cambiosDeCotizacionYEnvioSeLeenSinCacheDePrecio() {
        precio.setMoneda("USD"); precio.setPrecioOriginal(new BigDecimal("100"));
        assertEquals(new BigDecimal("120000.00"), service.listado(tienda).get(0).precioArs());
        config.setCotizacionUsd(new BigDecimal("1300"));
        assertEquals(new BigDecimal("130000.00"), service.detalle(tienda, precio.getProducto().getId()).precioArs());
        var categoria = new CatalogoCategoriaEnvio(); categoria.setRevendedor(revendedor); categoria.setCostoArs(new BigDecimal("5000"));
        precio.setIncluyeEnvio(false); precio.setCategoriaEnvio(categoria);
        assertEquals(new BigDecimal("135000.00"), service.listado(tienda).get(0).precioArs());
        categoria.setCostoArs(new BigDecimal("6000"));
        assertEquals(new BigDecimal("136000.00"), service.detalle(tienda, precio.getProducto().getId()).precioArs());
    }
    @Test void contactosSonComercialesActivosDeLaTiendaConResponsablePrimero() throws Exception {
        var sf = storefront("+5491112345678");
        var valido = asesor(revendedor, true, "+5491112345679");
        var inactivo = asesor(revendedor, false, "+5491112345680");
        var invalido = asesor(revendedor, true, "123");
        var otro = new Revendedor(); otro.setId(UUID.randomUUID());
        when(storefronts.findByRevendedor_Id(tienda)).thenReturn(Optional.of(sf));
        when(asesores.findByRevendedor_IdOrderByNombreAsc(tienda)).thenReturn(List.of(valido, inactivo, invalido, asesor(otro, true, "+5491112345681")));
        var result = service.contacto(tienda, precio.getProducto().getId(), "tienda.test.invalid");
        assertEquals(2, result.contactos().size()); assertTrue(result.contactos().get(0).responsable());
        assertEquals(valido.getId().toString(), result.contactos().get(1).id());
        assertEquals("100000.00", result.precioPublicoArs());
        var json = new ObjectMapper().writeValueAsString(result);
        for (String privado : List.of("password", "email", "usuario", "cotizacion", "precioOriginal", "costoArs")) assertFalse(json.contains(privado));
        verify(precios, never()).save(any()); verify(storefronts, never()).save(any());
    }
    @Test void ceroContactosNoInventaTelefonoNiHeredaUnoCentral() {
        when(storefronts.findByRevendedor_Id(tienda)).thenReturn(Optional.of(storefront("123")));
        when(asesores.findByRevendedor_IdOrderByNombreAsc(tienda)).thenReturn(List.of());
        assertTrue(service.contacto(tienda, precio.getProducto().getId(), "tienda.test.invalid").contactos().isEmpty());
    }
    @Test void unResponsableEsUnSoloContacto() {
        when(storefronts.findByRevendedor_Id(tienda)).thenReturn(Optional.of(storefront("+5491112345678")));
        when(asesores.findByRevendedor_IdOrderByNombreAsc(tienda)).thenReturn(List.of());
        assertEquals(1, service.contacto(tienda, precio.getProducto().getId(), "tienda.test.invalid").contactos().size());
    }
    RevendedorStorefront storefront(String telefono) {
        var sf = new RevendedorStorefront(); sf.setRevendedor(revendedor); sf.setNombreComercial("Tienda local"); sf.setTelefonoComercial(telefono); return sf;
    }
    AsesorVenta asesor(Revendedor r, boolean activo, String telefono) {
        var a = new AsesorVenta(); a.setId(UUID.randomUUID()); a.setNombre("Asesor local"); a.setRevendedor(r); a.setActivo(activo); a.setWhatsappComercial(telefono); return a;
    }
}
