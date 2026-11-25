package ar.com.mercadodigital.backend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminRevendedorProductoImportResponse {
    private Integer filasRecibidas;
    private Integer creados;
    private Integer actualizados;
    private Integer omitidos;
    private Integer rechazados;
    private List<AdminRevendedorProductoImportErrorResponse> errores;
}
