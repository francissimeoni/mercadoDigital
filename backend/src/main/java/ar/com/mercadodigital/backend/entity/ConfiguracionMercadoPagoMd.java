package ar.com.mercadodigital.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "configuracion_mercadopago_md")
public class ConfiguracionMercadoPagoMd extends BaseEntity {

    @Column(name = "clave", nullable = false, unique = true, length = 50)
    private String clave;

    @Column(name = "mercado_pago_user_id", length = 80, unique = true)
    private String mercadoPagoUserId;

    @Column(name = "public_key", length = 150)
    private String publicKey;

    @Column(name = "access_token", length = 255)
    private String accessToken;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "live_mode", nullable = false)
    private boolean liveMode = false;

    @Column(length = 120)
    private String scope;

    @Column(name = "payout_email", length = 180)
    private String payoutEmail;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;
}
