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
@Table(name = "asesores_venta")
public class AsesorVenta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revendedor_id", nullable = false)
    private Revendedor revendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(length = 80)
    private String provincia;

    @Column(length = 120)
    private String localidad;

    @Column(name = "zona_atencion", length = 200)
    private String zonaAtencion;

    @Column(name = "whatsapp_comercial", length = 16)
    private String whatsappComercial;

    @Column(nullable = false)
    private boolean activo = true;
}
