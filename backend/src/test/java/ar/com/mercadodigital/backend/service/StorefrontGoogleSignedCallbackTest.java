package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.com.mercadodigital.backend.config.StorefrontGoogleOAuthProperties;
import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;
import ar.com.mercadodigital.backend.entity.StorefrontGoogleOAuthIntento;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.repository.StorefrontGoogleOAuthIntentoRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

// Real HTTP/token decoder/signature validation, exclusively on ephemeral loopback endpoints.
class StorefrontGoogleSignedCallbackTest {
    private HttpServer server;
    private RSAKey key;
    private String token;
    private String submittedForm;
    private StorefrontGoogleOAuthService service;
    private StorefrontGoogleOAuthIntento intent;
    private final StorefrontGoogleOAuthIntentoRepository intents = mock(StorefrontGoogleOAuthIntentoRepository.class);
    private final StorefrontAuthService auth = mock(StorefrontAuthService.class);
    private final UsuarioRepository users = mock(UsuarioRepository.class);

    @BeforeEach void setup() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("local-test").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.createContext("/token", exchange -> {
            submittedForm = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = ("{\"id_token\":\"" + token + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        var properties = new StorefrontGoogleOAuthProperties();
        properties.setClientId("local-client"); properties.setClientSecret("local-secret");
        properties.setRedirectUri(base + "/callback"); properties.setTokenUrl(base + "/token"); properties.setJwkSetUri(base + "/jwks");
        service = new StorefrontGoogleOAuthService(properties, mock(StorefrontService.class), auth, intents, users, RestClient.builder());
        intent = new StorefrontGoogleOAuthIntento();
        intent.setNonce("expected-nonce"); intent.setCodeVerifier("local-verifier"); intent.setStorefrontHost("shop.test");
        intent.setReturnPath("/mi-cuenta"); intent.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        when(intents.findByStateHash(anyString())).thenReturn(Optional.of(intent));
    }

    @AfterEach void stop() { if (server != null) server.stop(0); }

    private String signed(String variant) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(variant.equals("issuer") ? "https://attacker.test" : "https://accounts.google.com")
            .audience(variant.equals("audience") ? "another-client" : "local-client").subject("google-subject")
            .issueTime(Date.from(now.minusSeconds(600))).expirationTime(Date.from(now.plusSeconds(variant.equals("expired") ? -120 : 300)))
            .claim("nonce", variant.equals("nonce") ? "other-nonce" : "expected-nonce")
            .claim("email", "customer@test.com").claim("email_verified", !variant.equals("unverified")).claim("name", "Cliente").build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(variant.equals("signature") ? new RSAKeyGenerator(2048).generate() : key));
        return jwt.serialize();
    }

    @Test void signedCallbackValidatesIdentityAndIssuesOneTicket() throws Exception {
        token = signed("valid");
        var user = new Usuario(); user.setId(UUID.randomUUID()); user.setEmail("customer@test.com");
        when(auth.autenticarClienteGoogle("shop.test", "customer@test.com", "Cliente")).thenReturn(StorefrontAuthResponse.builder().email(user.getEmail()).build());
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        String redirect = service.procesarCallback("local-code", "local-state");
        assertTrue(redirect.startsWith("https://shop.test/ingresar/google?ticket="));
        assertTrue(submittedForm.contains("code_verifier=local-verifier"));
        assertTrue(submittedForm.contains("grant_type=authorization_code"));
        assertEquals(user.getId(), intent.getUsuarioId()); assertNotNull(intent.getTicketHash());
        assertNotNull(intent.getCompletedAt());
        assertThrows(RuntimeException.class, () -> service.procesarCallback("local-code", "local-state"));
        verify(auth, times(1)).autenticarClienteGoogle(anyString(), anyString(), anyString());
    }

    @ParameterizedTest @ValueSource(strings = {"issuer", "audience", "nonce", "expired", "signature", "unverified"})
    void invalidIdentityNeverCreatesSessionOrTicket(String variant) throws Exception {
        token = signed(variant);
        assertThrows(RuntimeException.class, () -> service.procesarCallback("local-code", "local-state"));
        verifyNoInteractions(auth, users); verify(intents, never()).save(any());
        assertNull(intent.getTicketHash()); assertNull(intent.getCompletedAt());
    }
}
