package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingResponse;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;

@ExtendWith(MockitoExtension.class)
class PublicPedidoTrackingServiceTest {

    private static final String STOREFRONT_HOST = "www.mercado-digital.online";

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProductoMediaRepository productoMediaRepository;

    @InjectMocks
    private PublicPedidoTrackingService publicPedidoTrackingService;

    @Test
    void trackingExitosoDevuelvePedidoPublicoSeguro() {
        String trackingToken = "tracking-publico-1";
        UUID productoId = UUID.randomUUID();
        Pedido pedido = pedidoPublicoBase(trackingToken, productoId);

        ProductoMedia media = new ProductoMedia();
        media.setProducto(pedido.getItems().get(0).getProducto());
        media.setTipoMedia(TipoMedia.IMAGEN);
        media.setUrl("https://cdn.local/producto.png");
        media.setOrden(1);
        media.setEsPrincipal(true);

        when(pedidoRepository.findByTrackingToken(trackingToken)).thenReturn(Optional.of(pedido));
        when(productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId)).thenReturn(List.of(media));

        PublicPedidoTrackingResponse response = publicPedidoTrackingService.getByTrackingToken(trackingToken, STOREFRONT_HOST);

        assertEquals(pedido.getId(), response.getPedidoId());
        assertEquals("CREADO", response.getEstadoPedido());
        assertEquals("PENDIENTE", response.getEstadoPago());
        assertEquals("PSP", response.getMetodoPago());
        assertEquals("Calle 123, La Plata", response.getDireccionEnvio());
        assertEquals(new BigDecimal("100.00"), response.getTotalProducto());
        assertEquals("Producto publico", response.getItems().get(0).getNombreProducto());
        assertEquals("https://cdn.local/producto.png", response.getItems().get(0).getImagenPrincipal());
        assertEquals("PENDIENTE_PREPARACION", response.getEnvio().getEstadoEnvio());
        assertNull(response.getEnvio().getTrackingLogistica());
    }

    @Test
    void tokenInexistenteDevuelve404() {
        when(pedidoRepository.findByTrackingToken("tracking-inexistente")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> publicPedidoTrackingService.getByTrackingToken("tracking-inexistente", STOREFRONT_HOST));
    }

    @Test
    void pedidoOperativoNoEsAccesiblePorTrackingPublico() {
        Pedido pedido = pedidoPublicoBase("tracking-operativo", UUID.randomUUID());
        pedido.setOrigenPedido(OrigenPedido.OPERATIVO);
        when(pedidoRepository.findByTrackingToken("tracking-operativo")).thenReturn(Optional.of(pedido));

        assertThrows(ResourceNotFoundException.class,
                () -> publicPedidoTrackingService.getByTrackingToken("tracking-operativo", STOREFRONT_HOST));
    }

    @Test
    void trackingPublicoConHostDeOtroTenantDevuelve404() {
        String trackingToken = "tracking-publico-otro-tenant";
        Pedido pedido = pedidoPublicoBase(trackingToken, UUID.randomUUID());
        when(pedidoRepository.findByTrackingToken(trackingToken)).thenReturn(Optional.of(pedido));

        assertThrows(ResourceNotFoundException.class,
                () -> publicPedidoTrackingService.getByTrackingToken(trackingToken, "www.vidrieraurbana.com"));
    }

    private Pedido pedidoPublicoBase(String trackingToken, UUID productoId) {
        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID());
        pedido.setTrackingToken(trackingToken);
        pedido.setOrigenPedido(OrigenPedido.PUBLICO_DIRECTO);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setTotalEnvio(BigDecimal.ZERO);
        pedido.setTotalSeguro(new BigDecimal("20.00"));
        pedido.setTotalFinal(new BigDecimal("120.00"));
        pedido.setCreatedAt(LocalDateTime.now());

        Revendedor revendedor = new Revendedor();
        revendedor.setDominio(STOREFRONT_HOST);
        pedido.setRevendedor(revendedor);

        ProductoCanonico canonico = new ProductoCanonico();
        canonico.setNombrePublico("Producto publico");

        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setNombre("Producto interno");
        producto.setProductoCanonico(canonico);

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setCantidad(1);
        pedido.setItems(List.of(item));

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMetodo("PSP");
        pago.setMonto(new BigDecimal("120.00"));
        pedido.setPago(pago);

        Envio envio = new Envio();
        envio.setPedido(pedido);
        envio.setEstadoEnvio(EnvioEstado.PENDIENTE_PREPARACION);
        envio.setDireccionEnvio("Calle 123, La Plata");
        pedido.setEnvio(envio);
        return pedido;
    }
}
