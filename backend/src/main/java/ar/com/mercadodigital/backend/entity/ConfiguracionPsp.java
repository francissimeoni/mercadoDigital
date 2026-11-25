package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "configuracion_psp")
public class ConfiguracionPsp extends BaseEntity {

    @Column(name = "clave", nullable = false, unique = true, length = 50)
    private String clave;

    @Column(name = "psp_fee_porcentaje", nullable = false, precision = 8, scale = 4)
    private BigDecimal pspFeePorcentaje = BigDecimal.valueOf(8);

    @Column(name = "seguro_transporte_porcentaje", nullable = false, precision = 8, scale = 4)
    private BigDecimal seguroTransportePorcentaje = BigDecimal.valueOf(2);
}
