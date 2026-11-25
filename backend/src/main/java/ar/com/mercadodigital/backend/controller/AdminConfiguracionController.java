package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.request.AdminCategoriaProductoRequest;
import ar.com.mercadodigital.backend.dto.request.AdminCorreoMdConfigRequest;
import ar.com.mercadodigital.backend.dto.request.AdminCorreoMdTestRequest;
import ar.com.mercadodigital.backend.dto.request.AdminCostoMdConfigRequest;
import ar.com.mercadodigital.backend.dto.request.AdminPspConfigRequest;
import ar.com.mercadodigital.backend.dto.request.AdminRevendedorMercadoPagoRequest;
import ar.com.mercadodigital.backend.dto.response.AdminCategoriaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCorreoMdConfigResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCostoMdConfigResponse;
import ar.com.mercadodigital.backend.dto.response.AdminPspConfigResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoOAuthLinkResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.service.CategoriaProductoService;
import ar.com.mercadodigital.backend.service.CorreoMdConfigService;
import ar.com.mercadodigital.backend.service.CostoOperativoMdService;
import ar.com.mercadodigital.backend.service.MercadoPagoMdConfigService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.PspConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/configuracion")
public class AdminConfiguracionController {

    private final CostoOperativoMdService costoOperativoMdService;
    private final PspConfigService pspConfigService;
    private final CategoriaProductoService categoriaProductoService;
    private final CorreoMdConfigService correoMdConfigService;
    private final MercadoPagoMdConfigService mercadoPagoMdConfigService;
    private final MercadoPagoOAuthService mercadoPagoOAuthService;

    @GetMapping("/costo-md")
    public ResponseEntity<AdminCostoMdConfigResponse> getCostoMd() {
        return ResponseEntity.ok(costoOperativoMdService.getConfiguracionAdmin());
    }

    @PutMapping("/costo-md")
    public ResponseEntity<AdminCostoMdConfigResponse> upsertCostoMd(@Valid @RequestBody AdminCostoMdConfigRequest request) {
        return ResponseEntity.ok(costoOperativoMdService.actualizarConfiguracion(request));
    }

    @GetMapping("/psp")
    public ResponseEntity<AdminPspConfigResponse> getPsp() {
        return ResponseEntity.ok(pspConfigService.getConfiguracionAdmin());
    }

    @PutMapping("/psp")
    public ResponseEntity<AdminPspConfigResponse> upsertPsp(@Valid @RequestBody AdminPspConfigRequest request) {
        return ResponseEntity.ok(pspConfigService.actualizarConfiguracion(request));
    }

    @GetMapping("/correo-md")
    public ResponseEntity<AdminCorreoMdConfigResponse> getCorreoMd() {
        return ResponseEntity.ok(correoMdConfigService.getConfiguracionAdmin());
    }

    @PutMapping("/correo-md")
    public ResponseEntity<AdminCorreoMdConfigResponse> upsertCorreoMd(@Valid @RequestBody AdminCorreoMdConfigRequest request) {
        return ResponseEntity.ok(correoMdConfigService.actualizarConfiguracion(request));
    }

    @PostMapping("/correo-md/test")
    public ResponseEntity<Void> testCorreoMd(@Valid @RequestBody AdminCorreoMdTestRequest request) {
        correoMdConfigService.enviarTest(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mercadopago-md")
    public ResponseEntity<AdminRevendedorMercadoPagoResponse> getMercadoPagoMd() {
        return ResponseEntity.ok(mercadoPagoMdConfigService.getEstado());
    }

    @PutMapping("/mercadopago-md")
    public ResponseEntity<AdminRevendedorMercadoPagoResponse> upsertMercadoPagoMd(
            @RequestBody AdminRevendedorMercadoPagoRequest request) {
        return ResponseEntity.ok(mercadoPagoMdConfigService.upsert(request));
    }

    @GetMapping("/mercadopago-md/oauth-link")
    public ResponseEntity<AdminRevendedorMercadoPagoOAuthLinkResponse> generarOAuthLinkMercadoPagoMd() {
        return ResponseEntity.ok(mercadoPagoOAuthService.generarAuthorizationLinkMd());
    }

    @GetMapping("/categorias-producto")
    public ResponseEntity<List<AdminCategoriaProductoResponse>> getCategoriasProducto() {
        return ResponseEntity.ok(categoriaProductoService.getCategoriasAdmin());
    }

    @PostMapping("/categorias-producto")
    public ResponseEntity<AdminCategoriaProductoResponse> createCategoriaProducto(
            @Valid @RequestBody AdminCategoriaProductoRequest request) {
        return ResponseEntity.ok(categoriaProductoService.crearCategoriaAdmin(request));
    }

    @PutMapping("/categorias-producto/{id}")
    public ResponseEntity<AdminCategoriaProductoResponse> updateCategoriaProducto(
            @PathVariable UUID id,
            @Valid @RequestBody AdminCategoriaProductoRequest request) {
        return ResponseEntity.ok(categoriaProductoService.actualizarCategoriaAdmin(id, request));
    }
}
