package ar.com.mercadodigital.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProveedorProductoImportErrorResponse {
    private Integer fila;
    private String sku;
    private String mensaje;
}
