package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevendedorPricingConfigResponse {
    private UUID revendedorId;
    private boolean modoCatalogo;
    private BigDecimal markupPorcentaje;
    private BigDecimal markupFijo;
    private BigDecimal comisionAsesorPorcentaje;
    private BigDecimal comisionAsesorFija;
    private String codigoDescuento;
    private BigDecimal descuentoFijo;
}
