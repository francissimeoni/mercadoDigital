package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiberarProveedorRequest {

    @NotBlank(message = "motivo obligatorio")
    private String motivo;
}
