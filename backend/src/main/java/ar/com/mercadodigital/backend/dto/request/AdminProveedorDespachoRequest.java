package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProveedorDespachoRequest {

    @NotNull
    private Boolean despachaMercaderia;
}
