package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rmas")
public class RMA extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_item_id", nullable = false)
    private PedidoItem pedidoItem;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_rma", nullable = false, length = 40)
    private RmaEstado estadoRma = RmaEstado.SOLICITADO;

    @Column(name = "tiene_seguro", nullable = false)
    private boolean tieneSeguro = false;

    @Column(name = "responsable_probable", length = 60)
    private String responsableProbable;

    @Column(name = "observaciones_inspeccion", columnDefinition = "text")
    private String observacionesInspeccion;

    @Column(name = "resolucion_final", length = 40)
    private String resolucionFinal;

    @Column(name = "monto_compensacion", precision = 18, scale = 2)
    private BigDecimal montoCompensacion;

    @Column(name = "referencia_compensacion", length = 160)
    private String referenciaCompensacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por_usuario_id", nullable = false)
    private Usuario creadoPor;
}
