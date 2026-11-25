package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicCatalogoItemResponse {
    UUID productoId;
    UUID productoCanonicoId;
    String nombrePublico;
    String categoriaProducto;
    BigDecimal mejorPrecioFinalEstimado;
    Integer demoraEstimadaDias;
    Integer stockDisponible;
    Integer cantidadOfertas;
    String imagenPrincipal;
    String origen;
    boolean promocion;
    boolean novedad;
    boolean modoCatalogo;
    String precioPublicoArs;
}
