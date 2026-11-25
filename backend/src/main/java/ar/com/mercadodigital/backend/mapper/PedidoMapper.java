package ar.com.mercadodigital.backend.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.com.mercadodigital.backend.dto.response.PedidoItemResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoResponse;
import ar.com.mercadodigital.backend.entity.Pedido;

@Component
public class PedidoMapper {

    public PedidoResponse toResponse(Pedido pedido) {
        List<PedidoItemResponse> itemResponses = pedido.getItems().stream()
                .map(item -> PedidoItemResponse.builder()
                        .id(item.getId())
                        .productoId(item.getProducto() != null ? item.getProducto().getId() : null)
                        .revendedorProductoId(item.getRevendedorProducto() != null
                                ? item.getRevendedorProducto().getId() : null)
                        .productoNombre(item.getRevendedorProducto() != null
                                ? item.getRevendedorProducto().getNombre()
                                : item.getProducto().getNombre())
                        .proveedorId(item.getProveedor() != null ? item.getProveedor().getId() : null)
                        .cantidad(item.getCantidad())
                        .precioCosto(item.getPrecioCosto())
                        .markupAplicado(item.getMarkupAplicado())
                        .precioItemFinal(item.getPrecioItemFinal())
                        .tieneSeguro(item.isTieneSeguro())
                        .montoSeguro(item.getMontoSeguro())
                        .build())
                .toList();

        return PedidoResponse.builder()
                .id(pedido.getId())
                .revendedorId(pedido.getRevendedor().getId())
                .clienteId(pedido.getCliente().getId())
                .estadoPedido(pedido.getEstadoPedido().name())
                .totalBruto(pedido.getTotalBruto())
                .totalPsp(pedido.getTotalPsp())
                .totalEnvio(pedido.getTotalEnvio())
                .totalSeguro(pedido.getTotalSeguro())
                .totalDescuento(pedido.getTotalDescuento())
                .totalFinal(pedido.getTotalFinal())
                .items(itemResponses)
                .estadoPago(pedido.getPago() != null ? pedido.getPago().getEstadoPago().name() : null)
                .estadoEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getEstadoEnvio().name() : null)
                .codigoDescuentoAplicado(pedido.getCodigoDescuentoAplicado())
                .trackingToken(pedido.getTrackingToken())
                .createdAt(pedido.getCreatedAt())
                .updatedAt(pedido.getUpdatedAt())
                .build();
    }
}
