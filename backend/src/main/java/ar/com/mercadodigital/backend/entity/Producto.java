package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
@Table(name = "productos")
public class Producto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_canonico_id")
    private ProductoCanonico productoCanonico;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_costo", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioCosto;

    @Column(nullable = false, length = 10)
    private String moneda = "ARS";

    @Column(name = "categoria_paquete", nullable = false, length = 30)
    private String categoriaPaquete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_producto_id")
    private CategoriaProducto categoriaProducto;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "producto")
    private List<RevendedorMarkup> markups = new ArrayList<>();

    @OneToMany(mappedBy = "producto")
    private List<ProveedorStock> stocks = new ArrayList<>();
}
