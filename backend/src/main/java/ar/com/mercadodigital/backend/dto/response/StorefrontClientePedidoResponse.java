/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.dto.response;

import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingEnvioResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingItemResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@lombok.Getter
@lombok.Builder
public class StorefrontClientePedidoResponse {
    private UUID pedidoId;
    private String trackingToken;
    private LocalDateTime fechaCreacion;
    private String estadoPedido;
    private String estadoPago;
    private String metodoPago;
    private BigDecimal totalFinal;
    private List<PublicPedidoTrackingItemResponse> items;
    private PublicPedidoTrackingEnvioResponse envio;
    private String transferenciaToken;
    private boolean comprobanteCargado;
}

