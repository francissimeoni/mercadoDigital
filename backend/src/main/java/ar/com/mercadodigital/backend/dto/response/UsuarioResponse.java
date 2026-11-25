package ar.com.mercadodigital.backend.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UsuarioResponse {
    private UUID id;
    private String nombre;
    private String email;
}
