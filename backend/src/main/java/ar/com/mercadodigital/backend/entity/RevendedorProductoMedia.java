package ar.com.mercadodigital.backend.entity;

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
@Table(name = "revendedor_producto_media")
public class RevendedorProductoMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_producto_id", nullable = false)
    private RevendedorProducto revendedorProducto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_media", nullable = false, length = 20)
    private TipoMedia tipoMedia;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;
}
