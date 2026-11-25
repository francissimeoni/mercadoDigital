package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RmaResponse {
    private UUID id;
    private UUID pedidoItemId;
    private String motivo;
    private String descripcion;
    private String estadoRma;
    private String resolucionFinal;
    private String observacionesInspeccion;
    private String responsableProbable;
    private BigDecimal montoCompensacion;
    private String referenciaCompensacion;
    private UUID pedidoId;
    private String trackingToken;
    private String estadoPedido;
    private String estadoEnvio;
    private String trackingLogistica;
    private String transportistaNombre;
    private String clienteNombre;
    private String clienteEmail;
    private String revendedorNombre;
    private String storefrontNombre;
    private String storefrontDominio;
    private String proveedorNombre;
    private String proveedorEmail;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal precioItemFinal;
    private BigDecimal precioCosto;
    private boolean tieneSeguro;
    private BigDecimal montoSeguro;
    private BigDecimal totalEnvioPedido;
    private String direccionEnvio;
    private boolean rotuloDevolucionDescargado;
    private boolean rotuloReposicionGenerado;
    private boolean permiteSolicitarEnvio;
    private boolean permiteDescargarRotuloDevolucion;
    private boolean permiteDescargarRotuloReposicion;
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
    private boolean permiteSolicitarReposicionProveedor;
    private boolean permiteRegistrarRecepcionReposicionMd;
    private boolean permiteReenviarReposicionCliente;
    private boolean permiteEntregarReposicionCliente;
    private String instruccionesSiguientePaso;
    private List<ReposicionBitacoraPasoResponse> reposicionBitacora;
    private List<RmaMensajeResponse> mensajes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
