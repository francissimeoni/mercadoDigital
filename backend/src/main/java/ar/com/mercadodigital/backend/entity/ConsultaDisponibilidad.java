package ar.com.mercadodigital.backend.entity;

import java.time.LocalDateTime;

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
@Table(name = "consultas_disponibilidad")
public class ConsultaDisponibilidad extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revendedor_id")
    private Revendedor revendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revendedor_producto_id")
    private RevendedorProducto revendedorProducto;

    @Column(name = "nombre_contacto", length = 120)
    private String nombreContacto;

    @Column(name = "email_contacto", length = 180)
    private String emailContacto;

    @Column(name = "telefono_contacto", length = 80)
    private String telefonoContacto;

    @Column(nullable = false, length = 160)
    private String localidad;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "respuesta_proveedor", columnDefinition = "TEXT")
    private String respuestaProveedor;

    @Column(name = "checkout_token", length = 80, unique = true)
    private String checkoutToken;

    @Column(name = "fecha_consulta", nullable = false)
    private LocalDateTime fechaConsulta;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "accion_token", length = 80, unique = true)
    private String accionToken;

    @Column(name = "accion_token_expira_at")
    private LocalDateTime accionTokenExpiraAt;
}
