package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevendedorAsesorRequest {

    @NotBlank
    private String nombre;

    private String fotoUrl;

    @Size(max = 80)
    private String provincia;

    @Size(max = 120)
    private String localidad;

    @Size(max = 200)
    private String zonaAtencion;

    @Size(min = 2, max = 2)
    private String paisTelefono;

    private Boolean telefonoMovil;

    private Boolean quitarFoto;

    @Size(max = 40)
    private String whatsappComercial;

    private Boolean activo;

    @Email
    @Size(max = 180)
    private String email;

    @Size(min = 6, max = 120)
    private String password;
}
