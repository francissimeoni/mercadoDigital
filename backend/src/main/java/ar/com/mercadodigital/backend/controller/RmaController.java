package ar.com.mercadodigital.backend.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

import ar.com.mercadodigital.backend.dto.request.ResolverRmaRequest;
import ar.com.mercadodigital.backend.dto.request.SolicitarRmaRequest;
import ar.com.mercadodigital.backend.dto.request.AgregarRmaMensajeRequest;
import ar.com.mercadodigital.backend.dto.request.ActualizarReposicionManualRequest;
import ar.com.mercadodigital.backend.dto.response.RmaResponse;
import ar.com.mercadodigital.backend.service.RmaService;
import ar.com.mercadodigital.backend.service.RotuloRmaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/rma")
public class RmaController {

    private final RmaService rmaService;
    private final RotuloRmaService rotuloRmaService;

    @GetMapping
    public ResponseEntity<List<RmaResponse>> getRmas(
            Authentication authentication,
            @RequestParam(required = false) UUID pedidoId) {
        return ResponseEntity.ok(rmaService.getVisibles(
                resolverEmail(authentication),
                resolverRoles(authentication),
                pedidoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RmaResponse> getDetalle(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(rmaService.getDetalleVisible(
                id,
                resolverEmail(authentication),
                resolverRoles(authentication)));
    }

    @PostMapping("/solicitar")
    public ResponseEntity<RmaResponse> solicitar(@Valid @RequestBody SolicitarRmaRequest request) {
        return ResponseEntity.ok(rmaService.solicitar(request));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<RmaResponse> aprobar(@PathVariable UUID id) {
        return ResponseEntity.ok(rmaService.aprobar(id));
    }

    @PostMapping("/{id}/solicitar-envio")
    public ResponseEntity<RmaResponse> solicitarEnvio(@PathVariable UUID id) {
        return ResponseEntity.ok(rmaService.solicitarEnvio(id));
    }

    @PostMapping("/{id}/recibir-en-deposito")
    public ResponseEntity<RmaResponse> recibirEnDeposito(@PathVariable UUID id) {
        return ResponseEntity.ok(rmaService.recibirEnDeposito(id));
    }

    @PostMapping("/{id}/reposicion/solicitar-proveedor")
    public ResponseEntity<RmaResponse> solicitarReposicionProveedor(
            @PathVariable UUID id,
            @RequestBody(required = false) ActualizarReposicionManualRequest request) {
        return ResponseEntity.ok(rmaService.solicitarReposicionProveedor(id, request));
    }

    @PostMapping("/{id}/reposicion/recibir-md")
    public ResponseEntity<RmaResponse> recibirReposicionMd(
            @PathVariable UUID id,
            @RequestBody(required = false) ActualizarReposicionManualRequest request) {
        return ResponseEntity.ok(rmaService.recibirReposicionMd(id, request));
    }

    @PostMapping("/{id}/reposicion/reenviar-cliente")
    public ResponseEntity<RmaResponse> reenviarReposicionCliente(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarReposicionManualRequest request) {
        return ResponseEntity.ok(rmaService.reenviarReposicionCliente(id, request));
    }

    @PostMapping("/{id}/reposicion/entregar-cliente")
    public ResponseEntity<RmaResponse> entregarReposicionCliente(
            @PathVariable UUID id,
            @RequestBody(required = false) ActualizarReposicionManualRequest request) {
        return ResponseEntity.ok(rmaService.entregarReposicionCliente(id, request));
    }

    @PostMapping("/{id}/resolver")
    public ResponseEntity<RmaResponse> resolver(@PathVariable UUID id, @Valid @RequestBody ResolverRmaRequest request) {
        return ResponseEntity.ok(rmaService.resolver(id, request, List.of()));
    }

    @PostMapping(value = "/{id}/resolver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RmaResponse> resolverMultipart(
            @PathVariable UUID id,
            @Valid @ModelAttribute ResolverRmaRequest request,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {
        return ResponseEntity.ok(rmaService.resolver(id, request, archivos != null ? archivos : List.of()));
    }

    @PostMapping(value = "/{id}/mensajes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RmaResponse> agregarMensaje(@PathVariable UUID id, @Valid @RequestBody AgregarRmaMensajeRequest request) {
        return ResponseEntity.ok(rmaService.agregarMensajeAdmin(id, request, List.of()));
    }

    @PostMapping(value = "/{id}/mensajes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RmaResponse> agregarMensajeMultipart(
            @PathVariable UUID id,
            @Valid @ModelAttribute AgregarRmaMensajeRequest request,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {
        return ResponseEntity.ok(rmaService.agregarMensajeAdmin(id, request, archivos != null ? archivos : List.of()));
    }

    @GetMapping(value = "/{id}/rotulo-devolucion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getRotuloDevolucion(@PathVariable UUID id) {
        byte[] pdf = rotuloRmaService.generarRotuloDevolucionPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=rma-" + id + "-devolucion.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/{id}/rotulo-reposicion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getRotuloReposicion(@PathVariable UUID id) {
        byte[] pdf = rotuloRmaService.generarRotuloReposicionPdf(id);
        rmaService.registrarGeneracionRotuloReposicion(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=rma-" + id + "-reposicion.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String resolverEmail(Principal principal) {
        return principal != null ? principal.getName() : "";
    }

    private List<String> resolverRoles(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();
    }
}
