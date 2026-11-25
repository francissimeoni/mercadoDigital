package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.request.PublicStorefrontEventoRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.CategoriaProducto;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.StorefrontEvento;
import ar.com.mercadodigital.backend.entity.StorefrontEventoTipo;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.repository.ProductoPreguntaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.StorefrontEventoRepository;

@ExtendWith(MockitoExtension.class)
class StorefrontMetricasServiceTest {
    @Test void rankingEnriquecePortadasEnUnLoteYSinEliminarHistoricos() {
        UUID tienda=UUID.randomUUID(), producto=UUID.randomUUID(), historico=UUID.randomUUID();
        var a=org.mockito.Mockito.mock(StorefrontEventoRepository.RankingProducto.class);
        var b=org.mockito.Mockito.mock(StorefrontEventoRepository.RankingProducto.class);
        when(a.getProductoId()).thenReturn(producto);when(a.getNombre()).thenReturn("Actual");when(a.getTotal()).thenReturn(3L);
        when(b.getProductoId()).thenReturn(historico);when(b.getNombre()).thenReturn("Histórico eliminado");when(b.getTotal()).thenReturn(1L);
        when(eventoRepository.rankingProductos(org.mockito.ArgumentMatchers.eq(tienda),any(),any(),any(),any())).thenReturn(java.util.List.of(a,b));
        var p=new RevendedorProducto();p.setId(producto);
        var m=new ar.com.mercadodigital.backend.entity.RevendedorProductoMedia();m.setId(UUID.randomUUID());m.setTipoMedia(ar.com.mercadodigital.backend.entity.TipoMedia.IMAGEN);m.setUrl("/foto.jpg");m.setEsPrincipal(true);p.getMedia().add(m);
        when(productoRepository.findByRevendedor_IdAndIdIn(org.mockito.ArgumentMatchers.eq(tienda),any())).thenReturn(java.util.List.of(p));
        var result=service.obtener(tienda,java.time.LocalDate.now().minusDays(6),java.time.LocalDate.now());
        assertEquals(2,result.getProductosMasVistos().size());assertEquals("/foto.jpg",result.getProductosMasVistos().get(0).getImagenPrincipalUrl());
        org.junit.jupiter.api.Assertions.assertNull(result.getProductosMasVistos().get(1).getImagenPrincipalUrl());
        verify(productoRepository).findByRevendedor_IdAndIdIn(tienda,java.util.List.of(producto,historico));
        org.mockito.Mockito.verifyNoMoreInteractions(productoRepository);
    }

    @Mock private StorefrontService storefrontService;
    @Mock private RevendedorRepository revendedorRepository;
    @Mock private RevendedorProductoRepository productoRepository;
    @Mock private ProductoPreguntaRepository preguntaRepository;
    @Mock private StorefrontEventoRepository eventoRepository;
    private StorefrontMetricasService service;

    @BeforeEach
    void setUp() {
        service = new StorefrontMetricasService(storefrontService, revendedorRepository, productoRepository,
                preguntaRepository, eventoRepository);
    }

    @Test
    void registraProductoSoloDentroDeLaTiendaResueltaYAnonimizaVisitante() {
        UUID revendedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setNombre("Notebooks");
        RevendedorProducto producto = new RevendedorProducto();
        producto.setId(productoId);
        producto.setNombre("Equipo profesional");
        producto.setCategoriaProducto(categoria);

        when(storefrontService.resolverPorHost("tienda.com.ar")).thenReturn(StorefrontResponse.builder()
                .tipoStorefront(TipoStorefront.REVENDEDOR).revendedorId(revendedorId).build());
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(productoRepository.findByIdAndRevendedor_Id(productoId, revendedorId)).thenReturn(Optional.of(producto));
        when(eventoRepository.save(any(StorefrontEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PublicStorefrontEventoRequest request = new PublicStorefrontEventoRequest();
        request.setHost("tienda.com.ar");
        request.setTipo(StorefrontEventoTipo.VISTA_PRODUCTO);
        request.setVisitanteId("identificador-del-browser");
        request.setProductoId(productoId);
        service.registrar(request);

        ArgumentCaptor<StorefrontEvento> captor = ArgumentCaptor.forClass(StorefrontEvento.class);
        verify(eventoRepository).save(captor.capture());
        StorefrontEvento evento = captor.getValue();
        verify(productoRepository).findByIdAndRevendedor_Id(productoId, revendedorId);
        assertEquals(revendedorId, evento.getRevendedor().getId());
        assertEquals("Equipo profesional", evento.getProductoNombre());
        assertEquals("Notebooks", evento.getRubroNombre());
        assertEquals(64, evento.getVisitanteHash().length());
        assertNotEquals("identificador-del-browser", evento.getVisitanteHash());
    }
}
