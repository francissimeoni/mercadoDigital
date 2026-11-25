/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.service.postcompra;

import java.util.UUID;

public record EventoComprobanteTransferenciaCargado(UUID pedidoId, UUID revendedorId, String revendedorEmail, String revendedorDominio) {
}

