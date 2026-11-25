package ar.com.mercadodigital.backend.service.postcompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoRmaCerrado(
        UUID rmaId,
        UUID pedidoId,
        UUID revendedorId,
        String clienteEmail,
        String clienteNombre,
        String trackingToken,
        String estadoRma,
        String productoNombre,
        String resolucionFinal,
        BigDecimal montoCompensacion,
        String referenciaCompensacion,
        LocalDateTime fechaEvento) {
}
