package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import ar.com.mercadodigital.backend.dto.response.CatalogoItemResponse;
import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoDetalleResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private ProductoCanonicoRepository productoCanonicoRepository;
    @Mock
    private OfertaProductoCanonicoService ofertaProductoCanonicoService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProductoMediaRepository productoMediaRepository;

    @InjectMocks
    private CatalogoService catalogoService;

    @Test
    void noIncluyeCanonicoSiTodasLasOfertasTienenStockCero() {
        UUID revendedorId = UUID.randomUUID();
        ProductoCanonico canonico = buildCanonico("Canonico A");

        when(productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc()).thenReturn(List.of(canonico));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonico.getId(), revendedorId))
                .thenReturn(List.of());

        List<CatalogoItemResponse> result = catalogoService.getCatalogo(revendedorId);
        assertEquals(0, result.size());
    }

    @Test
    void devuelveUnItemPorCanonicoYOrdenadoPorNombrePublico() {
        UUID revendedorId = UUID.randomUUID();
        ProductoCanonico canonicoZ = buildCanonico("Zulu");
        ProductoCanonico canonicoA = buildCanonico("Alpha");

        when(productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc())
                .thenReturn(List.of(canonicoZ, canonicoA));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonicoZ.getId(), revendedorId))
                .thenReturn(List.of(oferta(new BigDecimal("2000"), 3, 10, canonicoZ.getId())));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonicoA.getId(), revendedorId))
                .thenReturn(List.of(oferta(new BigDecimal("1000"), 5, 4, canonicoA.getId())));
        when(productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        List<CatalogoItemResponse> result = catalogoService.getCatalogo(revendedorId);

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).getNombrePublico());
        assertEquals("Zulu", result.get(1).getNombrePublico());
    }

    @Test
    void cantidadOfertasCuentaSoloConStockPositivo() {
        UUID revendedorId = UUID.randomUUID();
        ProductoCanonico canonico = buildCanonico("Canonico A");

        when(productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc()).thenReturn(List.of(canonico));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonico.getId(), revendedorId))
                .thenReturn(List.of(
                        oferta(new BigDecimal("1000"), 2, 20, canonico.getId()),
                        oferta(new BigDecimal("1100"), 2, 8, canonico.getId()),
                        oferta(new BigDecimal("1200"), 4, 1, canonico.getId())));
        when(productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        List<CatalogoItemResponse> result = catalogoService.getCatalogo(revendedorId);

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getCantidadOfertas());
        assertEquals(new BigDecimal("1000"), result.get(0).getMejorCostoOperativoEstimado());
        assertEquals(20, result.get(0).getStockDisponible());
    }

    @Test
    void getProductoDetalleDevuelveMejorOfertaYDescripcion() {
        UUID revendedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        ProductoCanonico canonico = buildCanonico("Canonico A");
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setDescripcion("Descripcion operativa");

        when(productoCanonicoRepository.findByIdAndActivoTrue(canonico.getId())).thenReturn(Optional.of(canonico));
        when(ofertaProductoCanonicoService.getOfertasInternasConStock(canonico.getId(), revendedorId))
                .thenReturn(List.of(oferta(new BigDecimal("1000"), 2, 20, canonico.getId(), productoId)));
        when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(productoId))
                .thenReturn(Optional.empty());
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId))
                .thenReturn(List.of());

        ProductoDetalleResponse result = catalogoService.getProductoDetalle(canonico.getId(), revendedorId);

        assertEquals(canonico.getId(), result.getProductoCanonicoId());
        assertEquals("Canonico A", result.getNombrePublico());
        assertEquals("Descripcion operativa", result.getDescripcion());
        assertEquals(new BigDecimal("1000"), result.getMejorCostoOperativoEstimado());
        assertEquals(20, result.getStockDisponible());
    }

    private ProductoCanonico buildCanonico(String nombrePublico) {
        ProductoCanonico canonico = new ProductoCanonico();
        canonico.setId(UUID.randomUUID());
        canonico.setActivo(true);
        canonico.setNombrePublico(nombrePublico);
        canonico.setMarca("Marca");
        canonico.setModelo("Modelo");
        return canonico;
    }

    private OfertaProductoResponse oferta(BigDecimal precio, int demora, int stock, UUID canonicoId) {
        return oferta(precio, demora, stock, canonicoId, UUID.randomUUID());
    }

    private OfertaProductoResponse oferta(BigDecimal precio, int demora, int stock, UUID canonicoId, UUID productoId) {
        return OfertaProductoResponse.builder()
                .productoId(productoId)
                .productoCanonicoId(canonicoId)
                .precioFinalEstimado(precio)
                .demoraEstimadaDias(demora)
                .stockDisponible(stock)
                .categoriaPaquete("CHICO")
                .build();
    }
}
