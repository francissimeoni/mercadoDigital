package ar.com.mercadodigital.backend.entity;

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
@Table(name = "categorias_producto")
public class CategoriaProducto extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "slug", nullable = false, unique = true, length = 140)
    private String slug;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "sugerida", nullable = false)
    private boolean sugerida = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sugerida_por_proveedor_id")
    private Proveedor sugeridaPorProveedor;
}
