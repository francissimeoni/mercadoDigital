package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ar.com.mercadodigital.backend.dto.request.ProductoMediaRequest;
import ar.com.mercadodigital.backend.dto.request.ProveedorProductoUpsertRequest;
import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorProductoServiceTest {

    @Mock
    private ProveedorOwnershipService proveedorOwnershipService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProductoCanonicoRepository productoCanonicoRepository;
    @Mock
    private ProveedorStockRepository proveedorStockRepository;
    @Mock
    private ProductoMediaRepository productoMediaRepository;
    @Mock
    private ProductoMediaService productoMediaService;
    @Mock
    private CategoriaProductoService categoriaProductoService;

    @InjectMocks
    private ProveedorProductoService proveedorProductoService;

    @Test
    void crearProductoPermiteProductoPendienteDeNormalizacion() {
        UUID proveedorId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.existsBySkuIgnoreCase("SKU-TEST-1")).thenReturn(false);

        ArgumentCaptor<Producto> productoCaptor = ArgumentCaptor.forClass(Producto.class);
        when(productoRepository.save(productoCaptor.capture())).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            producto.setId(UUID.randomUUID());
            return producto;
        });
        when(proveedorStockRepository.findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(any(), any())).thenReturn(12);
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(any())).thenReturn(List.of());

        ProveedorProductoResponse result = proveedorProductoService.crearProducto(
                proveedorId,
                buildRequest(null),
                authProveedor());

        assertNull(result.getProductoCanonicoId());
        assertEquals("PENDIENTE", result.getEstadoNormalizacion());
        assertEquals("SKU-TEST-1", productoCaptor.getValue().getSku());
        verify(proveedorStockRepository).save(any(ProveedorStock.class));
        verifyNoInteractions(productoMediaService);
    }

    @Test
    void crearProductoPermiteAdjuntarHastaTresImagenesIniciales() {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);

        ProveedorProductoUpsertRequest request = buildRequest(null);
        request.setImagenes(List.of(
                "https://cdn.local/producto-1.jpg",
                "https://cdn.local/producto-2.jpg"));

        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.existsBySkuIgnoreCase("SKU-TEST-1")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            producto.setId(productoId);
            return producto;
        });
        when(proveedorStockRepository.findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(any(), any())).thenReturn(12);
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)).thenReturn(List.of());

        proveedorProductoService.crearProducto(proveedorId, request, authProveedor());

        verify(productoMediaService).addMedia(productoId, TipoMedia.IMAGEN, "https://cdn.local/producto-1.jpg", 1, true);
        verify(productoMediaService).addMedia(productoId, TipoMedia.IMAGEN, "https://cdn.local/producto-2.jpg", 2, false);
    }

    @Test
    void actualizarProductoPermiteAsociarCanonicoExistente() {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID canonicoId = UUID.randomUUID();

        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setProveedor(proveedor);

        ProductoCanonico canonico = new ProductoCanonico();
        canonico.setId(canonicoId);
        canonico.setNombrePublico("Canonico Demo");

        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.findByIdAndProveedor_Id(productoId, proveedorId)).thenReturn(Optional.of(producto));
        when(productoCanonicoRepository.findByIdAndActivoTrue(canonicoId)).thenReturn(Optional.of(canonico));
        when(productoRepository.existsBySkuIgnoreCaseAndIdNot("SKU-TEST-1", productoId)).thenReturn(false);
        when(productoRepository.save(producto)).thenReturn(producto);
        when(proveedorStockRepository.findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(proveedorId, productoId))
                .thenReturn(Optional.of(new ProveedorStock()));
        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(proveedorId, productoId)).thenReturn(12);
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)).thenReturn(List.of(
                buildMedia(producto, TipoMedia.IMAGEN, true),
                buildMedia(producto, TipoMedia.VIDEO, false)));

        ProveedorProductoResponse result = proveedorProductoService.actualizarProducto(
                proveedorId,
                productoId,
                buildRequest(canonicoId),
                authProveedor());

        assertEquals(canonicoId, result.getProductoCanonicoId());
        assertEquals("ASOCIADO", result.getEstadoNormalizacion());
        assertEquals("Canonico Demo", result.getProductoCanonicoNombre());
        assertEquals(2, result.getCantidadMedia());
    }

    @Test
    void desactivarProductoHaceBajaLogicaDelProductoDelProveedor() {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setProveedor(proveedor);
        producto.setSku("SKU-TEST-1");
        producto.setNombre("Producto proveedor demo");
        producto.setPrecioCosto(new BigDecimal("1500.00"));
        producto.setMoneda("ARS");
        producto.setCategoriaPaquete("CHICO");
        producto.setActivo(true);

        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.findByIdAndProveedor_Id(productoId, proveedorId)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);
        when(proveedorStockRepository.sumStockByProveedorIdAndProductoId(proveedorId, productoId)).thenReturn(12);
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)).thenReturn(List.of());

        ProveedorProductoResponse result = proveedorProductoService.desactivarProducto(
                proveedorId,
                productoId,
                authProveedor());

        assertEquals(false, result.isActivo());
        verify(productoRepository).save(producto);
    }

    @Test
    void mediaRequiereProductoDelProveedor() {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.findByIdAndProveedor_Id(productoId, proveedorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> proveedorProductoService.getMedia(
                proveedorId,
                productoId,
                authProveedor()));
    }

    @Test
    void addMediaReutilizaServicioExistente() {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setProveedor(proveedor);

        ProductoMediaRequest request = new ProductoMediaRequest();
        request.setTipoMedia(TipoMedia.IMAGEN);
        request.setUrl("https://cdn.local/demo.png");
        request.setOrden(1);
        request.setEsPrincipal(true);

        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);
        when(productoRepository.findByIdAndProveedor_Id(productoId, proveedorId)).thenReturn(Optional.of(producto));
        when(productoMediaService.addMedia(productoId, TipoMedia.IMAGEN, "https://cdn.local/demo.png", 1, true))
                .thenReturn(ProductoMediaResponse.builder()
                        .id(mediaId)
                        .productoId(productoId)
                        .tipoMedia("IMAGEN")
                        .url("https://cdn.local/demo.png")
                        .orden(1)
                        .esPrincipal(true)
                        .build());

        ProductoMediaResponse result = proveedorProductoService.addMedia(proveedorId, productoId, request, authProveedor());

        assertEquals(mediaId, result.getId());
        verify(productoMediaService).addMedia(productoId, TipoMedia.IMAGEN, "https://cdn.local/demo.png", 1, true);
    }

    @Test
    void crearProductoRechazaMasDeTresImagenes() {
        UUID proveedorId = UUID.randomUUID();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);

        ProveedorProductoUpsertRequest request = buildRequest(null);
        request.setImagenes(List.of(
                "https://cdn.local/producto-1.jpg",
                "https://cdn.local/producto-2.jpg",
                "https://cdn.local/producto-3.jpg",
                "https://cdn.local/producto-4.jpg"));

        when(proveedorOwnershipService.resolveProveedorAutorizado(eq(proveedorId), any())).thenReturn(proveedor);

        assertThrows(BadRequestException.class, () -> proveedorProductoService.crearProducto(
                proveedorId,
                request,
                authProveedor()));
    }

    private ProveedorProductoUpsertRequest buildRequest(UUID canonicoId) {
        ProveedorProductoUpsertRequest request = new ProveedorProductoUpsertRequest();
        request.setSku("SKU-TEST-1");
        request.setNombre("Producto proveedor demo");
        request.setDescripcion("Descripcion");
        request.setPrecioCosto(new BigDecimal("1500.00"));
        request.setStock(12);
        request.setCategoriaPaquete("CHICO");
        request.setProductoCanonicoId(canonicoId);
        request.setActivo(true);
        return request;
    }

    private ProductoMedia buildMedia(Producto producto, TipoMedia tipoMedia, boolean esPrincipal) {
        ProductoMedia media = new ProductoMedia();
        media.setId(UUID.randomUUID());
        media.setProducto(producto);
        media.setTipoMedia(tipoMedia);
        media.setUrl("https://cdn.local/" + media.getId() + ".png");
        media.setOrden(1);
        media.setEsPrincipal(esPrincipal);
        return media;
    }

    private UsernamePasswordAuthenticationToken authProveedor() {
        return new UsernamePasswordAuthenticationToken(
                "proveedor@test.com",
                null,
                List.of(new SimpleGrantedAuthority("PROVEEDOR")));
    }
}
