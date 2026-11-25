package ar.com.mercadodigital.backend.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.*;
import jakarta.validation.constraints.*;

/** Contratos exclusivamente privados. No incluirlos en las respuestas públicas de tienda. */
public final class CatalogoAdminDto {
    private CatalogoAdminDto() {}

    public record ConfigInput(
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6) @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cotizacionUsd,
            @Size(max = 2000) String condicionesEntrega,
            @Size(max = 40) String telefonoComercial,
            @NotNull @Min(-1) Long version,
            @Size(max = 64) String impactoToken) {}
    public record CategoriaInput(
            @NotBlank @Size(max = 100) String nombre,
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal costoArs,
            @NotNull @Min(-1) Long version,
            @Size(max = 64) String impactoToken) {}
    public record ProductoInput(
            @NotBlank @Pattern(regexp = "ARS|USD") String moneda,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precioOriginal,
            @NotNull Boolean incluyeEnvio,
            UUID categoriaEnvioId,
            @NotNull @Min(-1) Long version,
            @Size(max = 64) String previewToken) {}
    public record Precio(String estado, List<String> pendientes, String monedaOriginal,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precioOriginal, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cotizacionUsd, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal baseArs,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal envioArs, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precioPublicoArs) {}
    public record Producto(UUID productoId, String nombre, String sku, String moneda,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precioOriginal, Boolean incluyeEnvio, UUID categoriaEnvioId,
            long version, Precio calculo, boolean activo) {}
    public record Config(@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cotizacionUsd, String condicionesEntrega, String telefonoComercial,
            long version, String modo, List<String> pendientes) {}
    public record Categoria(UUID id, String nombre, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal costoArs, long version) {}
    public record Cambio(UUID productoId, String nombre, Precio anterior, Precio propuesto) {}
    public record Impacto(List<Cambio> productos, String token) {}
    public record ProductoPreview(Precio calculo, String token) {}
}
