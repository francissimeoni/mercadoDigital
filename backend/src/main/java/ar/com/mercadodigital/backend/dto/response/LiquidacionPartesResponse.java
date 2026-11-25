package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiquidacionPartesResponse {
    private UUID pedidoId;
    private String estadoLiquidacionProveedor;
    private LocalDateTime liberadoProveedorAt;
    private String origenLiberacion;
    private BigDecimal totalProveedor;
    private BigDecimal totalRevendedor;
    private BigDecimal totalMd;
    private BigDecimal totalEnvio;
    private BigDecimal totalSeguro;
    private BigDecimal totalPsp;
    private BigDecimal totalComponentes;
    private String payoutEstado;
    private String payoutId;
    private String payoutErrorUltimo;
    private LocalDateTime payoutUltimoIntentoAt;
    private LocalDateTime payoutCompletadoAt;
}
