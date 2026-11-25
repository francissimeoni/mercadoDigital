package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "revendedor_productos")
public class RevendedorProducto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false)
    private Revendedor revendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_producto_id")
    private CategoriaProducto categoriaProducto;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_venta", precision = 19, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false, length = 10)
    private String moneda = "ARS";

    @Column
    private Integer stock;

    @Column(name = "categoria_paquete", length = 30)
    private String categoriaPaquete;

    @jakarta.persistence.Version
    @Column(nullable = false)
    private long version;

    @Column(name = "catalogo_alta_token", length = 36)
    private String catalogoAltaToken;

    @Column(name = "catalogo_alta_hash", length = 64)
    private String catalogoAltaHash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "es_promocion", nullable = false)
    private boolean promocion;

    @Column(name = "es_novedad", nullable = false)
    private boolean novedad;

    @OneToMany(mappedBy = "revendedorProducto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RevendedorProductoMedia> media = new ArrayList<>();
}
