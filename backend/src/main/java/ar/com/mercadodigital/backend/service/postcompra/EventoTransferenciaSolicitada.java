/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.service.postcompra;

import java.math.BigDecimal;
import java.util.UUID;

public record EventoTransferenciaSolicitada(UUID pedidoId, UUID revendedorId, String clienteEmail, String clienteNombre, String trackingToken, BigDecimal total, String alias, String titular, String transferenciaToken) {
}

