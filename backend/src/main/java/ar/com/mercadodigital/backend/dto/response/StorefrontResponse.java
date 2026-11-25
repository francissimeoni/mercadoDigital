package ar.com.mercadodigital.backend.dto.response;

import ar.com.mercadodigital.backend.dto.response.StorefrontGaleriaItemResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontPortadaResponse;
import java.math.BigDecimal;

import java.util.UUID;
import java.util.List;

import ar.com.mercadodigital.backend.entity.TipoStorefront;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StorefrontResponse {
    TipoStorefront tipoStorefront;
    UUID revendedorId;
    UUID proveedorId;
    String dominio;
    String nombreComercial;
    String logoUrl;
    String faviconUrl;
    String colorPrimario;
    String colorSecundario;
    String heroTitulo;
    String heroSubtitulo;
    String heroImagenUrl;
    List<StorefrontPortadaResponse> portadas;
    String emailComercial;
    String telefonoComercial;
    String direccionComercial;
    String footerTexto;
    String quienesTitulo;
    String quienesTexto;
    List<StorefrontGaleriaItemResponse> quienesGaleria;
    String instagramUrl;
    String facebookUrl;
    String tiktokUrl;
    String youtubeUrl;
    boolean transferenciaHabilitada;
    String transferenciaAlias;
    String transferenciaTitular;
    boolean publicada;
    boolean modoCatalogo;

    private final boolean mercadoPagoHabilitado;
    private final BigDecimal mercadoPagoRecargoPorcentaje;
}
