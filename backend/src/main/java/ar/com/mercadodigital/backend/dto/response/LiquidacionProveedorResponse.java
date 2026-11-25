package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiquidacionProveedorResponse {
    private UUID pedidoId;
    private String estadoLiquidacionProveedor;
    private LocalDateTime liberadoProveedorAt;
    private String motivoLiberacionManual;
    private BigDecimal totalProveedor;
}
