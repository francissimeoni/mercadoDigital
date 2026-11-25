/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.service.postcompra;

public record BrandingPostCompra(String nombreComercial, String colorPrimario, String logoUrl, String baseUrlPublica, String emailComercial, String telefonoComercial, String direccionComercial, String emailRemitente, String nombreRemitente, String smtpHost, Integer smtpPort, String smtpUsername, String smtpPassword, boolean smtpAuth, boolean smtpStartTls) {
    public BrandingPostCompra(String nombreComercial, String colorPrimario, String logoUrl, String baseUrlPublica, String emailComercial, String emailRemitente, String nombreRemitente, String smtpHost, Integer smtpPort, String smtpUsername, String smtpPassword, boolean smtpAuth, boolean smtpStartTls) {
        this(nombreComercial, colorPrimario, logoUrl, baseUrlPublica, emailComercial, null, null, emailRemitente, nombreRemitente, smtpHost, smtpPort, smtpUsername, smtpPassword, smtpAuth, smtpStartTls);
    }

    public String trackingUrl(String trackingToken) {
        return this.baseUrlPublica + "/pedido/" + trackingToken;
    }
}

