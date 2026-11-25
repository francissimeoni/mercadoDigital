package ar.com.mercadodigital.backend.dto.request;

import java.math.BigDecimal;
import java.util.List;
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
public class ProveedorProductoUpsertRequest {

    @NotBlank
    @Size(max = 80)
    private String sku;

    @NotBlank
    @Size(max = 180)
    private String nombre;

    @Size(max = 5000)
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal precioCosto;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotBlank
    @Size(max = 30)
    private String categoriaPaquete;

    @Size(max = 3)
    private List<@NotBlank @Size(max = 2048) String> imagenes;

    private UUID categoriaProductoId;

    private UUID productoCanonicoId;

    private Boolean activo;
}
