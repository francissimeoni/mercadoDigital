package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevendedorMetricasResponse {
    LocalDate desde;
    LocalDate hasta;
    long visitantesUnicos;
    long visitas;
    long vistasProductos;
    long consultas;
    long clicksWhatsapp;
    List<RevendedorMetricaRankingResponse> rubrosMasVistos;
    List<RevendedorMetricaRankingResponse> productosMasVistos;
}
