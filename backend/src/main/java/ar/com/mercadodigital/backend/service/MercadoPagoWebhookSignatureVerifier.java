package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Verifica el manifiesto firmado del webhook antes de consultar un pago externo. */
@Service
public class MercadoPagoWebhookSignatureVerifier {
    private final MercadoPagoProperties properties;

    public MercadoPagoWebhookSignatureVerifier(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    public void verify(String signature, String requestId, String dataId) {
        String secret = properties.getWebhookSecret();
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(signature)
                || !StringUtils.hasText(requestId) || !StringUtils.hasText(dataId)
                || signature.length() > 256 || requestId.length() > 128 || dataId.length() > 128) {
            reject();
        }
        String ts = null, hash = null;
        for (String item : signature.split(",")) {
            String[] pair = item.trim().split("=", 2);
            if (pair.length == 2 && "ts".equals(pair[0])) ts = pair[1];
            if (pair.length == 2 && "v1".equals(pair[0])) hash = pair[1];
        }
        if (ts == null || !ts.matches("[0-9]{10,16}") || hash == null || !hash.matches("[a-fA-F0-9]{64}")) {
            reject();
        }
        try {
            String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] calculated = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(calculated, HexFormat.of().parseHex(hash))) reject();
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            reject();
        }
    }

    private void reject() {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma del webhook inválida");
    }
}
