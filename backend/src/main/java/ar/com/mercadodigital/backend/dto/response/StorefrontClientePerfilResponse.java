/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.dto.response;


@lombok.Getter
@lombok.Builder
public class StorefrontClientePerfilResponse {
    private String nombre;
    private String email;
    private String telefonoWhatsapp;
    private String provincia;
    private String localidad;
    private String codigoPostal;
    private String calle;
    private String numero;
    private String pisoDepto;
    private String observacionesEnvio;
}

