package ar.com.mercadodigital.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminRevendedorProductoImportErrorResponse {
    private Integer fila;
    private String sku;
    private String mensaje;
}
