package ar.com.mercadodigital.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ar.com.mercadodigital.backend.dto.request.PublicAgregarRmaMensajeRequest;
import ar.com.mercadodigital.backend.dto.request.PublicSolicitarRmaRequest;
import ar.com.mercadodigital.backend.dto.response.PublicRmaResponse;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.RMA;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicRmaService {

    private final PublicPedidoTrackingService publicPedidoTrackingService;
    private final RmaService rmaService;

    @Transactional(readOnly = true)
    public List<PublicRmaResponse> getByTrackingToken(String trackingToken, String storefrontHost) {
        Pedido pedido = publicPedidoTrackingService.getPedidoPublicoByTrackingToken(trackingToken, storefrontHost);
        return rmaService.getPublicByPedido(pedido.getId());
    }

    @Transactional
    public PublicRmaResponse solicitar(String trackingToken, String storefrontHost, PublicSolicitarRmaRequest request) {
        return solicitar(trackingToken, storefrontHost, request, List.of());
    }

    @Transactional
    public PublicRmaResponse solicitar(
            String trackingToken,
            String storefrontHost,
            PublicSolicitarRmaRequest request,
            List<MultipartFile> archivos) {
        Pedido pedido = publicPedidoTrackingService.getPedidoPublicoByTrackingToken(trackingToken, storefrontHost);
        return rmaService.solicitarPublico(pedido, request, archivos);
    }

    @Transactional
    public PublicRmaResponse agregarMensaje(
            String trackingToken,
            String storefrontHost,
            UUID rmaId,
            PublicAgregarRmaMensajeRequest request) {
        return agregarMensaje(trackingToken, storefrontHost, rmaId, request, List.of());
    }

    @Transactional
    public PublicRmaResponse agregarMensaje(
            String trackingToken,
            String storefrontHost,
            UUID rmaId,
            PublicAgregarRmaMensajeRequest request,
            List<MultipartFile> archivos) {
        Pedido pedido = publicPedidoTrackingService.getPedidoPublicoByTrackingToken(trackingToken, storefrontHost);
        RMA rma = rmaService.getVisiblePublico(rmaId, pedido.getId())
                .orElseThrow(() -> new ResourceNotFoundException("RMA no encontrado: " + rmaId));
        return rmaService.agregarMensajeCliente(rma, request.getMensaje(), archivos);
    }

    @Transactional(readOnly = true)
    public void validarAccesoPublico(String trackingToken, String storefrontHost, UUID rmaId) {
        Pedido pedido = publicPedidoTrackingService.getPedidoPublicoByTrackingToken(trackingToken, storefrontHost);
        rmaService.getVisiblePublico(rmaId, pedido.getId())
                .orElseThrow(() -> new ResourceNotFoundException("RMA no encontrado: " + rmaId));
    }

    @Transactional
    public void registrarDescargaRotuloDevolucion(UUID rmaId) {
        rmaService.registrarDescargaRotuloDevolucion(rmaId);
    }
}
