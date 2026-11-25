/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.config.StorefrontGoogleOAuthProperties;
import ar.com.mercadodigital.backend.dto.request.StorefrontGoogleOAuthExchangeRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontGoogleOAuthExchangeResponse;
import ar.com.mercadodigital.backend.entity.StorefrontGoogleOAuthIntento;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.IntegracionPspException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.StorefrontGoogleOAuthIntentoRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.service.StorefrontAuthService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class StorefrontGoogleOAuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final long INTENT_TTL_MINUTES = 10L;
    private static final long TICKET_TTL_MINUTES = 5L;
    private final StorefrontGoogleOAuthProperties properties;
    private final StorefrontService storefrontService;
    private final StorefrontAuthService storefrontAuthService;
    private final StorefrontGoogleOAuthIntentoRepository intentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RestClient restClient;
    private final JwtDecoder jwtDecoder;

    public StorefrontGoogleOAuthService(StorefrontGoogleOAuthProperties properties, StorefrontService storefrontService, StorefrontAuthService storefrontAuthService, StorefrontGoogleOAuthIntentoRepository intentoRepository, UsuarioRepository usuarioRepository, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.storefrontService = storefrontService;
        this.storefrontAuthService = storefrontAuthService;
        this.intentoRepository = intentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.restClient = restClientBuilder.build();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(new JwtTimestampValidator(), this.issuerValidator(), this.audienceValidator()));
        this.jwtDecoder = decoder;
    }

    public boolean estaHabilitado() {
        return StringUtils.hasText(this.properties.getClientId()) && StringUtils.hasText(this.properties.getClientSecret()) && StringUtils.hasText(this.properties.getRedirectUri());
    }

    @Transactional
    public String crearAuthorizationUrl(String storefrontHost, String returnPath) {
        this.validarConfiguracion();
        this.storefrontService.resolverPorHost(storefrontHost);
        String host = StorefrontService.normalizarHost(storefrontHost);
        String state = this.randomToken(32);
        String nonce = this.randomToken(24);
        String codeVerifier = this.randomToken(64);
        StorefrontGoogleOAuthIntento intento = new StorefrontGoogleOAuthIntento();
        intento.setStateHash(this.sha256(state));
        intento.setNonce(nonce);
        intento.setCodeVerifier(codeVerifier);
        intento.setStorefrontHost(host);
        intento.setReturnPath(this.normalizarReturnPath(returnPath));
        intento.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10L));
        this.intentoRepository.save(intento);
        return UriComponentsBuilder.fromUriString(this.properties.getAuthorizationUrl()).queryParam("client_id", this.properties.getClientId().trim()).queryParam("redirect_uri", this.properties.getRedirectUri().trim()).queryParam("response_type", "code").queryParam("scope", "openid email profile").queryParam("state", state).queryParam("nonce", nonce).queryParam("code_challenge", this.sha256Base64Url(codeVerifier)).queryParam("code_challenge_method", "S256").queryParam("prompt", "select_account").build().encode().toUriString();
    }

    @Transactional
    public String procesarCallback(String code, String state) {
        this.validarConfiguracion();
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            throw new BadRequestException("Google no devolvio los datos necesarios para completar el acceso");
        }
        StorefrontGoogleOAuthIntento intento = this.resolveIntentoPorState(state);
        GoogleTokenResponse tokens = this.intercambiarCodigo(code.trim(), intento.getCodeVerifier());
        Jwt idToken = this.validarIdToken(tokens.idToken(), intento.getNonce());
        String email = idToken.getClaimAsString("email");
        Boolean emailVerified = (Boolean)idToken.getClaim("email_verified");
        if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
            throw new OperacionEnConflictoException("Google no pudo verificar el correo de la cuenta");
        }
        String nombre = idToken.getClaimAsString("name");
        StorefrontAuthResponse session = this.storefrontAuthService.autenticarClienteGoogle(intento.getStorefrontHost(), email, nombre);
        Usuario usuario = this.usuarioRepository.findByEmailIgnoreCase(session.getEmail()).orElseThrow(() -> new IllegalStateException("La cuenta Google no pudo asociarse"));
        String ticket = this.randomToken(36);
        intento.setUsuarioId(usuario.getId());
        intento.setTicketHash(this.sha256(ticket));
        intento.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        intento.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5L));
        this.intentoRepository.save(intento);
        return this.construirRetornoStorefront(intento.getStorefrontHost(), ticket, false);
    }

    @Transactional
    public String construirRetornoError(String state) {
        if (!StringUtils.hasText(state)) {
            return "https://mercado-digital.com.ar";
        }
        return this.intentoRepository.findByStateHash(this.sha256(state)).map(intento -> this.construirRetornoStorefront(intento.getStorefrontHost(), null, true)).orElse("https://mercado-digital.com.ar");
    }

    @Transactional
    public StorefrontGoogleOAuthExchangeResponse canjear(StorefrontGoogleOAuthExchangeRequest request) {
        String host = StorefrontService.normalizarHost(request.getStorefrontHost());
        StorefrontGoogleOAuthIntento intento = this.intentoRepository.findByTicketHash(this.sha256(request.getTicket().trim())).orElseThrow(() -> new BadRequestException("El acceso con Google no es valido o ya vencio"));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (intento.getConsumedAt() != null || intento.getUsuarioId() == null || now.isAfter(intento.getExpiresAt())) {
            throw new BadRequestException("El acceso con Google no es valido o ya vencio");
        }
        if (!StorefrontService.hostsEquivalentes(host, intento.getStorefrontHost())) {
            throw new BadRequestException("El acceso con Google pertenece a otra tienda");
        }
        Usuario usuario = (Usuario)this.usuarioRepository.findById(intento.getUsuarioId()).orElseThrow(() -> new BadRequestException("La cuenta Google ya no esta disponible"));
        StorefrontAuthResponse session = this.storefrontAuthService.autenticarClienteGoogle(intento.getStorefrontHost(), usuario.getEmail(), usuario.getNombre());
        intento.setConsumedAt(now);
        this.intentoRepository.save(intento);
        return StorefrontGoogleOAuthExchangeResponse.builder().session(session).returnPath(intento.getReturnPath()).build();
    }

    private StorefrontGoogleOAuthIntento resolveIntentoPorState(String state) {
        StorefrontGoogleOAuthIntento intento = this.intentoRepository.findByStateHash(this.sha256(state)).orElseThrow(() -> new BadRequestException("La solicitud de acceso con Google no es valida"));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (intento.getCompletedAt() != null || now.isAfter(intento.getExpiresAt())) {
            throw new BadRequestException("La solicitud de acceso con Google ya vencio o fue utilizada");
        }
        return intento;
    }

    private GoogleTokenResponse intercambiarCodigo(String code, String codeVerifier) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        form.add("code", code);
        form.add("client_id", this.properties.getClientId().trim());
        form.add("client_secret", this.properties.getClientSecret().trim());
        form.add("redirect_uri", this.properties.getRedirectUri().trim());
        form.add("grant_type", "authorization_code");
        form.add("code_verifier", codeVerifier);
        try {
            GoogleTokenResponse response = ((RestClient.RequestBodySpec)this.restClient.post().uri(this.properties.getTokenUrl(), new Object[0])).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(GoogleTokenResponse.class);
            if (response == null || !StringUtils.hasText(response.idToken())) {
                throw new IntegracionPspException("Google no devolvio una identidad valida");
            }
            return response;
        }
        catch (RestClientException ex) {
            throw new IntegracionPspException("No se pudo validar el acceso con Google", ex);
        }
    }

    private Jwt validarIdToken(String rawToken, String nonceEsperado) {
        try {
            Jwt jwt = this.jwtDecoder.decode(rawToken);
            if (!nonceEsperado.equals(jwt.getClaimAsString("nonce"))) {
                throw new BadRequestException("La respuesta de Google no corresponde a esta solicitud");
            }
            return jwt;
        }
        catch (JwtException ex) {
            throw new BadRequestException("La identidad devuelta por Google no es valida");
        }
    }

    private OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> jwt.getIssuer() != null && List.of("https://accounts.google.com", "accounts.google.com").contains(jwt.getIssuer().toString()) ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emisor Google invalido", null));
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return jwt -> jwt.getAudience().contains(this.properties.getClientId().trim()) ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia Google invalida", null));
    }

    private String construirRetornoStorefront(String host, String ticket, boolean error) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://" + host + "/ingresar/google");
        if (error) {
            builder.queryParam("error", "google_login");
        } else {
            builder.queryParam("ticket", ticket);
        }
        return builder.build().encode().toUriString();
    }

    private void validarConfiguracion() {
        if (!this.estaHabilitado()) {
            throw new IntegracionPspException("El acceso con Google todavia no esta configurado");
        }
    }

    private String normalizarReturnPath(String returnPath) {
        if (!StringUtils.hasText(returnPath) || !returnPath.startsWith("/") || returnPath.startsWith("//")) {
            return "/mi-cuenta";
        }
        return returnPath.length() <= 300 ? returnPath : "/mi-cuenta";
    }

    private String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private String sha256Base64Url(String value) {
        try {
            return URL_ENCODER.encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)));
        }
        catch (Exception ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private record GoogleTokenResponse(@JsonProperty(value="id_token") String idToken, @JsonProperty(value="access_token") String accessToken, @JsonProperty(value="expires_in") Long expiresIn, @JsonProperty(value="token_type") String tokenType, String scope) {
    }
}

