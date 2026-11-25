package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import ar.com.mercadodigital.backend.entity.StorefrontEventoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicStorefrontEventoRequest {
    @NotBlank
    @Size(max = 255)
    private String host;

    @NotNull
    private StorefrontEventoTipo tipo;

    @NotBlank
    @Size(max = 100)
    private String visitanteId;

    private UUID productoId;
}
