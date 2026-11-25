package ar.com.mercadodigital.backend.service.postcompra;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoEnvioListoParaRetiro(
        UUID pedidoId,
        UUID revendedorId,
        String clienteEmail,
        String clienteNombre,
        String trackingToken,
        String estadoPedido,
        String estadoEnvio,
        LocalDateTime fechaEvento) {
}
