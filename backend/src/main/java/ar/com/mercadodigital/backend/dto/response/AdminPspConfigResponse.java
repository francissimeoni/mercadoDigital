package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminPspConfigResponse {
    private UUID id;
    private BigDecimal pspFeePorcentaje;
    private BigDecimal seguroTransportePorcentaje;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
