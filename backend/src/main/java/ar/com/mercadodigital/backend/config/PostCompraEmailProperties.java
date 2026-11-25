package ar.com.mercadodigital.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.post-compra-email")
public class PostCompraEmailProperties {

    private String transport = "log";
    private String fromEmail = "no-reply@mercadodigital.local";
    private String fromName = "Mercado Digital";
    private String basePublicUrl = "http://localhost:5173";
}
