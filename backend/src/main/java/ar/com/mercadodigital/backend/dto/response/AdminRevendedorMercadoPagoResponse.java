package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminRevendedorMercadoPagoResponse {
    private UUID id;
    private String estado;
    private boolean readyForCheckout;
    private boolean usaCredencialesGlobalesFallback;
    private String mercadoPagoUserId;
    private String publicKey;
    private String accessTokenMasked;
    private boolean accessTokenConfigured;
    private String refreshTokenMasked;
    private boolean refreshTokenConfigured;
    private LocalDateTime tokenExpiresAt;
    private boolean liveMode;
    private String scope;
    private String payoutEmail;
    private LocalDateTime linkedAt;
    private LocalDateTime lastRefreshAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
