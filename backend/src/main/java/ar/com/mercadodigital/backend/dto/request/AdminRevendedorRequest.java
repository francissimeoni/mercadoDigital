package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRevendedorRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String dominio;

    @Email
    private String usuarioEmail;

    private String usuarioPassword;

    @NotNull
    private Boolean activo;
}
