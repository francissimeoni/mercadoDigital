package ar.com.mercadodigital.backend.dto.request;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRevendedorMercadoPagoRequest {
    private String mercadoPagoUserId;
    private String publicKey;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private Boolean liveMode;
    private String scope;
    private String payoutEmail;
}
