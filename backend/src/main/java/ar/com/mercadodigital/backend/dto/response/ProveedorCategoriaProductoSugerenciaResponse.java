package ar.com.mercadodigital.backend.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProveedorCategoriaProductoSugerenciaResponse {

    private UUID categoriaId;
    private String nombre;
    private boolean activa;
    private String estado;
    private String mensaje;
}
