package ar.com.mercadodigital.backend.dto.request;

import ar.com.mercadodigital.backend.entity.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUsuarioUpdateRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private TipoUsuario tipoUsuario;

    @NotNull
    private Boolean activo;

    private String nombreEntidad;

    private String dominioRevendedor;

    private Boolean proveedorDespachante;
}
