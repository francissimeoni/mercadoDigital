package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DespacharEnvioRequest {

    private UUID transportistaId;

    private String transportistaNombre;

    @NotBlank
    private String tracking;
}
