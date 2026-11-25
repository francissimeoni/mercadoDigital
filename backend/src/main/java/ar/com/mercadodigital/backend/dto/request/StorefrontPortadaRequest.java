package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StorefrontPortadaRequest {
    private UUID id;

    @NotBlank
    @Size(max = 500)
    private String imagenUrl;

    @Size(max = 180)
    private String titulo;

    @Size(max = 255)
    private String subtitulo;
}
