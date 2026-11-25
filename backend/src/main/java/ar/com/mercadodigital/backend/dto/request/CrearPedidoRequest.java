package ar.com.mercadodigital.backend.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearPedidoRequest {

    @NotNull
    private UUID clienteId;

    @NotNull
    private UUID revendedorId;

    @NotEmpty
    @Valid
    private List<PedidoItemRequest> items;

    @NotBlank
    private String metodoPago;

    @NotBlank
    private String direccionEnvio;
}
