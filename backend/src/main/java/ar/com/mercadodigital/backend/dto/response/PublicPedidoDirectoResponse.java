package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicPedidoDirectoResponse {
    private UUID pedidoId;
    private String estadoPedido;
    private String estadoPago;
    private String metodoPago;
    private BigDecimal totalProducto;
    private BigDecimal totalEnvio;
    private BigDecimal totalSeguro;
    private BigDecimal totalDescuento;
    private BigDecimal totalFinal;
    private Integer demoraEstimadaDias;
    private String trackingToken;
    private String transferenciaToken;
}
