package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RmaAdjuntoResponse {
    private UUID id;
    private String nombreArchivo;
    private String contentType;
    private long tamanoBytes;
    private String dataUrl;
    private LocalDateTime createdAt;
}
