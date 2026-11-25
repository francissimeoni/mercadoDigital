/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.controller;

import ar.com.mercadodigital.backend.dto.request.StorefrontClientePerfilRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontClientePedidoResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontClientePerfilResponse;
import ar.com.mercadodigital.backend.service.StorefrontClienteCuentaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/cliente/storefront"})
public class StorefrontClienteCuentaController {
    private final StorefrontClienteCuentaService cuentaService;

    @GetMapping(value={"/pedidos"})
    public List<StorefrontClientePedidoResponse> listarPedidos(@RequestParam String storefrontHost, Authentication authentication) {
        return this.cuentaService.listarPedidos(storefrontHost, authentication.getName());
    }

    @GetMapping(value={"/perfil"})
    public StorefrontClientePerfilResponse obtenerPerfil(@RequestParam String storefrontHost, Authentication authentication) {
        return this.cuentaService.obtenerPerfil(storefrontHost, authentication.getName());
    }

    @PutMapping(value={"/perfil"})
    public StorefrontClientePerfilResponse actualizarPerfil(@Valid @RequestBody StorefrontClientePerfilRequest request, Authentication authentication) {
        return this.cuentaService.actualizarPerfil(request, authentication.getName());
    }

    public StorefrontClienteCuentaController(StorefrontClienteCuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }
}

