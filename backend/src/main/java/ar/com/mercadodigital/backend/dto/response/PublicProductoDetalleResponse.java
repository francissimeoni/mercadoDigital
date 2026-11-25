package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicProductoDetalleResponse {
    UUID productoId;
    UUID productoCanonicoId;
    String nombrePublico;
    String descripcion;
    BigDecimal mejorPrecioFinalEstimado;
    BigDecimal seguroTransporteEstimado;
    BigDecimal totalConSeguroEstimado;
    Integer demoraEstimadaDias;
    Integer stockDisponible;
    String imagenPrincipal;
    List<String> imagenes;
    String origen;
    boolean promocion;
    boolean novedad;
    boolean modoCatalogo;
    String precioPublicoArs;
    String condicionesEntrega;
    String sku;
}
