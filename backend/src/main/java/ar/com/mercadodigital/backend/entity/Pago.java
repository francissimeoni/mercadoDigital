package ar.com.mercadodigital.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pagos")
public class Pago extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 40)
    private PagoEstado estadoPago = PagoEstado.PENDIENTE;

    @Column(nullable = false, length = 60)
    private String metodo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "psp_referencia", length = 150)
    private String pspReferencia;

    @Column(name = "psp_checkout_url", length = 500)
    private String pspCheckoutUrl;

    @Column(name = "transferencia_token", unique = true, length = 80)
    private String transferenciaToken;

    @Column(name = "transferencia_alias_snapshot", length = 180)
    private String transferenciaAliasSnapshot;

    @Column(name = "transferencia_solicitada_at")
    private LocalDateTime transferenciaSolicitadaAt;

    @Column(name = "comprobante_archivo", length = 255)
    private String comprobanteArchivo;

    @Column(name = "comprobante_nombre_original", length = 255)
    private String comprobanteNombreOriginal;

    @Column(name = "comprobante_content_type", length = 120)
    private String comprobanteContentType;

    @Column(name = "comprobante_cargado_at")
    private LocalDateTime comprobanteCargadoAt;
}
