package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnvioResponse {
    private UUID pedidoId;
    private String estadoEnvio;
    private String tracking;
    private String transportistaNombre;
    private BigDecimal costoEnvio;
}
