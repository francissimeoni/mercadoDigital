package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import java.math.BigDecimal;
import java.util.Comparator;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.PedidoLogisticaResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoProveedorResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoRevendedorResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoRevendedorItemResponse;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.repository.PedidoItemRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BandejasOperativasService {

    private final PedidoItemRepository pedidoItemRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public List<PedidoProveedorResponse> getPedidosPendientesProveedor(UUID proveedorId) {
        return pedidoItemRepository.findByProveedor_IdAndPedido_Envio_EstadoEnvioIn(
                proveedorId,
                List.of(EnvioEstado.PENDIENTE_PREPARACION, EnvioEstado.LISTO_PARA_RETIRO))
                .stream()
                .map(this::toProveedorResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoLogisticaResponse> getRetirosPendientes() {
        return pedidoItemRepository.findByPedido_Envio_EstadoEnvio(EnvioEstado.LISTO_PARA_RETIRO).stream()
                .map(this::toLogisticaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoLogisticaResponse> getDespachosPendientes() {
        return pedidoItemRepository.findByPedido_Envio_EstadoEnvio(EnvioEstado.RETIRADO).stream()
                .map(this::toLogisticaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoRevendedorResponse> getPedidosRevendedor(UUID revendedorId) {
        return pedidoRepository.findByRevendedor_Id(revendedorId).stream()
                .map(this::toRevendedorResponse)
                .toList();
    }

    private PedidoProveedorResponse toProveedorResponse(PedidoItem item) {
        Pedido pedido = item.getPedido();
        return PedidoProveedorResponse.builder()
                .pedidoId(pedido.getId())
                .itemId(item.getId())
                .productoId(item.getProducto().getId())
                .nombreProducto(item.getProducto().getNombre())
                .cantidad(item.getCantidad())
                .direccionEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getDireccionEnvio() : null)
                .estadoEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getEstadoEnvio().name() : null)
                .fechaCreacionPedido(pedido.getCreatedAt())
                .build();
    }

    private PedidoLogisticaResponse toLogisticaResponse(PedidoItem item) {
        Pedido pedido = item.getPedido();
        return PedidoLogisticaResponse.builder()
                .pedidoId(pedido.getId())
                .productoId(item.getProducto().getId())
                .nombreProducto(item.getProducto().getNombre())
                .proveedorId(item.getProveedor().getId())
                .nombreProveedor(item.getProveedor().getNombre())
                .cantidad(item.getCantidad())
                .estadoEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getEstadoEnvio().name() : null)
                .tracking(pedido.getEnvio() != null ? pedido.getEnvio().getTracking() : null)
                .fechaCreacionPedido(pedido.getCreatedAt())
                .build();
    }

    private PedidoRevendedorResponse toRevendedorResponse(Pedido pedido) {
        return PedidoRevendedorResponse.builder().pedidoId(pedido.getId()).estadoPedido(pedido.getEstadoPedido().name()).estadoEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getEstadoEnvio().name() : null).direccionEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getDireccionEnvio() : null).categoriaPaquete(pedido.getEnvio() != null ? pedido.getEnvio().getCategoriaPaquete() : null).transportistaNombre(pedido.getEnvio() != null && pedido.getEnvio().getTransportista() != null ? pedido.getEnvio().getTransportista().getNombre() : null).totalFinal(pedido.getTotalFinal()).totalBruto(pedido.getTotalBruto()).totalEnvio(pedido.getTotalEnvio()).totalSeguro(pedido.getTotalSeguro()).totalPsp(pedido.getTotalPsp()).totalDescuento(pedido.getTotalDescuento()).tracking(pedido.getEnvio() != null ? pedido.getEnvio().getTracking() : null).fechaCreacionPedido(pedido.getCreatedAt()).fechaActualizacionPedido(pedido.getUpdatedAt()).clienteNombre(pedido.getCliente() != null ? pedido.getCliente().getNombre() : null).clienteEmail(pedido.getCliente() != null ? pedido.getCliente().getEmail() : null).clienteTelefono(pedido.getCliente() != null ? pedido.getCliente().getTelefonoWhatsapp() : null).metodoPago(pedido.getPago() != null ? pedido.getPago().getMetodo() : null).estadoPago(pedido.getPago() != null ? pedido.getPago().getEstadoPago().name() : null).montoPago(pedido.getPago() != null ? pedido.getPago().getMonto() : null).referenciaPago(pedido.getPago() != null ? pedido.getPago().getPspReferencia() : null).fechaActualizacionPago(pedido.getPago() != null ? pedido.getPago().getUpdatedAt() : null).comprobanteCargado(pedido.getPago() != null && pedido.getPago().getComprobanteArchivo() != null).comprobanteCargadoAt(pedido.getPago() != null ? pedido.getPago().getComprobanteCargadoAt() : null).items(pedido.getItems().stream().map(item -> {
            String nombre = item.getRevendedorProducto() != null ? item.getRevendedorProducto().getNombre() : (item.getProducto() != null && item.getProducto().getProductoCanonico() != null ? item.getProducto().getProductoCanonico().getNombrePublico() : (item.getProducto() != null ? item.getProducto().getNombre() : "Producto"));
            String sku = item.getRevendedorProducto() != null ? item.getRevendedorProducto().getSku() : (item.getProducto() != null ? item.getProducto().getSku() : null);
            return PedidoRevendedorItemResponse.builder().itemId(item.getId()).productoId(item.getProducto() != null ? item.getProducto().getId() : null).revendedorProductoId(item.getRevendedorProducto() != null ? item.getRevendedorProducto().getId() : null).sku(sku).nombre(nombre).imagenPrincipal(this.resolverImagenPrincipal((PedidoItem)item)).cantidad(item.getCantidad()).precioUnitario(item.getPrecioItemFinal()).subtotal(item.getPrecioItemFinal().multiply(BigDecimal.valueOf(item.getCantidad().intValue()))).build();
        }).toList()).build();
    }

    private final ProductoMediaRepository productoMediaRepository;

    private String resolverImagenPrincipal(PedidoItem item) {
        if (item.getRevendedorProducto() != null) {
            return item.getRevendedorProducto().getMedia().stream().filter(media -> TipoMedia.IMAGEN.equals(media.getTipoMedia())).sorted(Comparator.comparing((RevendedorProductoMedia media) -> !media.isEsPrincipal()).thenComparing(media -> media.getOrden() != null ? media.getOrden() : Integer.MAX_VALUE)).map(RevendedorProductoMedia::getUrl).filter(url -> url != null && !url.isBlank()).findFirst().orElse(null);
        }
        if (item.getProducto() == null || item.getProducto().getId() == null) {
            return null;
        }
        return this.productoMediaRepository.findByProducto_IdAndEsPrincipalTrue(item.getProducto().getId()).filter(media -> TipoMedia.IMAGEN.equals((Object)media.getTipoMedia())).or(() -> this.productoMediaRepository.findByProducto_IdOrderByOrdenAsc(item.getProducto().getId()).stream().filter(media -> TipoMedia.IMAGEN.equals((Object)media.getTipoMedia())).findFirst()).map(media -> media.getUrl()).filter(url -> !url.isBlank()).orElse(null);
    }
}
