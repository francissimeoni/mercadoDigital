package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.service.StorefrontService;
import java.util.List;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingEnvioResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingItemResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingResponse;
import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicPedidoTrackingService {

    private static final int DEMORA_DEFAULT_DIAS = 5;

    private final PedidoRepository pedidoRepository;
    private final ProductoMediaRepository productoMediaRepository;

    @Transactional(readOnly=true)
    public PublicPedidoTrackingResponse getByTrackingToken(String trackingToken, String storefrontHost) {
        Pedido pedido = this.getPedidoPublicoByTrackingToken(trackingToken, storefrontHost);
        return PublicPedidoTrackingResponse.builder().pedidoId(pedido.getId()).estadoPedido(pedido.getEstadoPedido().name()).estadoPago(pedido.getPago() != null ? pedido.getPago().getEstadoPago().name() : null).metodoPago(pedido.getPago() != null ? pedido.getPago().getMetodo() : null).transferenciaToken(transferenciaPendiente(pedido)).comprobanteCargado(pedido.getPago() != null && StringUtils.hasText(pedido.getPago().getComprobanteArchivo())).fechaCreacion(pedido.getCreatedAt()).demoraEstimadaDias(5).direccionEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getDireccionEnvio() : null).totalProducto(this.calcularTotalProducto(pedido.getTotalFinal(), pedido.getTotalEnvio(), pedido.getTotalSeguro(), pedido.getTotalDescuento())).totalEnvio(this.nvl(pedido.getTotalEnvio())).totalSeguro(this.nvl(pedido.getTotalSeguro())).totalDescuento(this.nvl(pedido.getTotalDescuento())).totalFinal(this.nvl(pedido.getTotalFinal())).codigoDescuentoAplicado(pedido.getCodigoDescuentoAplicado()).items(pedido.getItems().stream().map(item -> PublicPedidoTrackingItemResponse.builder().pedidoItemId(item.getId()).productoPublicacionId(item.getRevendedorProducto() != null ? item.getRevendedorProducto().getId() : (item.getProducto() != null && item.getProducto().getProductoCanonico() != null ? item.getProducto().getProductoCanonico().getId() : (item.getProducto() != null ? item.getProducto().getId() : null))).nombreProducto(item.getRevendedorProducto() != null ? item.getRevendedorProducto().getNombre() : this.resolverNombreProducto(item.getProducto().getProductoCanonico() != null ? item.getProducto().getProductoCanonico().getNombrePublico() : item.getProducto().getNombre())).cantidad(item.getCantidad()).imagenPrincipal(item.getRevendedorProducto() != null ? this.resolverImagenPrincipalPropia(item.getRevendedorProducto().getMedia()) : this.resolverImagenPrincipal(item.getProducto().getId())).build()).toList()).envio(PublicPedidoTrackingEnvioResponse.builder().estadoEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getEstadoEnvio().name() : null).trackingLogistica(pedido.getEnvio() != null ? pedido.getEnvio().getTracking() : null).transportistaNombre(pedido.getEnvio() != null && pedido.getEnvio().getTransportista() != null ? pedido.getEnvio().getTransportista().getNombre() : null).build()).build();
    }

    @Transactional(readOnly = true)
    public Pedido getPedidoPublicoByTrackingToken(String trackingToken, String storefrontHost) {
        Pedido pedido = pedidoRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if (!OrigenPedido.PUBLICO_DIRECTO.equals(pedido.getOrigenPedido())) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }

        validarStorefrontHost(pedido, storefrontHost);
        return pedido;
    }

    private void validarStorefrontHost(Pedido pedido, String storefrontHost) {
        if (!StringUtils.hasText(storefrontHost) || pedido.getRevendedor() == null || !StringUtils.hasText(pedido.getRevendedor().getDominio())) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        if (!StorefrontService.hostsEquivalentes(pedido.getRevendedor().getDominio(), storefrontHost)) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
    }

    private String transferenciaPendiente(Pedido pedido) {
        var pago = pedido.getPago();
        return pedido.getEstadoPedido() == PedidoEstado.CREADO
                && pago != null && pago.getEstadoPago() == PagoEstado.PENDIENTE
                && "TRANSFERENCIA".equalsIgnoreCase(pago.getMetodo())
                && !StringUtils.hasText(pago.getComprobanteArchivo()) ? pago.getTransferenciaToken() : null;
    }

    private BigDecimal calcularTotalProducto(BigDecimal totalFinal, BigDecimal totalEnvio, BigDecimal totalSeguro, BigDecimal totalDescuento) {
        return nvl(totalFinal).subtract(nvl(totalEnvio)).subtract(nvl(totalSeguro)).add(nvl(totalDescuento));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String resolverNombreProducto(String nombreProducto) {
        return nombreProducto != null ? nombreProducto : "Producto";
    }

    private String resolverImagenPrincipal(UUID productoId) {
        return productoMediaRepository.findByProducto_IdOrderByOrdenAsc(productoId).stream()
                .filter(media -> TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                .sorted(Comparator.comparing(ProductoMedia::isEsPrincipal).reversed()
                        .thenComparing(ProductoMedia::getOrden))
                .map(ProductoMedia::getUrl)
                .findFirst()
                .orElse(null);
    }

    private String resolverImagenPrincipalPropia(java.util.List<RevendedorProductoMedia> media) {
        return media.stream()
                .filter(item -> TipoMedia.IMAGEN.equals(item.getTipoMedia()))
                .sorted(Comparator.comparing(RevendedorProductoMedia::isEsPrincipal).reversed()
                        .thenComparing(RevendedorProductoMedia::getOrden))
                .map(RevendedorProductoMedia::getUrl)
                .findFirst()
                .orElse(null);
    }
}
