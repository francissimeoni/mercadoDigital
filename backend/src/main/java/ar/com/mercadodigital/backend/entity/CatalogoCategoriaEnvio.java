package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "catalogo_categorias_envio", uniqueConstraints = @UniqueConstraint(
        name = "uk_catalogo_envio_nombre", columnNames = {"revendedor_id", "nombre"}))
public class CatalogoCategoriaEnvio extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false)
    private Revendedor revendedor;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(name = "costo_ars", nullable = false, precision = 19, scale = 2)
    private BigDecimal costoArs;
    @Version
    private long version;
}
