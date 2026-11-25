package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MercadoPagoWebhookSignatureVerifierTest {
    @Test
    void firmaValidaYAlteraciones() throws Exception {
        var config = new MercadoPagoProperties();
        config.setWebhookSecret("solo-pruebas-locales");
        var verifier = new MercadoPagoWebhookSignatureVerifier(config);
        String manifest = "id:12345;request-id:solicitud-1;ts:1742505638683;";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(config.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "ts=1742505638683,v1=" + HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

        assertDoesNotThrow(() -> verifier.verify(signature, "solicitud-1", "12345"));
        assertThrows(ResponseStatusException.class, () -> verifier.verify(signature, "solicitud-1", "54321"));
        assertThrows(ResponseStatusException.class, () -> verifier.verify(signature, "solicitud-ajena", "12345"));
        assertThrows(ResponseStatusException.class, () -> verifier.verify(null, "solicitud-1", "12345"));
    }

    @Test
    void sinClaveConfiguradaNoAceptaNotificaciones() {
        var verifier = new MercadoPagoWebhookSignatureVerifier(new MercadoPagoProperties());
        assertThrows(ResponseStatusException.class, () -> verifier.verify("ts=1742505638683,v1=" + "0".repeat(64), "solicitud-1", "12345"));
    }
}
