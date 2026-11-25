package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PedidoResponse {
    private UUID id;
    private UUID revendedorId;
    private UUID clienteId;
    private String estadoPedido;
    private BigDecimal totalBruto;
    private BigDecimal totalPsp;
    private BigDecimal totalEnvio;
    private BigDecimal totalSeguro;
    private BigDecimal totalDescuento;
    private BigDecimal totalFinal;
    private List<PedidoItemResponse> items;
    private String estadoPago;
    private String estadoEnvio;
    private String codigoDescuentoAplicado;
    private String trackingToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
