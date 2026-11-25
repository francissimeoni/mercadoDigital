package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminStorefrontRequest {

    private String dominioPublico;

    @NotBlank
    private String nombreComercial;

    private String logoUrl;
    private String faviconUrl;
    private String colorPrimario;
    private String colorSecundario;
    private String heroTitulo;
    private String heroSubtitulo;
    private String heroImagenUrl;
    private String emailComercial;
    private String telefonoComercial;
    private String direccionComercial;
    private String footerTexto;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private Boolean smtpAuth;
    private Boolean smtpStartTls;
    private Boolean sincronizacionPausada;

    @NotNull
    private Boolean publicada;
}
