package ar.com.mercadodigital.backend.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StorefrontPortadaResponse {
    UUID id;
    String imagenUrl;
    String titulo;
    String subtitulo;
}
