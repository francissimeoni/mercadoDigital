package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgregarRmaMensajeRequest {

    @NotBlank
    private String mensaje;

    private boolean interno;
}
