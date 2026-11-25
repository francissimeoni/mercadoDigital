package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoItemCanonicoRequest {

    @NotNull
    private UUID productoCanonicoId;

    @NotNull
    @Min(1)
    private Integer cantidad;

    private boolean tieneSeguro;
}
