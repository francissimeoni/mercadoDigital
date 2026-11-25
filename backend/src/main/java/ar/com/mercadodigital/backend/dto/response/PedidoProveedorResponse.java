package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PedidoProveedorResponse {
    private UUID pedidoId;
    private UUID itemId;
    private UUID productoId;
    private String nombreProducto;
    private Integer cantidad;
    private String direccionEnvio;
    private String estadoEnvio;
    private LocalDateTime fechaCreacionPedido;
}
