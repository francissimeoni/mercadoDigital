package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponderProductoPreguntaRequest {
    @NotBlank
    @Size(max = 4000)
    private String respuesta;
}
