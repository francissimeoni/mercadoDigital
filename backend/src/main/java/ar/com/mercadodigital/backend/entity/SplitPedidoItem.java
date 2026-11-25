package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "splits_pedido_item")
public class SplitPedidoItem {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_item_id", nullable = false, unique = true)
    private PedidoItem pedidoItem;

    @Column(name = "monto_proveedor", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoProveedor;

    @Column(name = "monto_revendedor", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoRevendedor;

    @Column(name = "monto_md", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoMd;

    @Column(name = "monto_envio", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoEnvio;

    @Column(name = "monto_seguro", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoSeguro;

    @Column(name = "monto_psp", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoPsp;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_liquidacion_proveedor", nullable = false, length = 40)
    private LiquidacionProveedorEstado estadoLiquidacionProveedor = LiquidacionProveedorEstado.PENDIENTE;

    @Column(name = "liberado_proveedor_at")
    private LocalDateTime liberadoProveedorAt;

    @Column(name = "motivo_liberacion_manual", length = 255)
    private String motivoLiberacionManual;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
