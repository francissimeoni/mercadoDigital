package ar.com.mercadodigital.backend.controller;

import ar.com.mercadodigital.backend.dto.request.RevendedorAjusteMasivoPreciosRequest;
import ar.com.mercadodigital.backend.service.RevendedorPedidoService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.com.mercadodigital.backend.dto.request.RevendedorAsesorRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorProductoMediaRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorProductoRequest;
import ar.com.mercadodigital.backend.dto.request.ResponderDisponibilidadRequest;
import ar.com.mercadodigital.backend.dto.request.ResponderProductoPreguntaRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorStorefrontContenidoRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorDespacharPedidoRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoOAuthLinkResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.dto.response.ConsultaDisponibilidadResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoRevendedorResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoPreguntaResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorAsesorResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorProductoMediaResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorProductoResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorStorefrontHostResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorStorefrontContenidoResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorMetricasResponse;
import ar.com.mercadodigital.backend.dto.response.AdminStorefrontAssetResponse;
import ar.com.mercadodigital.backend.service.BandejasOperativasService;
import ar.com.mercadodigital.backend.service.RevendedorAsesorService;
import ar.com.mercadodigital.backend.service.RevendedorConsultasService;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import ar.com.mercadodigital.backend.service.RevendedorOwnershipService;
import ar.com.mercadodigital.backend.service.RevendedorProductoService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import ar.com.mercadodigital.backend.service.StorefrontAssetStorageService;
import ar.com.mercadodigital.backend.service.TransferenciaService;
import ar.com.mercadodigital.backend.service.NotificacionConsultaEmailService;
import ar.com.mercadodigital.backend.service.StorefrontMetricasService;
import ar.com.mercadodigital.backend.service.RotuloEnvioService;
import ar.com.mercadodigital.backend.service.EnvioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/revendedores")
public class RevendedorController {

    private final BandejasOperativasService bandejasOperativasService;
    private final RevendedorAsesorService revendedorAsesorService;
    private final RevendedorProductoService revendedorProductoService;
    private final RevendedorConsultasService revendedorConsultasService;
    private final RevendedorOwnershipService ownershipService;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final MercadoPagoOAuthService mercadoPagoOAuthService;
    private final StorefrontService storefrontService;
    private final StorefrontAssetStorageService storefrontAssetStorageService;
    private final TransferenciaService transferenciaService;
    private final NotificacionConsultaEmailService notificacionConsultaEmailService;
    private final StorefrontMetricasService storefrontMetricasService;
    private final RotuloEnvioService rotuloEnvioService;
    private final EnvioService envioService;

    @GetMapping("/{revendedorId}/storefront/contenido")
    public ResponseEntity<RevendedorStorefrontContenidoResponse> getContenidoStorefront(@PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(storefrontService.getContenidoRevendedor(revendedorId));
    }

    @PutMapping("/{revendedorId}/storefront/contenido")
    public ResponseEntity<RevendedorStorefrontContenidoResponse> actualizarContenidoStorefront(
            @PathVariable UUID revendedorId,
            @Valid @RequestBody RevendedorStorefrontContenidoRequest request) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(storefrontService.actualizarContenidoRevendedor(revendedorId, request));
    }

    @PostMapping("/{revendedorId}/storefront/email/prueba")
    public ResponseEntity<Void> probarCorreoStorefront(@PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        notificacionConsultaEmailService.enviarPrueba(revendedorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{revendedorId}/storefront/assets")
    public ResponseEntity<AdminStorefrontAssetResponse> uploadStorefrontAsset(
            @PathVariable UUID revendedorId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "tipo", required = false) String tipo) {
        ownershipService.validarOwnership(revendedorId);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storefrontAssetStorageService.store(archivo, tipo, baseUrl));
    }

    @GetMapping("/{revendedorId}/pedidos")
    public ResponseEntity<List<PedidoRevendedorResponse>> getPedidos(@PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(bandejasOperativasService.getPedidosRevendedor(revendedorId));
    }

    @GetMapping(value = "/{revendedorId}/pedidos/{pedidoId}/rotulo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getRotuloPedido(@PathVariable UUID revendedorId, @PathVariable UUID pedidoId) {
        ownershipService.validarOwnership(revendedorId);
        byte[] pdf = rotuloEnvioService.generarRotuloPdfRevendedor(revendedorId, pedidoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=etiqueta-" + pedidoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{revendedorId}/pedidos/{pedidoId}/despachar")
    public ResponseEntity<Void> despacharPedido(@PathVariable UUID revendedorId, @PathVariable UUID pedidoId,
            @Valid @RequestBody RevendedorDespacharPedidoRequest request) {
        ownershipService.validarOwnership(revendedorId);
        envioService.despacharPorRevendedor(revendedorId, pedidoId,
                request.getTransportistaNombre(), request.getTracking());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{revendedorId}/pedidos/{pedidoId}/entregar")
    public ResponseEntity<Void> entregarPedido(@PathVariable UUID revendedorId, @PathVariable UUID pedidoId) {
        ownershipService.validarOwnership(revendedorId);
        envioService.marcarEntregadoPorRevendedor(revendedorId, pedidoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{revendedorId}/pedidos/{pedidoId}/comprobante")
    public ResponseEntity<FileSystemResource> getComprobanteTransferencia(
            @PathVariable UUID revendedorId, @PathVariable UUID pedidoId) {
        ownershipService.validarOwnership(revendedorId);
        TransferenciaService.ComprobanteArchivo comprobante = transferenciaService.getComprobante(revendedorId, pedidoId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(comprobante.contentType()); } catch (Exception ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(mediaType)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(comprobante.nombreOriginal() != null ? comprobante.nombreOriginal() : "comprobante",
                                        java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(comprobante.path()));
    }

    @PutMapping("/{revendedorId}/pedidos/{pedidoId}/confirmar-transferencia")
    public ResponseEntity<Void> confirmarTransferencia(@PathVariable UUID revendedorId, @PathVariable UUID pedidoId) {
        ownershipService.validarOwnership(revendedorId);
        transferenciaService.confirmar(revendedorId, pedidoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{revendedorId}/asesores")
    public ResponseEntity<List<RevendedorAsesorResponse>> getAsesores(@PathVariable UUID revendedorId) {
        return ResponseEntity.ok(revendedorAsesorService.listar(revendedorId));
    }

    @GetMapping("/{revendedorId}/storefront-host")
    public ResponseEntity<RevendedorStorefrontHostResponse> getStorefrontHost(@PathVariable UUID revendedorId) {
        return ResponseEntity.ok(revendedorAsesorService.getStorefrontHost(revendedorId));
    }

    @PostMapping(value = "/{revendedorId}/asesores", consumes = "application/json")
    public ResponseEntity<RevendedorAsesorResponse> crearAsesor(
            @PathVariable UUID revendedorId,
            @Valid @RequestBody RevendedorAsesorRequest request) {
        return ResponseEntity.ok(revendedorAsesorService.crear(revendedorId, request));
    }

    @PutMapping(value = "/{revendedorId}/asesores/{asesorId}", consumes = "application/json")
    public ResponseEntity<RevendedorAsesorResponse> actualizarAsesor(
            @PathVariable UUID revendedorId,
            @PathVariable UUID asesorId,
            @Valid @RequestBody RevendedorAsesorRequest request) {
        return ResponseEntity.ok(revendedorAsesorService.actualizar(revendedorId, asesorId, request));
    }

    @PostMapping(value = "/{revendedorId}/asesores", consumes = "multipart/form-data")
    public ResponseEntity<RevendedorAsesorResponse> crearAsesorConFoto(@PathVariable UUID revendedorId,
            @Valid @org.springframework.web.bind.annotation.RequestPart("datos") RevendedorAsesorRequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(revendedorAsesorService.guardarConFoto(revendedorId, null, request, foto,
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()));
    }

    @PutMapping(value = "/{revendedorId}/asesores/{asesorId}", consumes = "multipart/form-data")
    public ResponseEntity<RevendedorAsesorResponse> actualizarAsesorConFoto(@PathVariable UUID revendedorId,
            @PathVariable UUID asesorId,
            @Valid @org.springframework.web.bind.annotation.RequestPart("datos") RevendedorAsesorRequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(revendedorAsesorService.guardarConFoto(revendedorId, asesorId, request, foto,
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()));
    }

    @GetMapping("/{revendedorId}/productos-propios")
    public ResponseEntity<List<RevendedorProductoResponse>> getProductosPropios(
            @PathVariable UUID revendedorId) {
        return ResponseEntity.ok(revendedorProductoService.listar(revendedorId));
    }

    @GetMapping("/{revendedorId}/categorias-producto")
    public ResponseEntity<List<ProveedorCategoriaProductoResponse>> getCategoriasProducto(
            @PathVariable UUID revendedorId) {
        return ResponseEntity.ok(revendedorProductoService.listarCategorias(revendedorId));
    }

    @GetMapping("/{revendedorId}/productos-propios/consultas")
    public ResponseEntity<List<ProductoPreguntaResponse>> getConsultasProductosPropios(
            @PathVariable UUID revendedorId) {
        return ResponseEntity.ok(revendedorConsultasService.listarPreguntas(revendedorId));
    }

    @PutMapping("/{revendedorId}/productos-propios/consultas/{preguntaId}/respuesta")
    public ResponseEntity<ProductoPreguntaResponse> responderConsultaProductoPropio(
            @PathVariable UUID revendedorId,
            @PathVariable UUID preguntaId,
            @Valid @RequestBody ResponderProductoPreguntaRequest request) {
        return ResponseEntity.ok(revendedorConsultasService.responderPregunta(revendedorId, preguntaId, request));
    }

    @GetMapping("/{revendedorId}/productos-propios/disponibilidad")
    public ResponseEntity<List<ConsultaDisponibilidadResponse>> getDisponibilidadProductosPropios(
            @PathVariable UUID revendedorId) {
        return ResponseEntity.ok(revendedorConsultasService.listarDisponibilidad(revendedorId));
    }

    @PutMapping("/{revendedorId}/productos-propios/disponibilidad/{consultaId}/respuesta")
    public ResponseEntity<ConsultaDisponibilidadResponse> responderDisponibilidadProductoPropio(
            @PathVariable UUID revendedorId,
            @PathVariable UUID consultaId,
            @Valid @RequestBody ResponderDisponibilidadRequest request) {
        return ResponseEntity.ok(revendedorConsultasService.responderDisponibilidad(revendedorId, consultaId, request));
    }

    @GetMapping("/{revendedorId}/mercadopago")
    public ResponseEntity<AdminRevendedorMercadoPagoResponse> getMercadoPago(
            @PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(revendedorMercadoPagoService.get(revendedorId));
    }

    @DeleteMapping("/{revendedorId}/mercadopago")
    public ResponseEntity<AdminRevendedorMercadoPagoResponse> desvincularMercadoPago(
            @PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(revendedorMercadoPagoService.desvincular(revendedorId));
    }

    @GetMapping("/{revendedorId}/metricas")
    public ResponseEntity<RevendedorMetricasResponse> getMetricas(
            @PathVariable UUID revendedorId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(storefrontMetricasService.obtener(revendedorId, desde, hasta));
    }

    @GetMapping("/{revendedorId}/mercadopago/oauth-link")
    public ResponseEntity<AdminRevendedorMercadoPagoOAuthLinkResponse> getMercadoPagoOAuthLink(
            @PathVariable UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return ResponseEntity.ok(mercadoPagoOAuthService.generarAuthorizationLink(revendedorId));
    }

    @PostMapping("/{revendedorId}/productos-propios")
    public ResponseEntity<RevendedorProductoResponse> crearProductoPropio(
            @PathVariable UUID revendedorId,
            @Valid @RequestBody RevendedorProductoRequest request) {
        return ResponseEntity.ok(revendedorProductoService.crear(revendedorId, request));
    }

    @PutMapping("/{revendedorId}/productos-propios/{productoId}")
    public ResponseEntity<RevendedorProductoResponse> actualizarProductoPropio(
            @PathVariable UUID revendedorId,
            @PathVariable UUID productoId,
            @Valid @RequestBody RevendedorProductoRequest request) {
        return ResponseEntity.ok(revendedorProductoService.actualizar(revendedorId, productoId, request));
    }

    @PostMapping("/{revendedorId}/productos-propios/{productoId}/media")
    public ResponseEntity<RevendedorProductoMediaResponse> agregarMediaProductoPropio(
            @PathVariable UUID revendedorId,
            @PathVariable UUID productoId,
            @Valid @RequestBody RevendedorProductoMediaRequest request) {
        return ResponseEntity.ok(revendedorProductoService.agregarMedia(revendedorId, productoId, request));
    }

    @PutMapping("/{revendedorId}/productos-propios/{productoId}/media/{mediaId}")
    public ResponseEntity<RevendedorProductoMediaResponse> actualizarMediaProductoPropio(
            @PathVariable UUID revendedorId,
            @PathVariable UUID productoId,
            @PathVariable UUID mediaId,
            @Valid @RequestBody RevendedorProductoMediaRequest request) {
        return ResponseEntity.ok(revendedorProductoService.actualizarMedia(revendedorId, productoId, mediaId, request));
    }

    @DeleteMapping("/{revendedorId}/productos-propios/{productoId}/media/{mediaId}")
    public ResponseEntity<Void> eliminarMediaProductoPropio(
            @PathVariable UUID revendedorId,
            @PathVariable UUID productoId,
            @PathVariable UUID mediaId) {
        revendedorProductoService.eliminarMedia(revendedorId, productoId, mediaId);
        return ResponseEntity.noContent().build();
    }

    private final RevendedorPedidoService revendedorPedidoService;

    @PutMapping(value={"/{revendedorId}/pedidos/{pedidoId}/cancelar-impago"})
    public ResponseEntity<Void> cancelarPedidoImpago(@PathVariable UUID revendedorId, @PathVariable UUID pedidoId) {
        this.ownershipService.validarOwnership(revendedorId);
        this.revendedorPedidoService.cancelarImpago(revendedorId, pedidoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value={"/{revendedorId}/productos-propios/precios/ajuste-masivo"})
    public ResponseEntity<List<RevendedorProductoResponse>> ajustarPreciosMasivamente(@PathVariable UUID revendedorId, @Valid @RequestBody RevendedorAjusteMasivoPreciosRequest request) {
        return ResponseEntity.ok(this.revendedorProductoService.ajustarPreciosMasivamente(revendedorId, request));
    }
}
