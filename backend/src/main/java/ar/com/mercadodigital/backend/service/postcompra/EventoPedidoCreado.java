package ar.com.mercadodigital.backend.service.postcompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoPedidoCreado(
        UUID pedidoId,
        UUID revendedorId,
        String clienteEmail,
        String clienteNombre,
        String trackingToken,
        String estadoPedido,
        BigDecimal total,
        String moneda,
        LocalDateTime fechaEvento) {
}
