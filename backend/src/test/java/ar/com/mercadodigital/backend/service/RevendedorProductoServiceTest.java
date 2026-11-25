package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.request.RevendedorProductoRequest;
import ar.com.mercadodigital.backend.dto.response.RevendedorProductoResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.repository.CategoriaProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;

@ExtendWith(MockitoExtension.class)
class RevendedorProductoServiceTest {
    @Mock private CatalogoModoService modoCatalogo;

    @Test
    void ajusteMasivoLegadoRechazaMonedaDistintaDePesosSinConvertirla() {
        UUID tienda = UUID.randomUUID(), id = UUID.randomUUID();
        var product = new RevendedorProducto(); product.setMoneda("USD");
        product.setPrecioVenta(new BigDecimal("100")); product.setStock(0);
        var item = new ar.com.mercadodigital.backend.dto.request.RevendedorPrecioMasivoItemRequest();
        item.setProductoId(id); item.setPrecioFinal(new BigDecimal("200"));
        var input = new ar.com.mercadodigital.backend.dto.request.RevendedorAjusteMasivoPreciosRequest();
        input.setItems(java.util.List.of(item));
        when(productoRepository.findForUpdate(id, tienda)).thenReturn(Optional.of(product));
        org.junit.jupiter.api.Assertions.assertThrows(ar.com.mercadodigital.backend.exception.BadRequestException.class,
                () -> service.ajustarPreciosMasivamente(tienda, input));
        assertEquals(new BigDecimal("100"), product.getPrecioVenta()); assertEquals(0, product.getStock());
        org.mockito.Mockito.verify(productoRepository, org.mockito.Mockito.never()).save(any());
    }

    @Mock
    private RevendedorOwnershipService ownershipService;
    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private CategoriaProductoRepository categoriaProductoRepository;
    @Mock
    private RevendedorProductoRepository productoRepository;
    @Mock
    private RevendedorProductoMediaRepository mediaRepository;
    @InjectMocks
    private RevendedorProductoService service;

    @Test
    void ajusteMasivoRechazaDuplicadosAntesDeModificar() {
        var item = new ar.com.mercadodigital.backend.dto.request.RevendedorPrecioMasivoItemRequest();
        item.setProductoId(UUID.randomUUID()); item.setPrecioFinal(BigDecimal.TEN);
        var request = new ar.com.mercadodigital.backend.dto.request.RevendedorAjusteMasivoPreciosRequest();
        request.setItems(java.util.List.of(item, item));
        org.junit.jupiter.api.Assertions.assertThrows(ar.com.mercadodigital.backend.exception.BadRequestException.class,
                () -> service.ajustarPreciosMasivamente(UUID.randomUUID(), request));
        org.mockito.Mockito.verifyNoInteractions(productoRepository);
    }

    @Test
    void ajusteMasivoBloqueaPorTiendaYRedondeaHalfUp() {
        UUID storeId = UUID.randomUUID(), productId = UUID.randomUUID();
        var item = new ar.com.mercadodigital.backend.dto.request.RevendedorPrecioMasivoItemRequest();
        item.setProductoId(productId); item.setPrecioFinal(new BigDecimal("12.345"));
        var request = new ar.com.mercadodigital.backend.dto.request.RevendedorAjusteMasivoPreciosRequest(); request.setItems(java.util.List.of(item));
        var store = new Revendedor(); store.setId(storeId);
        var product = new RevendedorProducto(); product.setId(productId); product.setRevendedor(store); product.setStock(4);
        product.setPrecioVenta(BigDecimal.TEN);
        when(productoRepository.findForUpdate(productId, storeId)).thenReturn(Optional.of(product));
        when(productoRepository.save(product)).thenReturn(product);
        var result = service.ajustarPreciosMasivamente(storeId, request);
        assertEquals(new BigDecimal("12.35"), result.get(0).getPrecioVenta());
        assertEquals(4, product.getStock()); verify(ownershipService).validarOwnership(storeId);
    }

    @Test void ajusteAnteriorNoCompletaUnPrecioFaltanteDelCatalogo() {
        UUID tienda = UUID.randomUUID(), id = UUID.randomUUID();
        var producto = new RevendedorProducto();
        var item = new ar.com.mercadodigital.backend.dto.request.RevendedorPrecioMasivoItemRequest();
        item.setProductoId(id); item.setPrecioFinal(BigDecimal.TEN);
        var input = new ar.com.mercadodigital.backend.dto.request.RevendedorAjusteMasivoPreciosRequest(); input.setItems(java.util.List.of(item));
        when(productoRepository.findForUpdate(id, tienda)).thenReturn(Optional.of(producto));
        org.junit.jupiter.api.Assertions.assertThrows(ar.com.mercadodigital.backend.exception.BadRequestException.class,
                () -> service.ajustarPreciosMasivamente(tienda, input));
        org.junit.jupiter.api.Assertions.assertNull(producto.getPrecioVenta());
        verify(productoRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test void edicionAnteriorNoInventaCompatibilidadParaFichaDeCatalogo() {
        UUID tienda = UUID.randomUUID(), id = UUID.randomUUID();
        var producto = new RevendedorProducto();
        when(productoRepository.findByIdAndRevendedor_Id(id, tienda)).thenReturn(Optional.of(producto));
        org.junit.jupiter.api.Assertions.assertThrows(ar.com.mercadodigital.backend.exception.OperacionEnConflictoException.class,
                () -> service.actualizar(tienda, id, new RevendedorProductoRequest()));
        verify(productoRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void creaProductoPropioConContratoRecuperado() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        RevendedorProductoRequest request = new RevendedorProductoRequest();
        request.setSku(" SKU-1 ");
        request.setNombre(" Producto propio ");
        request.setDescripcion(" Descripcion ");
        request.setPrecioVenta(new BigDecimal("1999.90"));
        request.setStock(4);
        request.setCategoriaPaquete("mediano");
        request.setActivo(true);
        request.setPromocion(true);
        request.setNovedad(false);

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(productoRepository.save(any(RevendedorProducto.class))).thenAnswer(invocation -> {
            RevendedorProducto producto = invocation.getArgument(0);
            producto.setId(UUID.randomUUID());
            return producto;
        });

        RevendedorProductoResponse response = service.crear(revendedorId, request);

        assertEquals("SKU-1", response.getSku());
        assertEquals("Producto propio", response.getNombre());
        assertEquals("ARS", response.getMoneda());
        assertEquals("MEDIANO", response.getCategoriaPaquete());
        assertEquals(true, response.isPromocion());
        verify(ownershipService).validarOwnership(revendedorId);
    }
}
