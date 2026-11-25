package ar.com.mercadodigital.backend.dto.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevendedorMarkupOverrideRequest {

    private BigDecimal markupPorcentaje;
    private BigDecimal markupFijo;
    private BigDecimal comisionAsesorPorcentaje;
    private BigDecimal comisionAsesorFija;
    private Boolean activo;
}
