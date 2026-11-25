/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.dto.request;

import ar.com.mercadodigital.backend.dto.request.RevendedorPrecioMasivoItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@lombok.Getter
@lombok.Setter
public class RevendedorAjusteMasivoPreciosRequest {
    @NotEmpty
    @Valid
    private List<RevendedorPrecioMasivoItemRequest> items;
}

