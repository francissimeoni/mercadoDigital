package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductoResponse {
    private UUID id;
    private UUID productoCanonicoId;
    private String nombre;
    private String descripcion;
    private BigDecimal precioFinalEstimado;
    private String categoriaPaquete;
    private Integer stockDisponible;
    private Integer demoraEstimadaDias;
}
