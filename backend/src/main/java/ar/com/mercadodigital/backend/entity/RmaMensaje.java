package ar.com.mercadodigital.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rma_mensajes")
public class RmaMensaje extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rma_id", nullable = false)
    private RMA rma;

    @Enumerated(EnumType.STRING)
    @Column(name = "autor_tipo", nullable = false, length = 40)
    private RmaAutorTipo autorTipo = RmaAutorTipo.SISTEMA;

    @Column(name = "autor_nombre", length = 180)
    private String autorNombre;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(nullable = false)
    private boolean interno = false;
}
