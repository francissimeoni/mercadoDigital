package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevendedorDespacharPedidoRequest {
    @Size(max = 120)
    private String transportistaNombre;

    @Size(max = 120)
    private String tracking;
}
