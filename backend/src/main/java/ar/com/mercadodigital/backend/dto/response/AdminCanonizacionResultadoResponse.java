package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCanonizacionResultadoResponse {
    private UUID productoId;
    private String sku;
    private String nombreProductoProveedor;
    private UUID proveedorId;
    private String proveedorNombre;
    private UUID productoCanonicoId;
    private String productoCanonicoNombre;
    private String accion;
    private LocalDateTime updatedAt;
}
