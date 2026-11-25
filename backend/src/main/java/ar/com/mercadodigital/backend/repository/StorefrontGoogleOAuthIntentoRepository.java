/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.repository;

import ar.com.mercadodigital.backend.entity.StorefrontGoogleOAuthIntento;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface StorefrontGoogleOAuthIntentoRepository
extends JpaRepository<StorefrontGoogleOAuthIntento, UUID> {
    @Lock(value=LockModeType.PESSIMISTIC_WRITE)
    public Optional<StorefrontGoogleOAuthIntento> findByStateHash(String var1);

    @Lock(value=LockModeType.PESSIMISTIC_WRITE)
    public Optional<StorefrontGoogleOAuthIntento> findByTicketHash(String var1);
}

