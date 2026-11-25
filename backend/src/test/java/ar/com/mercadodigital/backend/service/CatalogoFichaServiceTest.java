package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ar.com.mercadodigital.backend.dto.CatalogoAdminDto.*;
import ar.com.mercadodigital.backend.dto.CatalogoFichaDto.*;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.exception.*;

@ExtendWith(MockitoExtension.class)
class CatalogoFichaServiceTest {
    @Mock RevendedorOwnershipService ownership;
    @Mock RevendedorStorefrontRepository storefronts;
    @Mock RevendedorProductoRepository productos;
    @Mock CategoriaProductoRepository categorias;
    @Mock CatalogoAdminService catalogo;
    @Mock RevendedorProductoService fichas;
    @Mock StorefrontAssetStorageService assets;
    @Mock AuditoriaEventoRepository auditoria;
    CatalogoFichaService service;
    UUID tienda = UUID.randomUUID(), id = UUID.randomUUID(), operacion = UUID.randomUUID();
    RevendedorProducto producto;
    RevendedorStorefront storefront;
    Precio calculo = new CatalogoPrecioCalculator().calcular("USD", new BigDecimal("100"), true, new BigDecimal("1200"), null);

    @BeforeEach void setup() {
        service = new CatalogoFichaService(ownership, storefronts, productos, categorias, catalogo, fichas, assets,
                auditoria, new ObjectMapper().findAndRegisterModules());
        var revendedor = new Revendedor(); revendedor.setId(tienda);
        storefront = new RevendedorStorefront(); storefront.setRevendedor(revendedor);
        producto = new RevendedorProducto(); producto.setId(id); producto.setRevendedor(revendedor);
        producto.setNombre("Artículo"); producto.setSku("SKU-1"); producto.setVersion(3);
        producto.setPrecioVenta(new BigDecimal("25000")); producto.setStock(7); producto.setCategoriaPaquete("MEDIANO");
        lenient().when(storefronts.findForCatalogoUpdate(tienda)).thenReturn(Optional.of(storefront));
        lenient().when(productos.findByIdAndRevendedor_Id(id, tienda)).thenReturn(Optional.of(producto));
        lenient().when(productos.findForUpdate(id, tienda)).thenReturn(Optional.of(producto));
        lenient().when(catalogo.previewProducto(eq(tienda), any(), any())).thenReturn(new ProductoPreview(calculo, "precio"));
        lenient().when(catalogo.previewNuevo(eq(tienda), any())).thenReturn(new ProductoPreview(calculo, "nuevo"));
        lenient().when(productos.saveAndFlush(any())).thenAnswer(inv -> { var p = (RevendedorProducto) inv.getArgument(0); if (p.getId() == null) p.setId(id); return p; });
    }
    Input input(boolean nuevo) {
        return new Input(operacion, "SKU-1", "Artículo", "Descripción", null, true, false, false,
                nuevo ? -1L : 3L, new ProductoInput("USD", new BigDecimal("100"), true, null, -1L, null), List.of(), List.of(), null);
    }
    Input token(Input i, String token) { return new Input(i.operacionId(), i.sku(), i.nombre(), i.descripcion(), i.categoriaProductoId(), i.activo(), i.promocion(), i.novedad(), i.versionFicha(), i.precio(), i.imagenes(), i.archivos(), token); }
    Input confirmado(boolean nuevo) { var i = input(nuevo); return token(i, service.preview(tienda, nuevo ? null : id, i).token()); }

    @Test void altaNoInventaPrecioStockNiPaqueteYGuardaPrecioEnMismaOperacion() {
        var input = confirmado(true);
        service.guardar(tienda, null, input, List.of(), "http://local.invalid/api");
        var captor = ArgumentCaptor.forClass(RevendedorProducto.class); verify(productos).saveAndFlush(captor.capture());
        var nuevo = captor.getValue(); assertNull(nuevo.getPrecioVenta()); assertNull(nuevo.getStock()); assertNull(nuevo.getCategoriaPaquete());
        assertEquals("ARS", nuevo.getMoneda()); assertEquals(operacion.toString(), nuevo.getCatalogoAltaToken());
        verify(catalogo).guardarProducto(eq(tienda), eq(id), argThat(p -> p.precioOriginal().equals(new BigDecimal("100")) && p.previewToken().equals("precio")));
        verify(auditoria).save(any());
    }
    @Test void editarConservaPrecioStockYPaqueteHistoricos() {
        service.guardar(tienda, id, confirmado(false), List.of(), "http://local.invalid");
        assertEquals(new BigDecimal("25000"), producto.getPrecioVenta()); assertEquals(7, producto.getStock()); assertEquals("MEDIANO", producto.getCategoriaPaquete());
    }
    @Test void conflictoFichaNoEscribeNada() {
        var i = confirmado(false); producto.setVersion(4);
        assertThrows(OperacionEnConflictoException.class, () -> service.guardar(tienda, id, i, List.of(), "http://local.invalid"));
        verify(productos, never()).saveAndFlush(any()); verifyNoInteractions(assets);
    }
    @Test void cambioDeCotizacionOEnvioInvalidaConfirmacion() {
        var i = confirmado(false);
        when(catalogo.previewProducto(eq(tienda), any(), any())).thenReturn(new ProductoPreview(calculo, "nuevo-costo"));
        assertThrows(OperacionEnConflictoException.class, () -> service.guardar(tienda, id, i, List.of(), "http://local.invalid"));
        verify(productos, never()).saveAndFlush(any());
    }
    @Test void skuDuplicadoEnTiendaSeRechazaAntesDeGuardar() {
        when(productos.existsByRevendedor_IdAndSkuIgnoreCaseAndIdNot(eq(tienda), anyString(), any())).thenReturn(true);
        assertThrows(OperacionEnConflictoException.class, () -> service.preview(tienda, null, input(true)));
        verifyNoInteractions(assets);
    }
    @Test void noPermiteOperarOtraTienda() {
        doThrow(new AccessDeniedException("ajena")).when(ownership).validarOwnership(tienda);
        assertThrows(AccessDeniedException.class, () -> service.guardar(tienda, null, input(true), List.of(), "http://local.invalid"));
        verifyNoInteractions(storefronts, productos, assets);
    }
    @Test void productoAjenoNoExisteEnElAlcance() {
        var ajeno = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.preview(tienda, ajeno, input(false)));
        verifyNoInteractions(assets);
    }
    @Test void rechazoDeCalculoNoPersisteFichaNiArchivos() {
        var pendiente = new CatalogoPrecioCalculator().calcular("USD", BigDecimal.TEN, true, null, null);
        when(catalogo.previewNuevo(eq(tienda), any())).thenReturn(new ProductoPreview(pendiente, "nuevo"));
        var i = confirmado(true);
        assertThrows(BadRequestException.class, () -> service.guardar(tienda, null, i, List.of(), "http://local.invalid"));
        verify(productos, never()).saveAndFlush(any()); verifyNoInteractions(assets);
    }
    @Test void imagenAjenaOUrlEjecutableNoSeAcepta() {
        var base = input(false);
        for (var imagen : List.of(new Imagen(UUID.randomUUID(), "https://imagen.invalid/a.png"), new Imagen(null, "javascript:alert(1)"))) {
            var i = new Input(base.operacionId(), base.sku(), base.nombre(), base.descripcion(), null, true, false, false, 3L, base.precio(), List.of(imagen), List.of(), null);
            assertThrows(BadRequestException.class, () -> service.preview(tienda, id, i));
        }
    }
    @Test void repetirAltaConservaUnSoloProductoYNoSubeOtraVez() {
        var i = confirmado(true);
        service.guardar(tienda, null, i, List.of(), "http://local.invalid");
        var captor = ArgumentCaptor.forClass(RevendedorProducto.class); verify(productos).saveAndFlush(captor.capture());
        when(productos.findByRevendedor_IdAndCatalogoAltaToken(tienda, operacion.toString())).thenReturn(Optional.of(captor.getValue()));
        clearInvocations(productos, catalogo, auditoria);
        service.guardar(tienda, null, i, List.of(), "http://local.invalid");
        verify(productos, never()).saveAndFlush(any()); verify(catalogo, never()).guardarProducto(any(), any(), any()); verifyNoInteractions(assets, auditoria);
    }
    @Test void noReutilizaClaveDeAltaParaDatosDistintos() {
        producto.setCatalogoAltaHash("otra-peticion");
        when(productos.findByRevendedor_IdAndCatalogoAltaToken(tienda, operacion.toString())).thenReturn(Optional.of(producto));
        assertThrows(OperacionEnConflictoException.class, () -> service.guardar(tienda, null, input(true), List.of(), "http://local.invalid"));
        verify(productos, never()).saveAndFlush(any());
    }
}
