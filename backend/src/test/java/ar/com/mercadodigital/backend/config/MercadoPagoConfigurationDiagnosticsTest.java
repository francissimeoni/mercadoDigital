package ar.com.mercadodigital.backend.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MercadoPagoConfigurationDiagnosticsTest {

    @Test
    void detectaConfiguracionFaltanteYWebhookLocal() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setReturnUrl("http://localhost:5173/pago/retorno");
        properties.setWebhookUrl("http://localhost:8080/webhooks/psp/mercadopago");

        MercadoPagoConfigurationDiagnostics.MercadoPagoConfigurationStatus status =
                MercadoPagoConfigurationDiagnostics.evaluate(properties);

        assertTrue(status.missingFields().contains("MERCADOPAGO_RETURN_URL") == false);
        assertTrue(status.missingFields().contains("MERCADOPAGO_WEBHOOK_URL") == false);
        assertTrue(status.warnings().stream().anyMatch(warning -> warning.contains("MERCADOPAGO_ACCESS_TOKEN")));
        assertTrue(status.warnings().stream().anyMatch(warning -> warning.contains("MERCADOPAGO_WEBHOOK_URL")));
        assertTrue(status.warnings().stream().anyMatch(warning -> warning.contains("MERCADOPAGO_RETURN_URL")));
    }

    @Test
    void noMarcaAdvertenciasCuandoLaConfiguracionEsPublicaYCompleta() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setAccessToken("test-access-token");
        properties.setClientId("123456");
        properties.setClientSecret("secret-demo");
        properties.setRedirectUri("https://backend-demo.ngrok.app/public/mercadopago/oauth/callback");
        properties.setAdminRedirectUrl("https://app-demo.ngrok.app/admin/storefronts");
        properties.setReturnUrl("https://tienda-demo.ngrok.app/pago/retorno");
        properties.setWebhookUrl("https://backend-demo.ngrok.app/webhooks/psp/mercadopago");

        MercadoPagoConfigurationDiagnostics.MercadoPagoConfigurationStatus status =
                MercadoPagoConfigurationDiagnostics.evaluate(properties);

        assertTrue(status.missingFields().isEmpty());
        assertTrue(status.warnings().isEmpty());
    }
}
