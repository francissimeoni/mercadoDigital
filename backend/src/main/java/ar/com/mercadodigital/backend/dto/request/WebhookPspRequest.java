package ar.com.mercadodigital.backend.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookPspRequest {

    @NotBlank
    private String idEventoPsp;

    @NotBlank
    private String tipoEvento;

    @NotBlank
    private String referenciaPedido;

    @NotBlank
    private String referenciaPago;

    @NotNull
    private BigDecimal monto;

    @NotBlank
    private String estado;
}
