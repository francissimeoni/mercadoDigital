package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "envios")
public class Envio extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportista_id")
    private Transportista transportista;

    @Column(name = "categoria_paquete", length = 30)
    private String categoriaPaquete;

    @Column(length = 120)
    private String tracking;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 40)
    private EnvioEstado estadoEnvio = EnvioEstado.PENDIENTE_PREPARACION;

    @Column(name = "direccion_envio", nullable = false)
    private String direccionEnvio;

    @Column(name = "costo_envio", nullable = false)
    private BigDecimal costoEnvio = BigDecimal.ZERO;
}
