package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.PspWebhook;

public interface PspWebhookRepository extends JpaRepository<PspWebhook, UUID> {
    Optional<PspWebhook> findByIdEventoPsp(String idEventoPsp);
}
