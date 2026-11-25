package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicRmaResponse {
    private UUID id;
    private UUID pedidoId;
    private UUID pedidoItemId;
    private String productoNombre;
    private String motivo;
    private String estadoRma;
    private String estadoCliente;
    private String resolucionFinal;
    private BigDecimal montoCompensacion;
    private String referenciaCompensacion;
    private String trackingLogistica;
    private boolean rotuloDevolucionDescargado;
    private boolean rotuloReposicionGenerado;
    private boolean permiteDescargarRotuloDevolucion;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaEnvioSolicitado;
    private LocalDateTime fechaRotuloDevolucionDescargado;
    private LocalDateTime fechaRecibidoDeposito;
    private LocalDateTime fechaCierre;
    private LocalDateTime fechaRotuloReposicionGenerado;
    private LocalDateTime fechaReposicionSolicitadaProveedor;
    private LocalDateTime fechaReposicionRecibidaMd;
    private LocalDateTime fechaReposicionReenviadaCliente;
    private LocalDateTime fechaReposicionEntregadaCliente;
    private String estadoReposicion;
    private String resumenReposicion;
    private String reposicionTracking;
    private String reposicionTransportistaNombre;
    private String instruccionesSiguientePaso;
    private List<ReposicionBitacoraPasoResponse> reposicionBitacora;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RmaMensajeResponse> mensajes;
}
