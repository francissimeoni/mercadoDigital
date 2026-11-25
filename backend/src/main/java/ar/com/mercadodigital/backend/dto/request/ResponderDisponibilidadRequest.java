package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponderDisponibilidadRequest {
    @NotNull
    private Boolean disponible;

    @Size(max = 4000)
    private String respuestaProveedor;
}
