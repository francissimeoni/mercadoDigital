package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "configuracion_costo_md_categoria")
public class ConfiguracionCostoMdCategoria extends BaseEntity {

    @Column(name = "categoria_paquete", nullable = false, unique = true, length = 30)
    private String categoriaPaquete;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_calculo", nullable = false, length = 20)
    private TipoCalculoCostoMd tipoCalculo = TipoCalculoCostoMd.MONTO_FIJO;
}
