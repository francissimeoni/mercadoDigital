package ar.com.mercadodigital.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.exception.BadRequestException;

@Service
public class MercadoPagoOAuthStateService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final long EXPIRATION_SECONDS = 30 * 60;

    private final ObjectMapper objectMapper;
    private final byte[] secretBytes;

    public MercadoPagoOAuthStateService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt-secret:md-dev-secret-cambiar-en-produccion}") String secret) {
        this.objectMapper = objectMapper;
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public OAuthStateToken generate(UUID revendedorId) {
        return generateFor(OAuthSubjectType.REVENDEDOR, revendedorId);
    }

    public OAuthStateToken generateProveedor(UUID proveedorId) {
        return generateFor(OAuthSubjectType.PROVEEDOR, proveedorId);
    }

    public OAuthStateToken generateFor(OAuthSubjectType subjectType, UUID subjectId) {
        try {
            long exp = Instant.now().plusSeconds(EXPIRATION_SECONDS).getEpochSecond();
            Map<String, Object> payload = Map.of(
                    "subjectType", subjectType.name(),
                    "subjectId", subjectId.toString(),
                    "nonce", UUID.randomUUID().toString(),
                    "exp", exp);
            String payloadBase64 = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signature = sign(payloadBase64);
            return new OAuthStateToken(
                    payloadBase64 + "." + signature,
                    LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar state OAuth de Mercado Pago.", ex);
        }
    }

    public UUID parseRevendedorId(String state) {
        OAuthStateSubject subject = parseSubject(state);
        if (subject.subjectType() != OAuthSubjectType.REVENDEDOR) {
            throw new BadRequestException("State OAuth no corresponde a revendedor.");
        }
        return subject.subjectId();
    }

    public UUID parseProveedorId(String state) {
        OAuthStateSubject subject = parseSubject(state);
        if (subject.subjectType() != OAuthSubjectType.PROVEEDOR) {
            throw new BadRequestException("State OAuth no corresponde a proveedor.");
        }
        return subject.subjectId();
    }

    public OAuthStateSubject parseSubject(String state) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 2) {
                throw new BadRequestException("State OAuth invalido.");
            }
            String expectedSignature = sign(parts[0]);
            if (!expectedSignature.equals(parts[1])) {
                throw new BadRequestException("State OAuth invalido.");
            }

            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[0]), MAP_TYPE);
            long exp = Long.parseLong(payload.get("exp").toString());
            if (Instant.now().getEpochSecond() > exp) {
                throw new BadRequestException("State OAuth expirado.");
            }
            OAuthSubjectType subjectType = OAuthSubjectType.valueOf(payload.get("subjectType").toString());
            UUID subjectId = UUID.fromString(payload.get("subjectId").toString());
            return new OAuthStateSubject(subjectType, subjectId);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("State OAuth invalido.");
        }
    }

    private String sign(String data) throws Exception {
        Mac sha256 = Mac.getInstance("HmacSHA256");
        sha256.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
        return URL_ENCODER.encodeToString(sha256.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public record OAuthStateToken(String value, LocalDateTime expiresAt) {
    }

    public record OAuthStateSubject(OAuthSubjectType subjectType, UUID subjectId) {
    }

    public enum OAuthSubjectType {
        REVENDEDOR,
        PROVEEDOR,
        MERCADO_DIGITAL
    }
}
