package ar.com.mercadodigital.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rma_adjuntos")
public class RmaAdjunto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rma_id", nullable = false)
    private RMA rma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rma_mensaje_id", nullable = false)
    private RmaMensaje rmaMensaje;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Lob
    @Column(name = "archivo", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] archivo;
}
