package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCategoriaProductoResponse {

    private UUID id;
    private String nombre;
    private String slug;
    private boolean activa;
    private boolean sugerida;
    private UUID sugeridaPorProveedorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
