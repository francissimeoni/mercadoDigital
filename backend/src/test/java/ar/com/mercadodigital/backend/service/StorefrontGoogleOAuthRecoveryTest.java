package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import ar.com.mercadodigital.backend.config.StorefrontGoogleOAuthProperties;
import ar.com.mercadodigital.backend.dto.request.StorefrontGoogleOAuthExchangeRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;
import ar.com.mercadodigital.backend.entity.StorefrontGoogleOAuthIntento;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.StorefrontGoogleOAuthIntentoRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;

class StorefrontGoogleOAuthRecoveryTest {
    private final StorefrontService storefront = mock(StorefrontService.class);
    private final StorefrontAuthService auth = mock(StorefrontAuthService.class);
    private final StorefrontGoogleOAuthIntentoRepository intents = mock(StorefrontGoogleOAuthIntentoRepository.class);
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private StorefrontGoogleOAuthService service;

    @BeforeEach
    void setup() {
        var properties = new StorefrontGoogleOAuthProperties();
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.setRedirectUri("https://central.test/callback");
        properties.setJwkSetUri("https://example.invalid/jwks");
        var builder = mock(RestClient.Builder.class);
        when(builder.build()).thenReturn(mock(RestClient.class));
        service = new StorefrontGoogleOAuthService(properties, storefront, auth, intents, users, builder);
    }

    @Test
    void authorizationBindsStateNonceAndPkceAndRejectsExternalReturnPath() throws Exception {
        String url = service.crearAuthorizationUrl("tienda.test", "//external.test");
        var query = UriComponentsBuilder.fromUriString(url).build().getQueryParams();
        var captor = ArgumentCaptor.forClass(StorefrontGoogleOAuthIntento.class);
        verify(intents).save(captor.capture());
        var intent = captor.getValue();
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(query.getFirst("state").getBytes(StandardCharsets.UTF_8))), intent.getStateHash());
        assertEquals(intent.getNonce(), query.getFirst("nonce"));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(intent.getCodeVerifier().getBytes(StandardCharsets.US_ASCII)));
        assertEquals(challenge, query.getFirst("code_challenge"));
        assertEquals("S256", query.getFirst("code_challenge_method"));
        assertEquals("/mi-cuenta", intent.getReturnPath());
        assertNotEquals(intent.getNonce(), intent.getCodeVerifier());
        verifyNoInteractions(auth, users);
    }

    @Test
    void expiredTicketCannotCreateSession() {
        var intent = validIntent();
        intent.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(intents.findByTicketHash(anyString())).thenReturn(Optional.of(intent));
        assertThrows(BadRequestException.class, () -> service.canjear(request("tienda.test")));
        verifyNoInteractions(auth, users);
    }

    @Test
    void ticketForOtherStoreCannotCreateSession() {
        when(intents.findByTicketHash(anyString())).thenReturn(Optional.of(validIntent()));
        assertThrows(BadRequestException.class, () -> service.canjear(request("otra.test")));
        verifyNoInteractions(auth, users);
    }

    @Test
    void ticketAllowsWwwAliasAndCanOnlyBeConsumedOnce() {
        var intent = validIntent();
        var user = new Usuario();
        user.setId(intent.getUsuarioId()); user.setEmail("customer@test.com"); user.setNombre("Cliente");
        var session = StorefrontAuthResponse.builder().email(user.getEmail()).token("test-token").build();
        when(intents.findByTicketHash(anyString())).thenReturn(Optional.of(intent));
        when(users.findById(intent.getUsuarioId())).thenReturn(Optional.of(user));
        when(auth.autenticarClienteGoogle("tienda.test", user.getEmail(), user.getNombre())).thenReturn(session);
        var response = service.canjear(request("www.tienda.test"));
        assertSame(session, response.getSession());
        assertEquals("/mi-cuenta", response.getReturnPath());
        assertNotNull(intent.getConsumedAt());
        assertThrows(BadRequestException.class, () -> service.canjear(request("tienda.test")));
        verify(auth, times(1)).autenticarClienteGoogle(anyString(), anyString(), anyString());
        verify(intents, times(1)).save(intent);
    }

    private StorefrontGoogleOAuthIntento validIntent() {
        var intent = new StorefrontGoogleOAuthIntento();
        intent.setStorefrontHost("tienda.test"); intent.setUsuarioId(UUID.randomUUID());
        intent.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5)); intent.setReturnPath("/mi-cuenta");
        return intent;
    }
    private StorefrontGoogleOAuthExchangeRequest request(String host) {
        var request = new StorefrontGoogleOAuthExchangeRequest();
        request.setStorefrontHost(host); request.setTicket("test-ticket"); return request;
    }
}
