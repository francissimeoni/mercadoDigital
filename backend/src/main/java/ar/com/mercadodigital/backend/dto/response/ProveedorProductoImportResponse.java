package ar.com.mercadodigital.backend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProveedorProductoImportResponse {
    private Integer filasLeidas;
    private Integer creados;
    private Integer actualizados;
    private Integer rechazados;
    private List<ProveedorProductoImportErrorResponse> errores;
}
