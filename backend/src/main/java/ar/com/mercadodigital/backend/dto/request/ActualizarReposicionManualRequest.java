package ar.com.mercadodigital.backend.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarReposicionManualRequest {

    private UUID transportistaId;

    private String transportistaNombre;

    private String tracking;

    private String observacion;
}
