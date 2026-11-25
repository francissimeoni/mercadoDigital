package ar.com.mercadodigital.backend.dto.request;

import ar.com.mercadodigital.backend.entity.TipoMedia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoMediaRequest {

    @NotNull
    private TipoMedia tipoMedia;

    @NotBlank
    private String url;

    private Integer orden;

    private Boolean esPrincipal;
}
