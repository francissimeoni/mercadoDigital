package ar.com.mercadodigital.backend.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRevendedorProductoImportRequest {

    @NotEmpty
    @Size(max = 2000)
    private List<@Valid Item> items = new ArrayList<>();

    private Boolean actualizarNombre = true;
    private Boolean actualizarDescripcion = true;
    private Boolean actualizarPrecio = true;
    private Boolean actualizarStock = true;
    private Boolean actualizarCategoria = true;
    private Boolean actualizarCategoriaPaquete = true;
    private Boolean actualizarEstado = true;
    private Boolean actualizarDestacados = true;
    private Boolean actualizarImagenes = true;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        private Integer fila;
        private Boolean incluir = true;
        private String sku;
        private String nombre;
        private String descripcion;
        private BigDecimal precioVenta;
        private Integer stock;
        private String categoriaPaquete;
        private UUID categoriaProductoId;
        private Boolean activo;
        private Boolean promocion;
        private Boolean novedad;
        @Size(max = 5)
        private List<String> imagenes = new ArrayList<>();
        private String modoImagenes;
    }
}
