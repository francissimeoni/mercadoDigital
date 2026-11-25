package ar.com.mercadodigital.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "revendedor_mercadopago_accounts")
public class RevendedorMercadoPagoAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false, unique = true)
    private Revendedor revendedor;

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

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;
}
