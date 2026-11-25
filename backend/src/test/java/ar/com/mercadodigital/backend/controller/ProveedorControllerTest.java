package ar.com.mercadodigital.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import ar.com.mercadodigital.backend.dto.request.ProveedorProductoUpsertRequest;
import ar.com.mercadodigital.backend.dto.response.ProductoMediaResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoSugerenciaResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoImportResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoResponse;
import ar.com.mercadodigital.backend.service.BandejasOperativasService;
import ar.com.mercadodigital.backend.service.CategoriaProductoService;
import ar.com.mercadodigital.backend.service.ProveedorOwnershipService;
import ar.com.mercadodigital.backend.service.ProveedorProductoImportService;
import ar.com.mercadodigital.backend.service.ProveedorProductoService;

@WebMvcTest(ProveedorController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BandejasOperativasService bandejasOperativasService;

    @MockBean
    private ProveedorOwnershipService proveedorOwnershipService;

    @MockBean
    private ProveedorProductoService proveedorProductoService;

    @MockBean
    private ProveedorProductoImportService proveedorProductoImportService;

    @MockBean
    private CategoriaProductoService categoriaProductoService;

    @Test
    void listaProductosProveedor() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(proveedorProductoService.getProductos(eq(proveedorId), nullable(Authentication.class)))
                .thenReturn(List.of(ProveedorProductoResponse.builder()
                        .id(UUID.randomUUID())
                        .proveedorId(proveedorId)
                        .sku("SKU-1")
                        .nombre("Producto proveedor")
                        .estadoNormalizacion("PENDIENTE")
                        .precioCosto(new BigDecimal("1500.00"))
                        .stockDisponible(12)
                        .activo(true)
                        .cantidadMedia(0)
                        .tieneVideo(false)
                        .build()));

        mockMvc.perform(get("/proveedores/{proveedorId}/productos", proveedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-1"));
    }

    @Test
    void creaProductoProveedor() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        when(proveedorProductoService.crearProducto(eq(proveedorId), any(ProveedorProductoUpsertRequest.class), nullable(Authentication.class)))
                .thenReturn(ProveedorProductoResponse.builder()
                        .id(UUID.randomUUID())
                        .proveedorId(proveedorId)
                        .sku("SKU-1")
                        .nombre("Producto proveedor")
                        .estadoNormalizacion("PENDIENTE")
                        .precioCosto(new BigDecimal("1500.00"))
                        .stockDisponible(12)
                        .activo(true)
                        .cantidadMedia(0)
                        .tieneVideo(false)
                        .build());

        mockMvc.perform(post("/proveedores/{proveedorId}/productos", proveedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"SKU-1",
                                  "nombre":"Producto proveedor",
                                  "descripcion":"Descripcion",
                                  "precioCosto":1500.00,
                                  "stock":12,
                                  "categoriaPaquete":"CHICO",
                                  "imagenes":["https://cdn.local/prod-1.jpg","https://cdn.local/prod-2.jpg"],
                                  "activo":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Producto proveedor"));
    }

    @Test
    void desactivaProductoProveedor() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        when(proveedorProductoService.desactivarProducto(eq(proveedorId), eq(productoId), nullable(Authentication.class)))
                .thenReturn(ProveedorProductoResponse.builder()
                        .id(productoId)
                        .proveedorId(proveedorId)
                        .sku("SKU-1")
                        .nombre("Producto proveedor")
                        .estadoNormalizacion("PENDIENTE")
                        .precioCosto(new BigDecimal("1500.00"))
                        .stockDisponible(12)
                        .activo(false)
                        .cantidadMedia(0)
                        .tieneVideo(false)
                        .build());

        mockMvc.perform(delete("/proveedores/{proveedorId}/productos/{productoId}", proveedorId, productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        verify(proveedorProductoService).desactivarProducto(eq(proveedorId), eq(productoId), nullable(Authentication.class));
    }

    @Test
    void importaProductosProveedorDesdeArchivo() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "catalogo.csv",
                "text/csv",
                """
                Codigo,Descripcion,precio,ROS,MZA,CBA,LUG
                SKU-1,Producto importado,1500,1,2,3,4
                """.getBytes());

        when(proveedorProductoImportService.importar(eq(proveedorId), any(), nullable(Authentication.class)))
                .thenReturn(ProveedorProductoImportResponse.builder()
                        .filasLeidas(1)
                        .creados(1)
                        .actualizados(0)
                        .rechazados(0)
                        .errores(List.of())
                        .build());

        mockMvc.perform(multipart("/proveedores/{proveedorId}/productos/importacion", proveedorId)
                        .file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(1))
                .andExpect(jsonPath("$.creados").value(1))
                .andExpect(jsonPath("$.rechazados").value(0));
    }

    @Test
    void mediaProveedorSePuedeConsultarYEliminar() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        when(proveedorProductoService.getMedia(eq(proveedorId), eq(productoId), nullable(Authentication.class)))
                .thenReturn(List.of(ProductoMediaResponse.builder()
                        .id(mediaId)
                        .productoId(productoId)
                        .tipoMedia("IMAGEN")
                        .url("https://cdn.local/demo.png")
                        .orden(1)
                        .esPrincipal(true)
                        .build()));
        doNothing().when(proveedorProductoService).deleteMedia(eq(proveedorId), eq(productoId), eq(mediaId), nullable(Authentication.class));

        mockMvc.perform(get("/proveedores/{proveedorId}/productos/{productoId}/media", proveedorId, productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoMedia").value("IMAGEN"));

        mockMvc.perform(delete("/proveedores/{proveedorId}/productos/{productoId}/media/{mediaId}", proveedorId, productoId, mediaId))
                .andExpect(status().isNoContent());
    }

    @Test
    void categoriasProductoProveedorSePuedenListarYSugerir() throws Exception {
        UUID proveedorId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        when(categoriaProductoService.getCategoriasActivasProveedor(eq(proveedorId), nullable(Authentication.class)))
                .thenReturn(List.of(
                        ProveedorCategoriaProductoResponse.builder()
                                .id(categoriaId)
                                .nombre("Electrohogar")
                                .build()));
        when(categoriaProductoService.sugerirCategoriaProveedor(eq(proveedorId), any(), nullable(Authentication.class)))
                .thenReturn(ProveedorCategoriaProductoSugerenciaResponse.builder()
                        .categoriaId(categoriaId)
                        .nombre("Electrohogar")
                        .activa(false)
                        .estado("CREADA_PENDIENTE")
                        .mensaje("Categoria sugerida correctamente.")
                        .build());

        mockMvc.perform(get("/proveedores/{proveedorId}/categorias-producto", proveedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Electrohogar"));

        mockMvc.perform(post("/proveedores/{proveedorId}/categorias-producto/sugerencias", proveedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Electrohogar" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CREADA_PENDIENTE"));
    }
}
