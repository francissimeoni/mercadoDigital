package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicPedidoPagoPspResponse {
    private UUID pedidoId;
    private String estadoPedido;
    private String estadoPago;
    private String metodoPago;
    private BigDecimal monto;
    private String referenciaPedido;
    private String referenciaPago;
    private String checkoutUrl;
}
