package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import ar.com.mercadodigital.backend.dto.request.AdminRevendedorProductoImportRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorProductoImportResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;
import ar.com.mercadodigital.backend.repository.CategoriaProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;

@ExtendWith(MockitoExtension.class)
class AdminRevendedorProductoImportServiceTest {
    @Mock private CatalogoModoService modoCatalogo;

    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private RevendedorProductoRepository productoRepository;
    @Mock
    private RevendedorProductoMediaRepository mediaRepository;
    @Mock
    private CategoriaProductoRepository categoriaProductoRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;
    @InjectMocks
    private AdminRevendedorProductoImportService service;

    @BeforeEach
    void setUpTransaction() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
    }

    @Test
    void creaProductoConTodosLosCamposDeLaCargaManual() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        when(revendedorRepository.existsById(revendedorId)).thenReturn(true);
        when(revendedorRepository.getReferenceById(revendedorId)).thenReturn(revendedor);
        when(productoRepository.findByRevendedor_IdAndSkuIgnoreCase(revendedorId, "NB-001"))
                .thenReturn(Optional.empty());
        when(productoRepository.save(any(RevendedorProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminRevendedorProductoImportRequest.Item item = new AdminRevendedorProductoImportRequest.Item();
        item.setFila(2);
        item.setIncluir(true);
        item.setSku(" NB-001 ");
        item.setNombre("Notebook lista para trabajar");
        item.setDescripcion("Descripción comercial");
        item.setPrecioVenta(new BigDecimal("299999.90"));
        item.setStock(7);
        item.setCategoriaPaquete("CHICO");
        item.setActivo(false);
        item.setPromocion(true);
        item.setNovedad(true);
        item.setImagenes(List.of("https://imagenes.test/uno.jpg", "https://imagenes.test/dos.jpg"));
        item.setModoImagenes("AGREGAR");
        AdminRevendedorProductoImportRequest request = new AdminRevendedorProductoImportRequest();
        request.setItems(List.of(item));

        AdminRevendedorProductoImportResponse response = service.importar(revendedorId, request);

        assertEquals(1, response.getCreados());
        assertEquals(0, response.getRechazados());
        ArgumentCaptor<RevendedorProducto> captor = ArgumentCaptor.forClass(RevendedorProducto.class);
        verify(productoRepository).save(captor.capture());
        RevendedorProducto creado = captor.getValue();
        assertEquals("NB-001", creado.getSku());
        assertEquals("Notebook lista para trabajar", creado.getNombre());
        assertEquals("Descripción comercial", creado.getDescripcion());
        assertEquals(new BigDecimal("299999.90"), creado.getPrecioVenta());
        assertEquals(7, creado.getStock());
        assertEquals("CHICO", creado.getCategoriaPaquete());
        assertEquals(false, creado.isActivo());
        assertTrue(creado.isPromocion());
        assertTrue(creado.isNovedad());
        assertEquals(2, creado.getMedia().size());
        assertTrue(creado.getMedia().get(0).isEsPrincipal());
        verify(mediaRepository, times(2)).save(any(RevendedorProductoMedia.class));
        verify(transactionManager).commit(eq(transactionStatus));
    }

    @Test void importadorAnteriorRechazaFilaDeCatalogoSinInventarPrecio() {
        UUID tienda = UUID.randomUUID();
        when(revendedorRepository.existsById(tienda)).thenReturn(true);
        var producto = new RevendedorProducto(); producto.setSku("CAT-1");
        when(productoRepository.findByRevendedor_IdAndSkuIgnoreCase(tienda, "CAT-1")).thenReturn(Optional.of(producto));
        var item = new AdminRevendedorProductoImportRequest.Item(); item.setSku("CAT-1"); item.setIncluir(true); item.setFila(2);
        var input = new AdminRevendedorProductoImportRequest(); input.setItems(List.of(item));
        var result = service.importar(tienda, input);
        assertEquals(1, result.getRechazados()); assertEquals(0, result.getActualizados());
        org.junit.jupiter.api.Assertions.assertNull(producto.getPrecioVenta());
        verify(productoRepository, org.mockito.Mockito.never()).save(any());
        verify(transactionManager).rollback(transactionStatus);
    }
}
