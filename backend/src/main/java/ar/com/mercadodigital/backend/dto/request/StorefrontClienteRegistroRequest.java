package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorefrontClienteRegistroRequest {

    @NotBlank
    private String storefrontHost;

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 120)
    private String password;

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

    private String observacionesEnvio;
}
