package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReposicionBitacoraPasoResponse {
    private String id;
    private String titulo;
    private LocalDateTime fecha;
    private String observacion;
    private String tracking;
    private String transportistaNombre;
}
