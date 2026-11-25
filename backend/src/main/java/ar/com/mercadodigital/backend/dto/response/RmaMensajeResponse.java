package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RmaMensajeResponse {
    private UUID id;
    private String autorTipo;
    private String autorNombre;
    private String mensaje;
    private boolean interno;
    private List<RmaAdjuntoResponse> adjuntos;
    private LocalDateTime createdAt;
}
