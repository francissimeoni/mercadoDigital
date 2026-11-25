package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminRevendedorMercadoPagoOAuthLinkResponse {
    private String authorizationUrl;
    private String redirectUri;
    private LocalDateTime expiresAt;
}
