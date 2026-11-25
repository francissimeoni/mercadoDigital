package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificacionResponse {
    private UUID id;
    private UUID usuarioId;
    private String tipo;
    private String titulo;
    private String mensaje;
    private boolean leida;
    private LocalDateTime createdAt;
}
