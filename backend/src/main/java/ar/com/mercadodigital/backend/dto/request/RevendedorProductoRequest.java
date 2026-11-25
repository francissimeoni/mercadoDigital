package ar.com.mercadodigital.backend.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevendedorProductoRequest {
    @NotBlank
    @Size(max = 80)
    private String sku;
    @NotBlank
    @Size(max = 180)
    private String nombre;
    private String descripcion;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal precioVenta;
    @Min(0)
    private Integer stock;
    @NotBlank
    @Size(max = 30)
    private String categoriaPaquete;
    private UUID categoriaProductoId;
    @NotNull
    private Boolean activo;
    @NotNull
    private Boolean promocion;
    @NotNull
    private Boolean novedad;
}
