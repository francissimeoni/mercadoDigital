package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Preparación privada. Ningún consumidor público utiliza esta configuración en el paso 1. */
@Getter @Setter @Entity
@Table(name = "catalogo_configuraciones")
public class CatalogoConfiguracion extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false, unique = true)
    private Revendedor revendedor;
    @Column(name = "cotizacion_usd", precision = 19, scale = 6)
    private BigDecimal cotizacionUsd;
    @Column(name = "condiciones_entrega", length = 2000)
    private String condicionesEntrega;
    @Version
    private long version;
}
