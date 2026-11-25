package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.RevendedorMercadoPagoAccount;

public interface RevendedorMercadoPagoAccountRepository extends JpaRepository<RevendedorMercadoPagoAccount, UUID> {
    Optional<RevendedorMercadoPagoAccount> findByRevendedor_Id(UUID revendedorId);

    Optional<RevendedorMercadoPagoAccount> findByMercadoPagoUserId(String mercadoPagoUserId);
}
