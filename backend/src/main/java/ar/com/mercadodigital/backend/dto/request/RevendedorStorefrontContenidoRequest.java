package ar.com.mercadodigital.backend.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RevendedorStorefrontContenidoRequest {
    @Size(max = 180)
    private String nombreComercial;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String faviconUrl;

    @Size(max = 30)
    private String colorPrimario;

    @Size(max = 30)
    private String colorSecundario;

    @Size(max = 180)
    private String heroTitulo;

    @Size(max = 255)
    private String heroSubtitulo;

    @Size(max = 500)
    private String heroImagenUrl;

    @Valid
    @Size(max = 10)
    private List<StorefrontPortadaRequest> portadas = new ArrayList<>();

    @Size(max = 500)
    private String footerTexto;

    @Size(max = 180)
    private String quienesTitulo;

    @Size(max = 5000)
    private String quienesTexto;

    @Email
    @Size(max = 255)
    private String emailComercial;

    @Size(max = 80)
    private String telefonoComercial;

    @Size(max = 500)
    private String direccionComercial;

    @Valid
    @Size(max = 20)
    private List<StorefrontGaleriaItemRequest> quienesGaleria = new ArrayList<>();

    @Size(max = 500)
    private String instagramUrl;

    @Size(max = 500)
    private String facebookUrl;

    @Size(max = 500)
    private String tiktokUrl;

    @Size(max = 500)
    private String youtubeUrl;

    private boolean transferenciaHabilitada;

    @Size(max = 180)
    private String transferenciaAlias;

    @Size(max = 180)
    private String transferenciaTitular;

    @Size(max = 180)
    private String smtpHost;

    @Min(1)
    @Max(65535)
    private Integer smtpPort;

    @Size(max = 180)
    private String smtpUsername;

    @Size(max = 255)
    private String smtpPassword;

    private Boolean smtpAuth;

    private Boolean smtpStartTls;
}
