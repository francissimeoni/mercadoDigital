package ar.com.mercadodigital.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MercadoPagoConfigurationDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoConfigurationDiagnostics.class);

    private final MercadoPagoProperties mercadoPagoProperties;

    public MercadoPagoConfigurationDiagnostics(MercadoPagoProperties mercadoPagoProperties) {
        this.mercadoPagoProperties = mercadoPagoProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mercadoPagoProperties.isEnabled()) {
            log.info("Mercado Pago retirado de MercadoDigital: cobros, OAuth, webhooks y payouts deshabilitados.");
            return;
        }
        MercadoPagoConfigurationStatus status = evaluate(mercadoPagoProperties);

        if (!status.missingFields().isEmpty()) {
            log.warn("Mercado Pago no esta listo para checkout real. Faltan configuraciones: {}", String.join(", ", status.missingFields()));
        } else {
            log.info(
                    "Mercado Pago configurado. baseUrl={}, returnUrl={}, webhookUrl={}, currency={}, publicKeyPresent={}",
                    nullSafe(mercadoPagoProperties.getBaseUrl()),
                    nullSafe(mercadoPagoProperties.getReturnUrl()),
                    nullSafe(mercadoPagoProperties.getWebhookUrl()),
                    nullSafe(mercadoPagoProperties.getCurrency()),
                    StringUtils.hasText(mercadoPagoProperties.getPublicKey()));
        }

        for (String warning : status.warnings()) {
            log.warn("Diagnostico Mercado Pago: {}", warning);
        }
    }

    static MercadoPagoConfigurationStatus evaluate(MercadoPagoProperties properties) {
        List<String> missingFields = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!StringUtils.hasText(properties.getAccessToken())) {
            warnings.add("MERCADOPAGO_ACCESS_TOKEN global no esta cargado. Solo funcionaran revendedores con vinculacion propia.");
        }
        if (!StringUtils.hasText(properties.getReturnUrl())) {
            missingFields.add("MERCADOPAGO_RETURN_URL");
        }
        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            missingFields.add("MERCADOPAGO_WEBHOOK_URL");
        }
        if (!StringUtils.hasText(properties.getClientId())
                || !StringUtils.hasText(properties.getClientSecret())
                || !StringUtils.hasText(properties.getRedirectUri())) {
            warnings.add("OAuth de Mercado Pago no esta listo. Revisar MERCADOPAGO_CLIENT_ID, MERCADOPAGO_CLIENT_SECRET y MERCADOPAGO_REDIRECT_URI.");
        }
        if (!StringUtils.hasText(properties.getAdminRedirectUrl())) {
            warnings.add("MERCADOPAGO_ADMIN_REDIRECT_URL no esta cargado. El callback OAuth no sabra a que pantalla admin volver.");
        }

        if (StringUtils.hasText(properties.getWebhookUrl()) && isLocalOnlyUrl(properties.getWebhookUrl())) {
            warnings.add("MERCADOPAGO_WEBHOOK_URL apunta a localhost o loopback. Mercado Pago no podra llamar ese webhook desde fuera de esta maquina.");
        }

        if (StringUtils.hasText(properties.getReturnUrl()) && isLocalOnlyUrl(properties.getReturnUrl())) {
            warnings.add("MERCADOPAGO_RETURN_URL apunta a localhost o loopback. Esto sirve para pruebas locales en este navegador, pero no para una vuelta publica compartible.");
        }
        if (StringUtils.hasText(properties.getRedirectUri()) && isLocalOnlyUrl(properties.getRedirectUri())) {
            warnings.add("MERCADOPAGO_REDIRECT_URI apunta a localhost o loopback. Sirve para pruebas locales, pero no para onboarding real desde fuera de esta maquina.");
        }
        if (StringUtils.hasText(properties.getAdminRedirectUrl()) && isLocalOnlyUrl(properties.getAdminRedirectUrl())) {
            warnings.add("MERCADOPAGO_ADMIN_REDIRECT_URL apunta a localhost o loopback. El cierre visual del OAuth quedara atado a esta maquina.");
        }

        return new MercadoPagoConfigurationStatus(List.copyOf(missingFields), List.copyOf(warnings));
    }

    private static boolean isLocalOnlyUrl(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            String normalized = host.trim().toLowerCase(Locale.ROOT);
            return normalized.equals("localhost")
                    || normalized.equals("127.0.0.1")
                    || normalized.equals("::1")
                    || normalized.endsWith(".local");
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private static String nullSafe(String value) {
        return StringUtils.hasText(value) ? value : "<vacio>";
    }

    record MercadoPagoConfigurationStatus(List<String> missingFields, List<String> warnings) {
    }
}
