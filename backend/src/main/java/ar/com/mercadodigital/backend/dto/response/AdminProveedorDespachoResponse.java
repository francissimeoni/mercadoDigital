package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminProveedorDespachoResponse {

    private UUID proveedorId;
    private String nombre;
    private boolean despachaMercaderia;
    private LocalDateTime updatedAt;
}
