package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tarifas_transportista")
public class TarifaTransportista {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transportista_id", nullable = false)
    private Transportista transportista;

    @Column(name = "categoria_paquete", nullable = false, length = 30)
    private String categoriaPaquete;

    @Column(nullable = false, length = 60)
    private String zona;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precio;

    @Column(name = "demora_estimada_dias", nullable = false)
    private Integer demoraEstimadaDias;
}
