package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.ProveedorMercadoPagoAccount;

public interface ProveedorMercadoPagoAccountRepository extends JpaRepository<ProveedorMercadoPagoAccount, UUID> {
    Optional<ProveedorMercadoPagoAccount> findByProveedor_Id(UUID proveedorId);

    Optional<ProveedorMercadoPagoAccount> findByMercadoPagoUserId(String mercadoPagoUserId);
}
