package ar.com.mercadodigital.backend.service.postcompra;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoRmaReposicionEntregada(
        UUID rmaId,
        UUID pedidoId,
        UUID revendedorId,
        String clienteEmail,
        String clienteNombre,
        String trackingToken,
        String estadoRma,
        String productoNombre,
        String trackingReposicion,
        String transportistaNombre,
        LocalDateTime fechaEvento) {
}
