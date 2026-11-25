package ar.com.mercadodigital.backend.dto.request;

import ar.com.mercadodigital.backend.entity.TipoMedia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevendedorProductoMediaRequest {
    @NotNull
    private TipoMedia tipoMedia;
    @NotBlank
    @Size(max = 500)
    private String url;
    @NotNull
    @Min(0)
    private Integer orden;
    @NotNull
    private Boolean esPrincipal;
}
