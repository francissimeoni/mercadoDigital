package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OfertaProductoResponse {
    private UUID productoId;
    private UUID productoCanonicoId;
    private BigDecimal precioFinalEstimado;
    private BigDecimal montoSeguroEstimado;
    private Integer stockDisponible;
    private Integer demoraEstimadaDias;
    private String categoriaPaquete;
}
