package ar.com.mercadodigital.backend.dto.request;

import java.math.BigDecimal;

import ar.com.mercadodigital.backend.entity.TipoCalculoCostoMd;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCostoMdCategoriaRequest {

    @NotBlank
    private String categoriaPaquete;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal monto;

    private TipoCalculoCostoMd tipoCalculo;
}
