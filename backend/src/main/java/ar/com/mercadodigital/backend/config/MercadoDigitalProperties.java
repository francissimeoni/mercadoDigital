package ar.com.mercadodigital.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "mercado-digital")
public class MercadoDigitalProperties {

    private BigDecimal pspFee = new BigDecimal("0.08");
    private BigDecimal seguroTransporte = new BigDecimal("0.02");
}
