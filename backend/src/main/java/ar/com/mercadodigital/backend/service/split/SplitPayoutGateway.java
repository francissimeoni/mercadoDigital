package ar.com.mercadodigital.backend.service.split;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SplitPayoutGateway {

    SplitPayoutResult intentarPayoutRetirado(SplitPayoutRequest request);

    record TotalesSplit(
            BigDecimal totalProveedor,
            BigDecimal totalRevendedor,
            BigDecimal totalMd,
            BigDecimal totalEnvio,
            BigDecimal totalSeguro,
            BigDecimal totalPsp) {
    }

    record DestinoPayout(String actor, String email, BigDecimal monto) {
    }

    record SplitPayoutRequest(
            UUID pedidoId,
            String accessTokenOrigen,
            TotalesSplit totalesSplit,
            List<DestinoPayout> destinos) {
    }

    record SplitPayoutResult(
            String payoutId,
            String idempotencyKey,
            String estado,
            String payloadRespuestaJson) {
    }
}
