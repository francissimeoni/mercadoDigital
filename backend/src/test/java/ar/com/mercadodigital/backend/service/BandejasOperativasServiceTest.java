package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.PedidoLogisticaResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoProveedorResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoRevendedorResponse;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.repository.PedidoItemRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class BandejasOperativasServiceTest {

    @Mock
    private PedidoItemRepository pedidoItemRepository;
    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private BandejasOperativasService bandejasOperativasService;

    @Test
    void proveedorVeSoloSusPedidos() {
        UUID proveedorId = UUID.randomUUID();
        PedidoItem itemProveedor = item(proveedorId, "Proveedor A", EnvioEstado.PENDIENTE_PREPARACION);

        when(pedidoItemRepository.findByProveedor_IdAndPedido_Envio_EstadoEnvioIn(
                org.mockito.ArgumentMatchers.eq(proveedorId),
                org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(itemProveedor));

        List<PedidoProveedorResponse> response = bandejasOperativasService.getPedidosPendientesProveedor(proveedorId);

        assertEquals(1, response.size());
        assertEquals(itemProveedor.getPedido().getId(), response.get(0).getPedidoId());
        assertEquals(itemProveedor.getId(), response.get(0).getItemId());
        assertEquals("PENDIENTE_PREPARACION", response.get(0).getEstadoEnvio());
    }

    @Test
    void logisticaVeSoloRetirosYDespachosSegunEndpoint() {
        PedidoItem listo = item(UUID.randomUUID(), "Proveedor R", EnvioEstado.LISTO_PARA_RETIRO);
        PedidoItem retirado = item(UUID.randomUUID(), "Proveedor D", EnvioEstado.RETIRADO);

        when(pedidoItemRepository.findByPedido_Envio_EstadoEnvio(EnvioEstado.LISTO_PARA_RETIRO))
                .thenReturn(List.of(listo));
        when(pedidoItemRepository.findByPedido_Envio_EstadoEnvio(EnvioEstado.RETIRADO))
                .thenReturn(List.of(retirado));

        List<PedidoLogisticaResponse> retiros = bandejasOperativasService.getRetirosPendientes();
        List<PedidoLogisticaResponse> despachos = bandejasOperativasService.getDespachosPendientes();

        assertEquals(1, retiros.size());
        assertEquals("LISTO_PARA_RETIRO", retiros.get(0).getEstadoEnvio());
        assertEquals(1, despachos.size());
        assertEquals("RETIRADO", despachos.get(0).getEstadoEnvio());
    }

    @Test
    void revendedorVeSoloSusPedidos() {
        UUID revendedorId = UUID.randomUUID();

        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID());
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        pedido.setTotalFinal(new BigDecimal("1234.50"));
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        pedido.setRevendedor(revendedor);
        Envio envio = new Envio();
        envio.setPedido(pedido);
        envio.setEstadoEnvio(EnvioEstado.DESPACHADO);
        envio.setTracking("TRK-1");
        pedido.setEnvio(envio);

        when(pedidoRepository.findByRevendedor_Id(revendedorId)).thenReturn(List.of(pedido));

        List<PedidoRevendedorResponse> response = bandejasOperativasService.getPedidosRevendedor(revendedorId);

        assertEquals(1, response.size());
        assertEquals("PAGADO", response.get(0).getEstadoPedido());
        assertEquals("DESPACHADO", response.get(0).getEstadoEnvio());
        assertEquals("TRK-1", response.get(0).getTracking());
    }

    private PedidoItem item(UUID proveedorId, String nombreProveedor, EnvioEstado estadoEnvio) {
        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID());
        Envio envio = new Envio();
        envio.setPedido(pedido);
        envio.setEstadoEnvio(estadoEnvio);
        envio.setDireccionEnvio("Direccion 123");
        pedido.setEnvio(envio);

        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        proveedor.setNombre(nombreProveedor);

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Producto X");

        PedidoItem item = new PedidoItem();
        item.setId(UUID.randomUUID());
        item.setPedido(pedido);
        item.setProveedor(proveedor);
        item.setProducto(producto);
        item.setCantidad(2);
        return item;
    }
}
