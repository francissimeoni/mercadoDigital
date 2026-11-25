package ar.com.mercadodigital.backend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevendedorStorefrontContenidoResponse {
    String nombreComercial;
    String logoUrl;
    String faviconUrl;
    String colorPrimario;
    String colorSecundario;
    String heroTitulo;
    String heroSubtitulo;
    String heroImagenUrl;
    List<StorefrontPortadaResponse> portadas;
    String footerTexto;
    String quienesTitulo;
    String quienesTexto;
    List<StorefrontGaleriaItemResponse> quienesGaleria;
    String emailComercial;
    String telefonoComercial;
    String direccionComercial;
    String instagramUrl;
    String facebookUrl;
    String tiktokUrl;
    String youtubeUrl;
    boolean transferenciaHabilitada;
    String transferenciaAlias;
    String transferenciaTitular;
    String smtpHost;
    Integer smtpPort;
    String smtpUsername;
    boolean smtpConfigured;
    boolean smtpAuth;
    boolean smtpStartTls;
}
