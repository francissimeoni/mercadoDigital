package ar.com.mercadodigital.backend.service.postcompra;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoRmaEnvioSolicitado(
        UUID rmaId,
        UUID pedidoId,
        UUID revendedorId,
        String clienteEmail,
        String clienteNombre,
        String trackingToken,
        String estadoRma,
        String productoNombre,
        LocalDateTime fechaEvento) {
}
