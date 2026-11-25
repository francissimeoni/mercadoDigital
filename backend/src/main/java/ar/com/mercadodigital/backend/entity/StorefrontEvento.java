package ar.com.mercadodigital.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "storefront_eventos")
public class StorefrontEvento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false)
    private Revendedor revendedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private StorefrontEventoTipo tipoEvento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "visitante_hash", nullable = false, length = 64)
    private String visitanteHash;

    @Column(name = "producto_id", length = 36)
    @JdbcTypeCode(SqlTypes.CHAR)
    private java.util.UUID productoId;

    @Column(name = "producto_nombre", length = 180)
    private String productoNombre;

    @Column(name = "rubro_nombre", length = 180)
    private String rubroNombre;
}
