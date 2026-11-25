package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PedidoRevendedorItemResponse {
    private UUID itemId;
    private UUID productoId;
    private UUID revendedorProductoId;
    private String sku;
    private String nombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    private String imagenPrincipal;
}
