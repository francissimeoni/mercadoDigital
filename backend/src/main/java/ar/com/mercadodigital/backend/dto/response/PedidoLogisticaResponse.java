package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PedidoLogisticaResponse {
    private UUID pedidoId;
    private UUID productoId;
    private String nombreProducto;
    private UUID proveedorId;
    private String nombreProveedor;
    private Integer cantidad;
    private String estadoEnvio;
    private String tracking;
    private LocalDateTime fechaCreacionPedido;
}
