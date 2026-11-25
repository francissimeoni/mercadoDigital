package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import ar.com.mercadodigital.backend.entity.TipoMedia;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevendedorProductoMediaResponse {
    UUID id;
    TipoMedia tipoMedia;
    String url;
    Integer orden;
    boolean esPrincipal;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
