package ar.com.mercadodigital.backend.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransportistaResponse {
    UUID id;
    String nombre;
}
