package ar.com.mercadodigital.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "split_payout_ejecuciones")
public class SplitPayoutEjecucion extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pedido_id", nullable = false, unique = true, length = 36)
    private UUID pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private SplitPayoutEstado estado = SplitPayoutEstado.PENDIENTE;

    @Column(name = "intentos", nullable = false)
    private int intentos = 0;

    @Column(name = "payout_id", length = 80)
    private String payoutId;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @Column(name = "error_ultimo", length = 500)
    private String errorUltimo;

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;

    @Column(name = "ultimo_intento_at")
    private LocalDateTime ultimoIntentoAt;

    @Column(name = "completado_at")
    private LocalDateTime completadoAt;
}
