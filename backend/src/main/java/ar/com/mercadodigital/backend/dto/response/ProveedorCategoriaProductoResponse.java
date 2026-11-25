package ar.com.mercadodigital.backend.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProveedorCategoriaProductoResponse {

    private UUID id;
    private String nombre;
}
