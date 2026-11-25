package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogoItemResponse {
    private UUID productoCanonicoId;
    private String nombrePublico;
    private BigDecimal mejorCostoOperativoEstimado;
    private Integer demoraEstimadaDias;
    private Integer stockDisponible;
    private Integer cantidadOfertas;
    private String imagenPrincipal;
}
