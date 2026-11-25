package ar.com.mercadodigital.backend.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCostoMdConfigRequest {

    @Valid
    @NotEmpty
    private List<AdminCostoMdCategoriaRequest> items;
}
