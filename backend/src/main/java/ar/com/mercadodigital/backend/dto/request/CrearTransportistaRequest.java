package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearTransportistaRequest {

    @NotBlank
    private String nombre;
}
