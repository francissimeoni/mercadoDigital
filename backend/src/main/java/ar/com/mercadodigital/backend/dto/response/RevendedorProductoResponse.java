package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevendedorProductoResponse {
    UUID id;
    String sku;
    String nombre;
    String descripcion;
    BigDecimal precioVenta;
    String moneda;
    Integer stock;
    String categoriaPaquete;
    UUID categoriaProductoId;
    String categoriaProductoNombre;
    boolean activo;
    boolean promocion;
    boolean novedad;
    List<RevendedorProductoMediaResponse> media;
    String imagenPrincipalUrl;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
