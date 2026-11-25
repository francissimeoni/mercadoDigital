package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCanonizacionCandidatoResponse {
    private UUID productoCanonicoId;
    private String marca;
    private String modelo;
    private String nombrePublico;
    private LocalDateTime updatedAt;
}
