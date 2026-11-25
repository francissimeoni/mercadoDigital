package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsultaDisponibilidadResponse {
    private UUID id;
    private UUID productoId;
    private String productoNombre;
    private String nombreContacto;
    private String emailContacto;
    private String telefonoContacto;
    private String localidad;
    private String observaciones;
    private String estado;
    private String respuestaProveedor;
    private String checkoutToken;
    private LocalDateTime fechaConsulta;
    private LocalDateTime fechaRespuesta;
}
