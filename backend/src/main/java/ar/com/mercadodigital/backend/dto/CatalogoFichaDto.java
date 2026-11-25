package ar.com.mercadodigital.backend.dto;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ar.com.mercadodigital.backend.dto.response.RevendedorProductoResponse;

/** Contrato privado de ficha y precio; nunca se reutiliza como respuesta pública. */
public final class CatalogoFichaDto {
    private CatalogoFichaDto() {}
    public record Imagen(UUID id, @NotBlank @Size(max = 500) String url) {}
    public record Archivo(@NotBlank @Size(max = 180) String nombre,
            @Positive @Max(5242880) long size, @Pattern(regexp = "[a-f0-9]{64}") @NotNull String sha256) {}
    public record Input(@NotNull UUID operacionId,
            @NotBlank @Size(max = 80) String sku,
            @NotBlank @Size(max = 180) String nombre,
            @Size(max = 20000) String descripcion,
            UUID categoriaProductoId, @NotNull Boolean activo,
            @NotNull Boolean promocion, @NotNull Boolean novedad,
            @NotNull @Min(-1) Long versionFicha,
            @NotNull @Valid CatalogoAdminDto.ProductoInput precio,
            @NotNull @Size(max = 20) List<@NotNull @Valid Imagen> imagenes,
            @NotNull @Size(max = 5) List<@NotNull @Valid Archivo> archivos,
            @Size(max = 64) String previewToken) {}
    public record Ficha(RevendedorProductoResponse datos, long versionFicha,
            CatalogoAdminDto.Producto precio) {}
}
