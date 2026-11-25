package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ar.com.mercadodigital.backend.dto.request.PublicAgregarRmaMensajeRequest;
import ar.com.mercadodigital.backend.dto.request.PublicSolicitarRmaRequest;
import ar.com.mercadodigital.backend.dto.response.PublicRmaResponse;
import ar.com.mercadodigital.backend.service.PublicRmaService;
import ar.com.mercadodigital.backend.service.RotuloRmaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/pedido/{trackingToken}/rma")
public class PublicRmaController {

    private final PublicRmaService publicRmaService;
    private final RotuloRmaService rotuloRmaService;

    @GetMapping
    public ResponseEntity<List<PublicRmaResponse>> getByTrackingToken(
            @PathVariable String trackingToken,
            @RequestParam String storefrontHost) {
        return ResponseEntity.ok(publicRmaService.getByTrackingToken(trackingToken, storefrontHost));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicRmaResponse> solicitar(
            @PathVariable String trackingToken,
            @RequestParam String storefrontHost,
            @Valid @RequestBody PublicSolicitarRmaRequest request) {
        return ResponseEntity.ok(publicRmaService.solicitar(trackingToken, storefrontHost, request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicRmaResponse> solicitarMultipart(
            @PathVariable String trackingToken,
            @RequestParam String storefrontHost,
            @Valid @ModelAttribute PublicSolicitarRmaRequest request,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {
        return ResponseEntity.ok(publicRmaService.solicitar(trackingToken, storefrontHost, request, archivos != null ? archivos : List.of()));
    }

    @PostMapping(value = "/{rmaId}/mensajes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicRmaResponse> agregarMensaje(
            @PathVariable String trackingToken,
            @PathVariable UUID rmaId,
            @RequestParam String storefrontHost,
            @Valid @RequestBody PublicAgregarRmaMensajeRequest request) {
        return ResponseEntity.ok(publicRmaService.agregarMensaje(trackingToken, storefrontHost, rmaId, request));
    }

    @PostMapping(value = "/{rmaId}/mensajes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicRmaResponse> agregarMensajeMultipart(
            @PathVariable String trackingToken,
            @PathVariable UUID rmaId,
            @RequestParam String storefrontHost,
            @Valid @ModelAttribute PublicAgregarRmaMensajeRequest request,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {
        return ResponseEntity.ok(publicRmaService.agregarMensaje(trackingToken, storefrontHost, rmaId, request, archivos != null ? archivos : List.of()));
    }

    @GetMapping(value = "/{rmaId}/rotulo-devolucion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getRotuloDevolucion(
            @PathVariable String trackingToken,
            @PathVariable UUID rmaId,
            @RequestParam String storefrontHost) {
        publicRmaService.validarAccesoPublico(trackingToken, storefrontHost, rmaId);
        publicRmaService.registrarDescargaRotuloDevolucion(rmaId);
        byte[] pdf = rotuloRmaService.generarRotuloDevolucionPdf(rmaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=rma-" + rmaId + "-devolucion.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
