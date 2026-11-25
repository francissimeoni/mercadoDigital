package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitarRmaRequest {

    @NotNull
    private UUID pedidoItemId;

    @NotBlank
    private String motivo;

    private String descripcion;
}
