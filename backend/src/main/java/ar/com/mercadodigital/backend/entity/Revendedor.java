package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "revendedores")
public class Revendedor extends BaseEntity {

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 180)
    private String dominio;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "markup_porcentaje_default", nullable = false, precision = 5, scale = 2)
    private BigDecimal markupPorcentajeDefault = BigDecimal.ZERO;

    @Column(name = "markup_fijo_default", nullable = false, precision = 19, scale = 2)
    private BigDecimal markupFijoDefault = BigDecimal.ZERO;

    @Column(name = "comision_asesor_porcentaje_default", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionAsesorPorcentajeDefault = BigDecimal.ZERO;

    @Column(name = "comision_asesor_fija_default", nullable = false, precision = 19, scale = 2)
    private BigDecimal comisionAsesorFijaDefault = BigDecimal.ZERO;

    @Column(name = "codigo_descuento", length = 60)
    private String codigoDescuento;

    @Column(name = "descuento_fijo", nullable = false, precision = 19, scale = 2)
    private BigDecimal descuentoFijo = BigDecimal.ZERO;

    @OneToMany(mappedBy = "revendedor")
    private List<RevendedorMarkup> markups = new ArrayList<>();

    @OneToMany(mappedBy = "revendedor")
    private List<AsesorVenta> asesores = new ArrayList<>();

    @OneToOne(mappedBy = "revendedor")
    private RevendedorStorefront storefront;

    @OneToOne(mappedBy = "revendedor")
    private RevendedorMercadoPagoAccount mercadoPagoAccount;

    @OneToMany(mappedBy = "revendedor")
    private List<ClienteRevendedor> clientes = new ArrayList<>();
}
