package ar.com.mercadodigital.backend.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearPedidoCanonicoRequest {

    @NotNull
    private UUID clienteId;

    @NotNull
    private UUID revendedorId;

    private String codigoDescuento;

    @NotEmpty
    @Valid
    private List<PedidoItemCanonicoRequest> items;
}
