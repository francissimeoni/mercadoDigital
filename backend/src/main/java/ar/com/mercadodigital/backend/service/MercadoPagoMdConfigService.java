package ar.com.mercadodigital.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.dto.request.AdminRevendedorMercadoPagoRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.entity.ConfiguracionMercadoPagoMd;
import ar.com.mercadodigital.backend.repository.ConfiguracionMercadoPagoMdRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MercadoPagoMdConfigService {

    private static final String CLAVE_GLOBAL = "GLOBAL";

    private final ConfiguracionMercadoPagoMdRepository configuracionMercadoPagoMdRepository;
    private final MercadoPagoProperties mercadoPagoProperties;

    @Transactional(readOnly = true)
    public AdminRevendedorMercadoPagoResponse getEstado() {
        return toResponse(ensureConfig());
    }

    @Transactional
    public AdminRevendedorMercadoPagoResponse upsert(AdminRevendedorMercadoPagoRequest request) {
        ConfiguracionMercadoPagoMd config = ensureConfig();
        boolean wasLinked = isLinked(config);
        config.setMercadoPagoUserId(normalizeOptional(request.getMercadoPagoUserId()));
        config.setPublicKey(normalizeOptional(request.getPublicKey()));
        if (StringUtils.hasText(request.getAccessToken())) {
            config.setAccessToken(normalizeOptional(request.getAccessToken()));
        }
        if (StringUtils.hasText(request.getRefreshToken())) {
            config.setRefreshToken(normalizeOptional(request.getRefreshToken()));
        }
        if (request.getTokenExpiresAt() != null) {
            config.setTokenExpiresAt(request.getTokenExpiresAt());
        }
        config.setLiveMode(Boolean.TRUE.equals(request.getLiveMode()));
        config.setScope(normalizeOptional(request.getScope()));
        config.setPayoutEmail(normalizeOptional(request.getPayoutEmail()));

        if (!wasLinked && isLinked(config)) {
            config.setLinkedAt(LocalDateTime.now());
        } else if (!isLinked(config)) {
            config.setLinkedAt(null);
        }

        return toResponse(configuracionMercadoPagoMdRepository.save(config));
    }

    @Transactional
    public AdminRevendedorMercadoPagoResponse linkFromOAuth(MercadoPagoClient.MercadoPagoOAuthTokens tokens) {
        ConfiguracionMercadoPagoMd config = ensureConfig();
        boolean wasLinked = isLinked(config);
        config.setMercadoPagoUserId(normalizeOptional(tokens.userId()));
        config.setPublicKey(normalizeOptional(tokens.publicKey()));
        config.setAccessToken(normalizeOptional(tokens.accessToken()));
        config.setRefreshToken(normalizeOptional(tokens.refreshToken()));
        config.setTokenExpiresAt(tokens.expiresAt());
        config.setLiveMode(tokens.liveMode());
        config.setScope(normalizeOptional(tokens.scope()));
        if (!wasLinked && isLinked(config)) {
            config.setLinkedAt(LocalDateTime.now());
        }

        return toResponse(configuracionMercadoPagoMdRepository.save(config));
    }

    @Transactional(readOnly = true)
    public MercadoPagoMdPayoutConfig resolveForPayout() {
        ConfiguracionMercadoPagoMd config = ensureConfig();
        String payoutEmail = normalizeOptional(config.getPayoutEmail());
        return new MercadoPagoMdPayoutConfig(
                normalizeOptional(config.getAccessToken()),
                payoutEmail,
                isReady(config) && StringUtils.hasText(payoutEmail));
    }

    private ConfiguracionMercadoPagoMd ensureConfig() {
        return configuracionMercadoPagoMdRepository.findByClave(CLAVE_GLOBAL)
                .orElseGet(() -> {
                    ConfiguracionMercadoPagoMd nueva = new ConfiguracionMercadoPagoMd();
                    nueva.setClave(CLAVE_GLOBAL);
                    nueva.setMercadoPagoUserId(null);
                    nueva.setPublicKey(normalizeOptional(mercadoPagoProperties.getPublicKey()));
                    nueva.setAccessToken(normalizeOptional(mercadoPagoProperties.getAccessToken()));
                    nueva.setRefreshToken(null);
                    nueva.setTokenExpiresAt(null);
                    nueva.setLiveMode(false);
                    nueva.setScope(null);
                    nueva.setPayoutEmail(null);
                    return configuracionMercadoPagoMdRepository.save(nueva);
                });
    }

    private AdminRevendedorMercadoPagoResponse toResponse(ConfiguracionMercadoPagoMd config) {
        boolean hasToken = StringUtils.hasText(config.getAccessToken());
        boolean expired = isExpired(config);
        boolean ready = hasToken && !expired;
        return AdminRevendedorMercadoPagoResponse.builder()
                .id(config.getId())
                .estado(resolveStatus(ready, expired, hasToken))
                .readyForCheckout(ready)
                .usaCredencialesGlobalesFallback(false)
                .mercadoPagoUserId(config.getMercadoPagoUserId())
                .publicKey(config.getPublicKey())
                .accessTokenMasked(maskSecret(config.getAccessToken()))
                .accessTokenConfigured(hasToken)
                .refreshTokenMasked(maskSecret(config.getRefreshToken()))
                .refreshTokenConfigured(StringUtils.hasText(config.getRefreshToken()))
                .tokenExpiresAt(config.getTokenExpiresAt())
                .liveMode(config.isLiveMode())
                .scope(config.getScope())
                .payoutEmail(config.getPayoutEmail())
                .linkedAt(config.getLinkedAt())
                .lastRefreshAt(config.getLastRefreshAt())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String resolveStatus(boolean ready, boolean expired, boolean hasToken) {
        if (expired) {
            return "VENCIDA";
        }
        if (ready) {
            return "VINCULADA";
        }
        return hasToken ? "INCOMPLETA" : "PENDIENTE";
    }

    private boolean isLinked(ConfiguracionMercadoPagoMd config) {
        return StringUtils.hasText(config.getMercadoPagoUserId()) && StringUtils.hasText(config.getAccessToken());
    }

    private boolean isExpired(ConfiguracionMercadoPagoMd config) {
        return config.getTokenExpiresAt() != null && config.getTokenExpiresAt().isBefore(LocalDateTime.now());
    }

    private boolean isReady(ConfiguracionMercadoPagoMd config) {
        return isLinked(config) && !isExpired(config);
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String maskSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "********";
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    public record MercadoPagoMdPayoutConfig(String accessToken, String payoutEmail, boolean readyForPayout) {
    }
}
