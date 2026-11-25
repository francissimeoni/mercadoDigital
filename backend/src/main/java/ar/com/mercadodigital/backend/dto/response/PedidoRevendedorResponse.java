package ar.com.mercadodigital.backend.dto.response;

import ar.com.mercadodigital.backend.dto.response.PedidoRevendedorItemResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PedidoRevendedorResponse {
    private UUID pedidoId;
    private String estadoPedido;
    private String estadoEnvio;
    private String direccionEnvio;
    private String categoriaPaquete;
    private String transportistaNombre;
    private BigDecimal totalFinal;
    private BigDecimal totalBruto;
    private BigDecimal totalEnvio;
    private BigDecimal totalSeguro;
    private BigDecimal totalDescuento;
    private String tracking;
    private LocalDateTime fechaCreacionPedido;
    private LocalDateTime fechaActualizacionPedido;
    private String clienteNombre;
    private String clienteEmail;
    private String clienteTelefono;
    private String metodoPago;
    private String estadoPago;
    private boolean comprobanteCargado;
    private LocalDateTime comprobanteCargadoAt;
    private List<PedidoRevendedorItemResponse> items;

    private BigDecimal totalPsp;
    private BigDecimal montoPago;
    private String referenciaPago;
    private LocalDateTime fechaActualizacionPago;
}
