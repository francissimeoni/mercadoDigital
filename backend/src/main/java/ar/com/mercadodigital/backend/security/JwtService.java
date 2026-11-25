package ar.com.mercadodigital.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secretBytes;
    private final long expiracionSegundos;
    private final long refreshExpiracionSegundos;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt-secret:md-dev-secret-cambiar-en-produccion}") String jwtSecret,
            @Value("${app.security.jwt-expiration-seconds:3600}") long expiracionSegundos,
            @Value("${app.security.jwt-refresh-expiration-seconds:2592000}") long refreshExpiracionSegundos) {
        this.objectMapper = objectMapper;
        this.secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.expiracionSegundos = expiracionSegundos;
        this.refreshExpiracionSegundos = refreshExpiracionSegundos;
    }

    public String generarToken(UUID usuarioId, String email, List<String> roles) {
        return generarToken(usuarioId, email, roles, "access", expiracionSegundos);
    }

    public String generarRefreshToken(UUID usuarioId, String email) {
        return generarToken(usuarioId, email, List.of(), "refresh", refreshExpiracionSegundos);
    }

    private String generarToken(UUID usuarioId, String email, List<String> roles, String type, long durationSeconds) {
        try {
            String header = toBase64Json(Map.of("alg", "HS256", "typ", "JWT"));
            long exp = Instant.now().plusSeconds(durationSeconds).getEpochSecond();
            String payload = toBase64Json(Map.of(
                    "sub", email,
                    "uid", usuarioId.toString(),
                    "roles", roles,
                    "type", type,
                    "exp", exp));
            String signature = firmar(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar JWT", ex);
        }
    }

    public ClaimsJwt validarYParsear(String token) {
        return validarYParsear(token, false);
    }

    public ClaimsJwt validarRefreshToken(String token) {
        return validarYParsear(token, true);
    }

    private ClaimsJwt validarYParsear(String token, boolean requireRefresh) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT invalido");
            }
            String firmado = parts[0] + "." + parts[1];
            String firmaEsperada = firmar(firmado);
            if (!firmaEsperada.equals(parts[2])) {
                throw new IllegalArgumentException("Firma JWT invalida");
            }

            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), MAP_TYPE);
            long exp = Long.parseLong(payload.get("exp").toString());
            if (Instant.now().getEpochSecond() > exp) {
                throw new IllegalArgumentException("JWT expirado");
            }

            String type = payload.get("type") == null ? null : payload.get("type").toString();
            if (requireRefresh && !"refresh".equals(type)) {
                throw new IllegalArgumentException("JWT de renovacion invalido");
            }
            if (!requireRefresh && "refresh".equals(type)) {
                throw new IllegalArgumentException("JWT de acceso invalido");
            }

            String sub = payload.get("sub").toString();
            UUID uid = UUID.fromString(payload.get("uid").toString());
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) payload.get("roles");
            return new ClaimsJwt(uid, sub, roles);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Token JWT invalido", ex);
        }
    }

    private String toBase64Json(Map<String, Object> content) throws Exception {
        byte[] bytes = objectMapper.writeValueAsBytes(content);
        return URL_ENCODER.encodeToString(bytes);
    }

    private String firmar(String data) throws Exception {
        Mac sha256 = Mac.getInstance("HmacSHA256");
        sha256.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
        byte[] firma = sha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return URL_ENCODER.encodeToString(firma);
    }

    public record ClaimsJwt(UUID usuarioId, String email, List<String> roles) {
    }


    public String generarRefreshToken(UUID usuarioId, String email, List<String> roles) {
        return this.generarToken(usuarioId, email, roles, "refresh", this.refreshExpiracionSegundos);
    }
}
