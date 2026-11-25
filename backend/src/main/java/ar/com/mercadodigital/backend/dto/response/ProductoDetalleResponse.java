package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductoDetalleResponse {
    UUID productoCanonicoId;
    String nombrePublico;
    String descripcion;
    BigDecimal mejorCostoOperativoEstimado;
    Integer demoraEstimadaDias;
    Integer stockDisponible;
    String imagenPrincipal;
}
