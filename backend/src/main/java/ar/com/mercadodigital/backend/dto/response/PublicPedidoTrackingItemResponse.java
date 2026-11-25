package ar.com.mercadodigital.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class PublicPedidoTrackingItemResponse {
    private UUID pedidoItemId;
    private String nombreProducto;
    private Integer cantidad;
    private String imagenPrincipal;

    private UUID productoPublicacionId;
}
