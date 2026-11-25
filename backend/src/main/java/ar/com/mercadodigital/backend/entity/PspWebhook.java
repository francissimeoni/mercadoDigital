package ar.com.mercadodigital.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "psp_webhooks")
public class PspWebhook {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "id_evento_psp", nullable = false, unique = true, length = 120)
    private String idEventoPsp;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;

    @Column(name = "recibido_en", nullable = false)
    private LocalDateTime recibidoEn;

    @Column(name = "procesado_ok", nullable = false)
    private boolean procesadoOk = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
