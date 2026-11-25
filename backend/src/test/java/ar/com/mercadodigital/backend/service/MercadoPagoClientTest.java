package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.exception.IntegracionPspException;

class MercadoPagoClientTest {

    @Test
    void credencialesAusentesRompenPreparacionAntesDeLlamarAlProveedor() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setEnabled(true); // Cobertura del adaptador historico.
        properties.setReturnUrl("");
        properties.setWebhookUrl("");
        properties.setAccessToken("");

        MercadoPagoClient mercadoPagoClient = new MercadoPagoClient(RestClient.builder(), properties);

        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID());

        Pago pago = new Pago();
        pago.setId(UUID.randomUUID());
        pago.setMonto(new BigDecimal("100.00"));
        pago.setPspReferencia("psp-publico-" + pago.getId());

        assertThrows(IntegracionPspException.class, () -> mercadoPagoClient.crearPreferencia(pedido, pago));
    }
}
