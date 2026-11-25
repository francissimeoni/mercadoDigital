package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicPedidoTrackingResponse {
    private UUID pedidoId;
    private String estadoPedido;
    private String estadoPago;
    private String metodoPago;
    private String transferenciaToken;
    private boolean comprobanteCargado;
    private LocalDateTime fechaCreacion;
    private Integer demoraEstimadaDias;
    private String direccionEnvio;
    private BigDecimal totalProducto;
    private BigDecimal totalEnvio;
    private BigDecimal totalSeguro;
    private BigDecimal totalDescuento;
    private BigDecimal totalFinal;
    private String codigoDescuentoAplicado;
    private List<PublicPedidoTrackingItemResponse> items;
    private PublicPedidoTrackingEnvioResponse envio;
}
