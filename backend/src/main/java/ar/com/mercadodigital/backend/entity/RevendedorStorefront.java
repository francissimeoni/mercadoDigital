package ar.com.mercadodigital.backend.entity;

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
@Table(name = "revendedor_storefront")
public class RevendedorStorefront extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_storefront", nullable = false, length = 30)
    private TipoStorefront tipoStorefront = TipoStorefront.REVENDEDOR;

    @Column(name = "dominio_publico", length = 180, unique = true)
    private String dominioPublico;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "revendedor_id", unique = true)
    private Revendedor revendedor;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "proveedor_id", unique = true)
    private Proveedor proveedor;

    @Column(name = "nombre_comercial", nullable = false, length = 180)
    private String nombreComercial;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "color_primario", length = 30)
    private String colorPrimario;

    @Column(name = "color_secundario", length = 30)
    private String colorSecundario;

    @Column(name = "hero_titulo", length = 180)
    private String heroTitulo;

    @Column(name = "hero_subtitulo", length = 255)
    private String heroSubtitulo;

    @Column(name = "hero_imagen_url", length = 500)
    private String heroImagenUrl;

    @Column(name = "hero_portadas_json", columnDefinition = "TEXT")
    private String heroPortadasJson;

    @Column(name = "email_comercial", length = 180)
    private String emailComercial;

    @Column(name = "telefono_comercial", length = 80)
    private String telefonoComercial;

    @Column(name = "direccion_comercial", length = 255)
    private String direccionComercial;

    @Column(name = "footer_texto", length = 500)
    private String footerTexto;

    @Column(name = "quienes_titulo", length = 180)
    private String quienesTitulo;

    @Column(name = "quienes_texto", columnDefinition = "TEXT")
    private String quienesTexto;

    @Column(name = "quienes_galeria_json", columnDefinition = "TEXT")
    private String quienesGaleriaJson;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "tiktok_url", length = 500)
    private String tiktokUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "transferencia_habilitada", nullable = false)
    private boolean transferenciaHabilitada = false;

    @Column(name = "transferencia_alias", length = 180)
    private String transferenciaAlias;

    @Column(name = "transferencia_titular", length = 180)
    private String transferenciaTitular;

    @Column(name = "smtp_host", length = 180)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 180)
    private String smtpUsername;

    @Column(name = "smtp_password", length = 255)
    private String smtpPassword;

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth = true;

    @Column(name = "smtp_starttls", nullable = false)
    private boolean smtpStartTls = true;

    @Column(nullable = false)
    private boolean publicada = false;

    @Column(name = "sincronizacion_pausada", nullable = false)
    private boolean sincronizacionPausada = false;

    @Column(name = "sincronizacion_pausada_por", length = 180)
    private String sincronizacionPausadaPor;

    @Column(name = "sincronizacion_pausada_at")
    private LocalDateTime sincronizacionPausadaAt;
}
