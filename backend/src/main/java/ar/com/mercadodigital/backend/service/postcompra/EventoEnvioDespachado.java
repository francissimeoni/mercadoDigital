package ar.com.mercadodigital.backend.service.postcompra;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoEnvioDespachado(
        UUID pedidoId,
        UUID revendedorId,
        String clienteEmail,
        String clienteNombre,
        String trackingToken,
        String estadoPedido,
        String estadoEnvio,
        String trackingLogistica,
        String transportistaNombre,
        LocalDateTime fechaEvento) {
}
