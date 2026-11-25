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
@Table(name = "producto_preguntas")
public class ProductoPregunta extends BaseEntity {

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pregunta;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_pregunta", nullable = false)
    private LocalDateTime fechaPregunta;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}
