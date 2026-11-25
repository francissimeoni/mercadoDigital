package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicPedidoDirectoRequest {

    private UUID revendedorId;
    private UUID proveedorId;

    @NotBlank
    private String nombreApellido;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telefonoWhatsapp;

    @NotBlank
    private String provincia;

    @NotBlank
    private String localidad;

    @NotBlank
    private String codigoPostal;

    @NotBlank
    private String calle;

    @NotBlank
    private String numero;

    private String pisoDepto;

    private String observaciones;

    private String codigoDescuento;

    private UUID productoCanonicoId;
    private UUID productoId;
    private UUID revendedorProductoId;

    @NotNull
    @Min(1)
    private Integer cantidad;

    private boolean seguroTransporte;

    private UUID asesorVentaId;

    private String metodoPago = "PSP";

    @AssertTrue
    private boolean aceptacionTerminos;

    @AssertTrue(message = "Debes enviar una unica referencia de producto valida")
    public boolean isReferenciaComercialValida() {
        boolean modoRevendedor = revendedorId != null && productoCanonicoId != null;
        boolean modoProveedor = proveedorId != null && productoId != null;
        boolean modoProductoPropio = revendedorId != null && revendedorProductoId != null;
        return (modoRevendedor ? 1 : 0) + (modoProveedor ? 1 : 0) + (modoProductoPropio ? 1 : 0) == 1;
    }
}
