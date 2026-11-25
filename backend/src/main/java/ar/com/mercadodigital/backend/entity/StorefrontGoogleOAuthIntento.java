/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.entity;

import ar.com.mercadodigital.backend.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name="storefront_google_oauth_intentos")
@lombok.Getter
@lombok.Setter
public class StorefrontGoogleOAuthIntento
extends BaseEntity {
    @Column(name="state_hash", nullable=false, unique=true, length=64, columnDefinition="char(64)")
    @JdbcTypeCode(value=1)
    private String stateHash;
    @Column(name="ticket_hash", unique=true, length=64, columnDefinition="char(64)")
    @JdbcTypeCode(value=1)
    private String ticketHash;
    @Column(nullable=false, length=100)
    private String nonce;
    @Column(name="code_verifier", nullable=false, length=128)
    private String codeVerifier;
    @Column(name="storefront_host", nullable=false, length=255)
    private String storefrontHost;
    @Column(name="return_path", nullable=false, length=300)
    private String returnPath;
    @Column(name="usuario_id", columnDefinition="char(36)")
    @JdbcTypeCode(value=1)
    private UUID usuarioId;
    @Column(name="expires_at", nullable=false)
    private LocalDateTime expiresAt;
    @Column(name="completed_at")
    private LocalDateTime completedAt;
    @Column(name="consumed_at")
    private LocalDateTime consumedAt;
}

