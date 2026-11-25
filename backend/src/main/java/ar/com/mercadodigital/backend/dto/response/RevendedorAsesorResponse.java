package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevendedorAsesorResponse {
    private UUID id;
    private UUID revendedorId;
    private String nombre;
    private String email;
    private String fotoUrl;
    private String provincia;
    private String localidad;
    private String zonaAtencion;
    private String whatsappComercial;
    private boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
