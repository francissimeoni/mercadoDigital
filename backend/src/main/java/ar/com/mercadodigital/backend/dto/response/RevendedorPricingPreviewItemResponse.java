package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevendedorPricingPreviewItemResponse {
    private UUID productoCanonicoId;
    private UUID productoBaseId;
    private String nombrePublico;
    private BigDecimal costoOperativo;
    private BigDecimal markupPorcentaje;
    private BigDecimal markupFijo;
    private BigDecimal comisionAsesorPorcentaje;
    private BigDecimal comisionAsesorFija;
    private BigDecimal markupBruto;
    private BigDecimal comisionAsesorMonto;
    private BigDecimal gananciaNetaRevendedor;
    private BigDecimal precioFinalEstimado;
    private Integer demoraEstimadaDias;
    private Integer stockDisponible;
    private boolean overrideActivo;
    private String origenConfiguracion;
    private String imagenPrincipal;
}
