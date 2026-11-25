package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProveedorProductoResponse {
    private UUID id;
    private UUID proveedorId;
    private UUID productoCanonicoId;
    private String productoCanonicoNombre;
    private UUID categoriaProductoId;
    private String categoriaProductoNombre;
    private String estadoNormalizacion;
    private String sku;
    private String nombre;
    private String descripcion;
    private BigDecimal precioCosto;
    private String moneda;
    private String categoriaPaquete;
    private Integer stockDisponible;
    private boolean activo;
    private Integer cantidadMedia;
    private String imagenPrincipalUrl;
    private boolean tieneVideo;
}
