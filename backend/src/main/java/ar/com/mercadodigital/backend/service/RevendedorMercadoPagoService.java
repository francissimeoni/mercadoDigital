package ar.com.mercadodigital.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.dto.request.AdminRevendedorMercadoPagoRequest;
import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorMercadoPagoAccount;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.RevendedorMercadoPagoAccountRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevendedorMercadoPagoService {

    private final RevendedorRepository revendedorRepository;
    private final RevendedorMercadoPagoAccountRepository revendedorMercadoPagoAccountRepository;
    private final MercadoPagoProperties mercadoPagoProperties;

    @Transactional(readOnly = true)
    public AdminRevendedorMercadoPagoResponse get(UUID revendedorId) {
        if (!revendedorRepository.existsById(revendedorId)) {
            throw new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId);
        }
        return toResponse(revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId).orElse(null));
    }

    @Transactional
    public AdminRevendedorMercadoPagoResponse desvincular(UUID revendedorId) {
        if (!revendedorRepository.existsById(revendedorId)) {
            throw new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId);
        }
        revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId)
                .ifPresent(revendedorMercadoPagoAccountRepository::delete);
        return toResponse(null);
    }

    @Transactional
    public AdminRevendedorMercadoPagoResponse upsert(UUID revendedorId, AdminRevendedorMercadoPagoRequest request) {
        Revendedor revendedor = revendedorRepository.findById(revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId));

        RevendedorMercadoPagoAccount account = revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId)
                .orElseGet(() -> {
                    RevendedorMercadoPagoAccount nueva = new RevendedorMercadoPagoAccount();
                    nueva.setRevendedor(revendedor);
                    return nueva;
                });

        boolean wasLinked = isLinked(account);
        account.setMercadoPagoUserId(normalizeOptional(request.getMercadoPagoUserId()));
        validarMercadoPagoUserIdDisponible(account.getMercadoPagoUserId(), revendedorId);
        account.setPublicKey(normalizeOptional(request.getPublicKey()));
        account.setAccessToken(normalizeOptional(request.getAccessToken()));
        account.setRefreshToken(normalizeOptional(request.getRefreshToken()));
        account.setTokenExpiresAt(request.getTokenExpiresAt());
        account.setLiveMode(Boolean.TRUE.equals(request.getLiveMode()));
        account.setScope(normalizeOptional(request.getScope()));

        if (!wasLinked && isLinked(account)) {
            account.setLinkedAt(LocalDateTime.now());
        } else if (!isLinked(account)) {
            account.setLinkedAt(null);
        }

        RevendedorMercadoPagoAccount saved = revendedorMercadoPagoAccountRepository.save(account);
        revendedor.setMercadoPagoAccount(saved);
        return toResponse(saved);
    }

    @Transactional
    public AdminRevendedorMercadoPagoResponse linkFromOAuth(UUID revendedorId, MercadoPagoClient.MercadoPagoOAuthTokens tokens) {
        Revendedor revendedor = revendedorRepository.findById(revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId));

        RevendedorMercadoPagoAccount account = revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId)
                .orElseGet(() -> {
                    RevendedorMercadoPagoAccount nueva = new RevendedorMercadoPagoAccount();
                    nueva.setRevendedor(revendedor);
                    return nueva;
                });

        String mercadoPagoUserId = normalizeOptional(tokens.userId());
        validarMercadoPagoUserIdDisponible(mercadoPagoUserId, revendedorId);

        boolean wasLinked = isLinked(account);
        account.setMercadoPagoUserId(mercadoPagoUserId);
        account.setPublicKey(normalizeOptional(tokens.publicKey()));
        account.setAccessToken(normalizeOptional(tokens.accessToken()));
        account.setRefreshToken(normalizeOptional(tokens.refreshToken()));
        account.setTokenExpiresAt(tokens.expiresAt());
        account.setLiveMode(tokens.liveMode());
        account.setScope(normalizeOptional(tokens.scope()));
        if (!wasLinked && isLinked(account)) {
            account.setLinkedAt(LocalDateTime.now());
        }

        RevendedorMercadoPagoAccount saved = revendedorMercadoPagoAccountRepository.save(account);
        revendedor.setMercadoPagoAccount(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RevendedorMercadoPagoResolution resolveForRevendedor(UUID revendedorId) {
        if (!mercadoPagoProperties.isEnabled()) return new RevendedorMercadoPagoResolution(null, null, false, false);
        RevendedorMercadoPagoAccount account = revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId).orElse(null);
        if (account != null && isReadyForCheckout(account)) {
            return new RevendedorMercadoPagoResolution(account.getAccessToken(), account.getMercadoPagoUserId(), false, true);
        }
        return new RevendedorMercadoPagoResolution(
                normalizeOptional(mercadoPagoProperties.getAccessToken()),
                null,
                true,
                StringUtils.hasText(mercadoPagoProperties.getAccessToken()));
    }

    @Transactional(readOnly = true)
    public RevendedorMercadoPagoResolution resolveForMercadoPagoUserId(String mercadoPagoUserId) {
        if (!mercadoPagoProperties.isEnabled()) return new RevendedorMercadoPagoResolution(null, null, false, false);
        RevendedorMercadoPagoAccount account = StringUtils.hasText(mercadoPagoUserId)
                ? revendedorMercadoPagoAccountRepository.findByMercadoPagoUserId(mercadoPagoUserId.trim()).orElse(null)
                : null;
        if (account != null && isReadyForCheckout(account)) {
            return new RevendedorMercadoPagoResolution(account.getAccessToken(), account.getMercadoPagoUserId(), false, true);
        }
        return new RevendedorMercadoPagoResolution(
                normalizeOptional(mercadoPagoProperties.getAccessToken()),
                null,
                true,
                StringUtils.hasText(mercadoPagoProperties.getAccessToken()));
    }

    public AdminRevendedorMercadoPagoResponse toResponse(RevendedorMercadoPagoAccount account) {
        if (account == null) {
            boolean hasGlobalFallback = StringUtils.hasText(mercadoPagoProperties.getAccessToken());
            return AdminRevendedorMercadoPagoResponse.builder()
                    .estado(hasGlobalFallback ? "FALLBACK_GLOBAL" : "PENDIENTE")
                    .readyForCheckout(mercadoPagoProperties.isEnabled() && hasGlobalFallback)
                    .usaCredencialesGlobalesFallback(hasGlobalFallback)
                    .accessTokenConfigured(false)
                    .refreshTokenConfigured(false)
                    .build();
        }

        boolean expired = isExpired(account);
        boolean ready = isReadyForCheckout(account);
        boolean hasGlobalFallback = !ready && StringUtils.hasText(mercadoPagoProperties.getAccessToken());

        return AdminRevendedorMercadoPagoResponse.builder()
                .id(account.getId())
                .estado(resolveStatus(account, expired, hasGlobalFallback))
                .readyForCheckout(mercadoPagoProperties.isEnabled() && (ready || hasGlobalFallback))
                .usaCredencialesGlobalesFallback(hasGlobalFallback)
                .mercadoPagoUserId(account.getMercadoPagoUserId())
                .publicKey(account.getPublicKey())
                .accessTokenMasked(maskSecret(account.getAccessToken()))
                .accessTokenConfigured(StringUtils.hasText(account.getAccessToken()))
                .refreshTokenMasked(maskSecret(account.getRefreshToken()))
                .refreshTokenConfigured(StringUtils.hasText(account.getRefreshToken()))
                .tokenExpiresAt(account.getTokenExpiresAt())
                .liveMode(account.isLiveMode())
                .scope(account.getScope())
                .payoutEmail(account.getRevendedor() != null && account.getRevendedor().getUsuario() != null
                        ? account.getRevendedor().getUsuario().getEmail()
                        : null)
                .linkedAt(account.getLinkedAt())
                .lastRefreshAt(account.getLastRefreshAt())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private String resolveStatus(RevendedorMercadoPagoAccount account, boolean expired, boolean hasGlobalFallback) {
        if (expired) {
            return hasGlobalFallback ? "VENCIDA_CON_FALLBACK" : "VENCIDA";
        }
        if (isReadyForCheckout(account)) {
            return "VINCULADA";
        }
        return hasGlobalFallback ? "INCOMPLETA_CON_FALLBACK" : "PENDIENTE";
    }

    private boolean isLinked(RevendedorMercadoPagoAccount account) {
        return StringUtils.hasText(account.getMercadoPagoUserId()) && StringUtils.hasText(account.getAccessToken());
    }

    private boolean isExpired(RevendedorMercadoPagoAccount account) {
        return account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isBefore(LocalDateTime.now());
    }

    private boolean isReadyForCheckout(RevendedorMercadoPagoAccount account) {
        return isLinked(account) && !isExpired(account);
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validarMercadoPagoUserIdDisponible(String mercadoPagoUserId, UUID revendedorIdActual) {
        if (!StringUtils.hasText(mercadoPagoUserId)) {
            return;
        }
        revendedorMercadoPagoAccountRepository.findByMercadoPagoUserId(mercadoPagoUserId.trim())
                .filter(account -> !account.getRevendedor().getId().equals(revendedorIdActual))
                .ifPresent(account -> {
                    throw new OperacionEnConflictoException(
                            "La cuenta Mercado Pago ya esta vinculada a otro revendedor.");
                });
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

    public record RevendedorMercadoPagoResolution(String accessToken, String mercadoPagoUserId,
            boolean usaCredencialesGlobalesFallback, boolean readyForCheckout) {
    }
}
