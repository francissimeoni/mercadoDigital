package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.dto.request.RevendedorProductoRequest;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.exception.BadRequestException;

class CatalogoFichaStockTest {
    UUID tienda = UUID.randomUUID();
    RevendedorOwnershipService ownership = mock(RevendedorOwnershipService.class);
    RevendedorRepository revendedores = mock(RevendedorRepository.class);
    CategoriaProductoRepository categorias = mock(CategoriaProductoRepository.class);
    RevendedorProductoRepository productos = mock(RevendedorProductoRepository.class);
    RevendedorProductoMediaRepository media = mock(RevendedorProductoMediaRepository.class);
    Revendedor revendedor() { var r = new Revendedor(); r.setId(tienda); return r; }
    RevendedorProductoService service(boolean catalogo) {
        return new RevendedorProductoService(ownership, revendedores, categorias, productos, media,
                new CatalogoModoService(catalogo ? tienda.toString() : ""));
    }
    RevendedorProductoRequest input() {
        var r = new RevendedorProductoRequest(); r.setSku("C1"); r.setNombre("Ficha"); r.setPrecioVenta(BigDecimal.TEN);
        r.setCategoriaPaquete("MEDIANO"); r.setActivo(true); r.setPromocion(false); r.setNovedad(false); return r;
    }
    @Test void fichaNuevaDeCatalogoNoExigeNiInventaStock() {
        when(revendedores.findById(tienda)).thenReturn(Optional.of(revendedor()));
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = service(true).crear(tienda, input());
        assertNull(response.getStock()); verify(ownership).validarOwnership(tienda);
    }
    @Test void editarFichaEnCatalogoConservaStockHistorico() {
        var p = new RevendedorProducto(); p.setId(UUID.randomUUID()); p.setRevendedor(revendedor()); p.setStock(17);
        p.setPrecioVenta(BigDecimal.TEN); p.setCategoriaPaquete("MEDIANO");
        when(productos.findByIdAndRevendedor_Id(p.getId(), tienda)).thenReturn(Optional.of(p));
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        var request = input(); request.setStock(999);
        assertEquals(17, service(true).actualizar(tienda, p.getId(), request).getStock());
    }
    @Test void modoAnteriorSigueExigiendoStock() {
        when(revendedores.findById(tienda)).thenReturn(Optional.of(revendedor()));
        assertThrows(BadRequestException.class, () -> service(false).crear(tienda, input()));
        verify(productos, never()).save(any());
    }
}
