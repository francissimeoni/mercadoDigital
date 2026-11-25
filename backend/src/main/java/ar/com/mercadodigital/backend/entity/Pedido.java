package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pedidos")
public class Pedido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false)
    private Revendedor revendedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false, length = 40)
    private PedidoEstado estadoPedido = PedidoEstado.CREADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_pedido", nullable = false, length = 40)
    private OrigenPedido origenPedido = OrigenPedido.OPERATIVO;

    @Column(name = "tracking_token", unique = true, length = 36)
    private String trackingToken;

    @Column(name = "total_bruto", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBruto = BigDecimal.ZERO;

    @Column(name = "total_psp", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPsp = BigDecimal.ZERO;

    @Column(name = "total_envio", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalEnvio = BigDecimal.ZERO;

    @Column(name = "total_seguro", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSeguro = BigDecimal.ZERO;

    @Column(name = "total_descuento", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDescuento = BigDecimal.ZERO;

    @Column(name = "codigo_descuento_aplicado", length = 60)
    private String codigoDescuentoAplicado;

    @Column(name = "total_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFinal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private Pago pago;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private Envio envio;
}
