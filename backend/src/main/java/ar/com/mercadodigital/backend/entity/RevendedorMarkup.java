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
@Table(name = "revendedor_markups")
public class RevendedorMarkup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false)
    private Revendedor revendedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "markup_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal markupPorcentaje;

    @Column(name = "markup_fijo", nullable = false, precision = 19, scale = 2)
    private BigDecimal markupFijo;

    @Column(name = "comision_asesor_porcentaje", precision = 5, scale = 2)
    private BigDecimal comisionAsesorPorcentaje;

    @Column(name = "comision_asesor_fija", precision = 19, scale = 2)
    private BigDecimal comisionAsesorFija;

    @Column(nullable = false)
    private boolean activo = true;
}
