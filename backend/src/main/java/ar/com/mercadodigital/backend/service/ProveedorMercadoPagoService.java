package ar.com.mercadodigital.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.dto.request.AdminRevendedorMercadoPagoRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorMercadoPagoAccount;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProveedorMercadoPagoAccountRepository;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorMercadoPagoService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMercadoPagoAccountRepository proveedorMercadoPagoAccountRepository;
    private final MercadoPagoProperties mercadoPagoProperties;

    @Transactional
    public AdminRevendedorMercadoPagoResponse upsert(UUID proveedorId, AdminRevendedorMercadoPagoRequest request) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId));

        ProveedorMercadoPagoAccount account = proveedorMercadoPagoAccountRepository.findByProveedor_Id(proveedorId)
                .orElseGet(() -> {
                    ProveedorMercadoPagoAccount nueva = new ProveedorMercadoPagoAccount();
                    nueva.setProveedor(proveedor);
                    return nueva;
                });

        boolean wasLinked = isLinked(account);
        account.setMercadoPagoUserId(normalizeOptional(request.getMercadoPagoUserId()));
        validarMercadoPagoUserIdDisponible(account.getMercadoPagoUserId(), proveedorId);
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

        ProveedorMercadoPagoAccount saved = proveedorMercadoPagoAccountRepository.save(account);
        proveedor.setMercadoPagoAccount(saved);
        return toResponse(saved);
    }

    @Transactional
    public AdminRevendedorMercadoPagoResponse linkFromOAuth(UUID proveedorId, MercadoPagoClient.MercadoPagoOAuthTokens tokens) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId));

        ProveedorMercadoPagoAccount account = proveedorMercadoPagoAccountRepository.findByProveedor_Id(proveedorId)
                .orElseGet(() -> {
                    ProveedorMercadoPagoAccount nueva = new ProveedorMercadoPagoAccount();
                    nueva.setProveedor(proveedor);
                    return nueva;
                });

        String mercadoPagoUserId = normalizeOptional(tokens.userId());
        validarMercadoPagoUserIdDisponible(mercadoPagoUserId, proveedorId);

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

        ProveedorMercadoPagoAccount saved = proveedorMercadoPagoAccountRepository.save(account);
        proveedor.setMercadoPagoAccount(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminRevendedorMercadoPagoResponse getEstado(UUID proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId));
        return toResponse(proveedor.getMercadoPagoAccount());
    }

    public AdminRevendedorMercadoPagoResponse toResponse(ProveedorMercadoPagoAccount account) {
        if (account == null) {
            boolean hasGlobalFallback = StringUtils.hasText(mercadoPagoProperties.getAccessToken());
            return AdminRevendedorMercadoPagoResponse.builder()
                    .estado(hasGlobalFallback ? "FALLBACK_GLOBAL" : "PENDIENTE")
                    .readyForCheckout(hasGlobalFallback)
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
                .readyForCheckout(ready || hasGlobalFallback)
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
                .payoutEmail(account.getProveedor() != null && account.getProveedor().getUsuario() != null
                        ? account.getProveedor().getUsuario().getEmail()
                        : null)
                .linkedAt(account.getLinkedAt())
                .lastRefreshAt(account.getLastRefreshAt())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private String resolveStatus(ProveedorMercadoPagoAccount account, boolean expired, boolean hasGlobalFallback) {
        if (expired) {
            return hasGlobalFallback ? "VENCIDA_CON_FALLBACK" : "VENCIDA";
        }
        if (isReadyForCheckout(account)) {
            return "VINCULADA";
        }
        return hasGlobalFallback ? "INCOMPLETA_CON_FALLBACK" : "PENDIENTE";
    }

    private boolean isLinked(ProveedorMercadoPagoAccount account) {
        return StringUtils.hasText(account.getMercadoPagoUserId()) && StringUtils.hasText(account.getAccessToken());
    }

    private boolean isExpired(ProveedorMercadoPagoAccount account) {
        return account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isBefore(LocalDateTime.now());
    }

    private boolean isReadyForCheckout(ProveedorMercadoPagoAccount account) {
        return isLinked(account) && !isExpired(account);
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validarMercadoPagoUserIdDisponible(String mercadoPagoUserId, UUID proveedorIdActual) {
        if (!StringUtils.hasText(mercadoPagoUserId)) {
            return;
        }
        proveedorMercadoPagoAccountRepository.findByMercadoPagoUserId(mercadoPagoUserId.trim())
                .filter(account -> !account.getProveedor().getId().equals(proveedorIdActual))
                .ifPresent(account -> {
                    throw new OperacionEnConflictoException(
                            "La cuenta Mercado Pago ya esta vinculada a otro proveedor.");
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
}
