package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProveedorCategoriaProductoSugerenciaRequest {

    @NotBlank
    @Size(max = 120)
    private String nombre;
}
