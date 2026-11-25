package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductoPreguntaResponse {
    private UUID id;
    private UUID productoId;
    private String productoNombre;
    private String nombreContacto;
    private String emailContacto;
    private String telefonoContacto;
    private String pregunta;
    private String respuesta;
    private String estado;
    private LocalDateTime fechaPregunta;
    private LocalDateTime fechaRespuesta;
}
