package ar.com.mercadodigital.backend.service.split;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.split-payout.transport", havingValue = "noop")
public class SplitPayoutGatewayNoop implements SplitPayoutGateway {

    private static final Logger log = LoggerFactory.getLogger(SplitPayoutGatewayNoop.class);

    @Override
    public SplitPayoutResult intentarPayoutRetirado(SplitPayoutRequest request) {
        log.info(
                "Payout real no-op para pedido={} (fase base). proveedor={}, revendedor={}, md={}, envio={}, seguro={}, psp={}",
                request.pedidoId(),
                request.totalesSplit().totalProveedor(),
                request.totalesSplit().totalRevendedor(),
                request.totalesSplit().totalMd(),
                request.totalesSplit().totalEnvio(),
                request.totalesSplit().totalSeguro(),
                request.totalesSplit().totalPsp());
        return new SplitPayoutResult(null, null, "NOOP", null);
    }
}
