package ar.com.mercadodigital.backend.service.split;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.exception.IntegracionPspException;
import ar.com.mercadodigital.backend.service.MercadoPagoClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.split-payout.transport", havingValue = "mercadopago")
public class SplitPayoutGatewayMercadoPago implements SplitPayoutGateway {

    private final MercadoPagoClient mercadoPagoClient;

    @Override
    public SplitPayoutResult intentarPayoutRetirado(SplitPayoutRequest request) {
        if (request == null) {
            throw new IntegracionPspException("Request de payout vacio.");
        }
        if (!StringUtils.hasText(request.accessTokenOrigen())) {
            throw new IntegracionPspException("No hay token de origen para payout.");
        }
        List<MercadoPagoClient.MercadoPagoPayoutTransaction> transactions = request.destinos().stream()
                .filter(destino -> destino != null
                        && destino.monto() != null
                        && destino.monto().signum() > 0
                        && StringUtils.hasText(destino.email()))
                .map(destino -> new MercadoPagoClient.MercadoPagoPayoutTransaction(
                        destino.actor(),
                        destino.email().trim(),
                        destino.monto(),
                        "Liquidacion " + destino.actor() + " pedido " + request.pedidoId()))
                .toList();
        if (transactions.isEmpty()) {
            throw new IntegracionPspException("No hay destinos validos para payout.");
        }

        MercadoPagoClient.MercadoPagoPayoutData payout = mercadoPagoClient.crearPayout(
                request.accessTokenOrigen(),
                request.pedidoId().toString(),
                transactions);

        return new SplitPayoutResult(
                payout.payoutId(),
                payout.idempotencyKey(),
                payout.status(),
                payout.payloadRespuestaJson());
    }
}
