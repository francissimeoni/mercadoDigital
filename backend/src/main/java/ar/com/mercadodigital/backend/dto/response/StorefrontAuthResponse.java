package ar.com.mercadodigital.backend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StorefrontAuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private String nombre;
    private String issuedAt;
    private String revendedorId;
    private String clienteId;
    private String asesorId;
    private List<String> roles;
}
