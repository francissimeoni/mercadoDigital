package ar.com.mercadodigital.backend.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductoMediaResponse {
    private UUID id;
    private UUID productoId;
    private String tipoMedia;
    private String url;
    private Integer orden;
    private boolean esPrincipal;
}
