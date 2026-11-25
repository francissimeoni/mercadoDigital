package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import ar.com.mercadodigital.backend.entity.TipoCalculoCostoMd;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCostoMdCategoriaResponse {

    private UUID id;
    private String categoriaPaquete;
    private BigDecimal monto;
    private TipoCalculoCostoMd tipoCalculo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
