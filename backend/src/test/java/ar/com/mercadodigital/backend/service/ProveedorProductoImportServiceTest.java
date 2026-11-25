package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import ar.com.mercadodigital.backend.dto.response.ProveedorProductoImportResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorProductoImportServiceTest {

    @Mock
    private ProveedorOwnershipService proveedorOwnershipService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProveedorStockRepository proveedorStockRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private ProveedorProductoImportService proveedorProductoImportService;

    @Test
    void importaCsvYConsolidaStockPorSucursales() {
        UUID proveedorId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.findByProveedor_IdAndSkuIgnoreCase(proveedorId, "SKU-1")).thenReturn(Optional.empty());
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        doNothing().when(transactionManager).commit(transactionStatus);

        ArgumentCaptor<Producto> productoCaptor = ArgumentCaptor.forClass(Producto.class);
        when(productoRepository.save(productoCaptor.capture())).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            producto.setId(UUID.randomUUID());
            return producto;
        });

        ArgumentCaptor<ProveedorStock> stockCaptor = ArgumentCaptor.forClass(ProveedorStock.class);
        when(proveedorStockRepository.findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(proveedorStockRepository.save(stockCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "catalogo.csv",
                "text/csv",
                """
                Codigo;Descripcion;precio;ROS;MZA;CBA;LUG
                SKU-1;Notebook Proveedor;1234,50;2;3;4;5
                """.getBytes());

        ProveedorProductoImportResponse result = proveedorProductoImportService.importar(
                proveedorId,
                archivo,
                authProveedor());

        assertEquals(1, result.getFilasLeidas());
        assertEquals(1, result.getCreados());
        assertEquals(0, result.getActualizados());
        assertEquals(0, result.getRechazados());
        assertEquals("SKU-1", productoCaptor.getValue().getSku());
        assertEquals("Notebook Proveedor", productoCaptor.getValue().getNombre());
        assertEquals(new BigDecimal("1234.50"), productoCaptor.getValue().getPrecioCosto());
        assertEquals(14, stockCaptor.getValue().getStock());
        assertEquals(new BigDecimal("1234.50"), stockCaptor.getValue().getPrecioCostoActual());
    }

    @Test
    void actualizaProductoExistenteSinBorrarCanonicoNiCategoria() {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);

        Producto existente = new Producto();
        existente.setId(productoId);
        existente.setProveedor(proveedor);
        existente.setCategoriaPaquete("MEDIANO");
        existente.setActivo(false);

        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.findByProveedor_IdAndSkuIgnoreCase(proveedorId, "SKU-1")).thenReturn(Optional.of(existente));
        when(productoRepository.save(existente)).thenReturn(existente);
        when(proveedorStockRepository.findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(proveedorId, productoId))
                .thenReturn(Optional.of(new ProveedorStock()));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        doNothing().when(transactionManager).commit(transactionStatus);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "catalogo.csv",
                "text/csv",
                """
                Codigo,Descripcion,precio,ROS,MZA,CBA,LUG
                SKU-1,Producto Actualizado,999.99,1,0,0,0
                """.getBytes());

        ProveedorProductoImportResponse result = proveedorProductoImportService.importar(
                proveedorId,
                archivo,
                authProveedor());

        assertEquals(0, result.getCreados());
        assertEquals(1, result.getActualizados());
        assertEquals("MEDIANO", existente.getCategoriaPaquete());
        assertEquals(true, existente.isActivo());
        assertEquals("Producto Actualizado", existente.getDescripcion());
    }

    @Test
    void rechazaArchivoSinColumnasMinimas() {
        UUID proveedorId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "catalogo.csv",
                "text/csv",
                "Codigo,ROS\nSKU-1,5\n".getBytes());

        assertThrows(BadRequestException.class, () -> proveedorProductoImportService.importar(
                proveedorId,
                archivo,
                authProveedor()));
    }

    private UsernamePasswordAuthenticationToken authProveedor() {
        return new UsernamePasswordAuthenticationToken(
                "proveedor@test.com",
                null,
                List.of(new SimpleGrantedAuthority("PROVEEDOR")));
    }
}
