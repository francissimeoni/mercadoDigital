package ar.com.mercadodigital.backend.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransferenciaComprobanteResponse {
    String estado;
    String mensaje;
    String trackingToken;
    String tiendaNombre;
    String telefono;
    String email;
}
