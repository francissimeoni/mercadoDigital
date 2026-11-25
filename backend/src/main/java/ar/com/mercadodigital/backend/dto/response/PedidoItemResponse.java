package ar.com.mercadodigital.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PedidoItemResponse {
    private UUID id;
    private UUID productoId;
    private UUID revendedorProductoId;
    private String productoNombre;
    private UUID proveedorId;
    private Integer cantidad;
    private BigDecimal precioCosto;
    private BigDecimal markupAplicado;
    private BigDecimal precioItemFinal;
    private Boolean tieneSeguro;
    private BigDecimal montoSeguro;
}
