package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "catalogo_productos_configuracion")
public class CatalogoProductoConfiguracion extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private RevendedorProducto producto;
    @Column(nullable = false, length = 3)
    private String moneda;
    @Column(name = "precio_original", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioOriginal;
    @Column(name = "incluye_envio", nullable = false)
    private boolean incluyeEnvio;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_envio_id")
    private CatalogoCategoriaEnvio categoriaEnvio;
    @Version
    private long version;
}
