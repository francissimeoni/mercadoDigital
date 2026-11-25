/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.Setter
public class StorefrontClientePerfilRequest {
    @NotBlank
    private String storefrontHost;
    @NotBlank
    @Size(max=120)
    private String nombre;
    @NotBlank
    @Size(max=80)
    private String telefonoWhatsapp;
    @NotBlank
    @Size(max=120)
    private String provincia;
    @NotBlank
    @Size(max=160)
    private String localidad;
    @NotBlank
    @Size(max=20)
    private String codigoPostal;
    @NotBlank
    @Size(max=180)
    private String calle;
    @NotBlank
    @Size(max=30)
    private String numero;
    @Size(max=80)
    private String pisoDepto;
    @Size(max=1000)
    private String observacionesEnvio;
}

