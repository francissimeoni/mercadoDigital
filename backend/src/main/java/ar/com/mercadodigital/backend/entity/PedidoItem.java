package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pedido_items")
public class PedidoItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revendedor_producto_id")
    private RevendedorProducto revendedorProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_costo", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioCosto;

    @Column(name = "markup_aplicado", nullable = false, precision = 19, scale = 2)
    private BigDecimal markupAplicado;

    @Column(name = "precio_item_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioItemFinal;

    @Column(name = "tiene_seguro", nullable = false)
    private boolean tieneSeguro = false;

    @Column(name = "monto_seguro", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoSeguro = BigDecimal.ZERO;
}
