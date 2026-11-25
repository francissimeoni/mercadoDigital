package ar.com.mercadodigital.backend.service.postcompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoPagoConfirmado(
        UUID pedidoId,
        UUID pagoId,
        UUID revendedorId,
        String clienteEmail,
        String clienteNombre,
        String trackingToken,
        String estadoPedido,
        BigDecimal montoConfirmado,
        String moneda,
        LocalDateTime fechaConfirmacion) {
}
