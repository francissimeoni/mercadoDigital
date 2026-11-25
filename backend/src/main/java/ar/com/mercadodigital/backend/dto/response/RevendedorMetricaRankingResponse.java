package ar.com.mercadodigital.backend.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevendedorMetricaRankingResponse {
    UUID productoId;
    String nombre;
    long total;
    String imagenPrincipalUrl;
}
