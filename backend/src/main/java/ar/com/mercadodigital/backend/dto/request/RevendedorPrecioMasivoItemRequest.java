/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@lombok.Getter
@lombok.Setter
public class RevendedorPrecioMasivoItemRequest {
    @NotNull
    private UUID productoId;
    @NotNull
    @DecimalMin(value="0.00")
    private BigDecimal precioFinal;
}

