package ar.com.mercadodigital.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCanonizacionCrearCanonicoRequest {
    private String marca;
    private String modelo;
    private String nombrePublico;
    private String atributosJson;
}
