package ar.com.mercadodigital.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEvento extends BaseEntity {

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "referencia_id", length = 36)
    private UUID referenciaId;

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "fecha_evento", nullable = false, updatable = false)
    private LocalDateTime fechaEvento;
}
