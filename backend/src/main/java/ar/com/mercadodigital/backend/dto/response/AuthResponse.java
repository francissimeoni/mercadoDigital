package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private LocalDateTime issuedAt;
    private String revendedorId;
    private String proveedorId;
    private String clienteId;
}
