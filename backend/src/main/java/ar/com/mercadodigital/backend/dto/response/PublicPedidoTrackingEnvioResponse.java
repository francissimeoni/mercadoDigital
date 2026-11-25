package ar.com.mercadodigital.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicPedidoTrackingEnvioResponse {
    private String estadoEnvio;
    private String trackingLogistica;

    private String transportistaNombre;
}
