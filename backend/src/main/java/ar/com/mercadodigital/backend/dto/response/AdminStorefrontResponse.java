package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import ar.com.mercadodigital.backend.entity.TipoStorefront;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStorefrontResponse {
    private UUID id;
    private TipoStorefront tipoStorefront;
    private String dominioPublico;
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
    private boolean smtpConfigured;
    private boolean smtpAuth;
    private boolean smtpStartTls;
    private boolean publicada;
    private boolean sincronizacionPausada;
    private String sincronizacionPausadaPor;
    private LocalDateTime sincronizacionPausadaAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
