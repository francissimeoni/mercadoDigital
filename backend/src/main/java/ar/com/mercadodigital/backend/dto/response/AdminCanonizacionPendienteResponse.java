package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCanonizacionPendienteResponse {
    private UUID productoId;
    private String sku;
    private String nombre;
    private String descripcion;
    private BigDecimal precioCosto;
    private String moneda;
    private String categoriaPaquete;
    private Integer stockDisponible;
    private String imagenPrincipalUrl;
    private UUID proveedorId;
    private String proveedorNombre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
