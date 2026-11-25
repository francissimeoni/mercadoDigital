package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicDisponibilidadRequest {
    @NotNull
    private UUID productoId;

    @NotNull
    private UUID revendedorId;

    @Size(max = 120)
    private String nombreContacto;

    @Email
    @Size(max = 180)
    private String emailContacto;

    @Size(max = 80)
    private String telefonoContacto;

    @Size(max = 160)
    private String localidad;

    @Size(max = 4000)
    private String observaciones;
}
