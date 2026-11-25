package ar.com.mercadodigital.backend.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import ar.com.mercadodigital.backend.config.SecurityConfig;
import ar.com.mercadodigital.backend.security.*;
import ar.com.mercadodigital.backend.service.CatalogoAdminService;

@WebMvcTest(controllers = CatalogoAdminController.class, properties = {
        "app.security.enabled=true", "app.security.jwt-secret=catalogo-local-test-only"})
@Import({SecurityConfig.class, JwtFilter.class, JwtService.class})
class CatalogoAdminControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean CatalogoAdminService service;
    UUID tienda = UUID.randomUUID();
    String path() { return "/revendedores/" + tienda + "/catalogo-config"; }
    String token(String role) { return "Bearer " + jwt.generarToken(UUID.randomUUID(), "local@test.invalid", List.of(role)); }

    @Test void anonimoNoAccede() throws Exception { mvc.perform(get(path())).andExpect(status().isForbidden()); verifyNoInteractions(service); }
    @Test void clienteYAsesorNoAcceden() throws Exception {
        for (String rol : List.of("CLIENTE", "ASESOR_VENTA")) mvc.perform(get(path()).header("Authorization", token(rol))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void duenoYAdminLleganAlServicioConTiendaSeleccionada() throws Exception {
        for (String rol : List.of("REVENDEDOR", "ADMIN_MD")) mvc.perform(get(path()).header("Authorization", token(rol))).andExpect(status().isOk());
        verify(service, times(2)).configuracion(tienda);
    }
    @Test void ownershipAjenoResponde403SinExponerDatos() throws Exception {
        when(service.configuracion(tienda)).thenThrow(new AccessDeniedException("otra tienda"));
        mvc.perform(get(path()).header("Authorization", token("REVENDEDOR"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").exists());
    }
    @Test void validaCamposAntesDelServicio() throws Exception {
        String producto = path() + "/productos/" + UUID.randomUUID();
        for (String body : List.of(
                "{\"moneda\":\"EUR\",\"precioOriginal\":100,\"incluyeEnvio\":true,\"version\":-1}",
                "{\"moneda\":\"ARS\",\"precioOriginal\":0,\"incluyeEnvio\":true,\"version\":-1}",
                "{\"moneda\":\"ARS\",\"precioOriginal\":100,\"version\":-1}",
                "{\"moneda\":\"ARS\",\"precioOriginal\":100.001,\"incluyeEnvio\":true,\"version\":-1}")) {
            mvc.perform(put(producto).header("Authorization", token("REVENDEDOR")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void previewNoExigeStockNiAdmitePrecioFinalComoFuenteDelCalculo() throws Exception {
        mvc.perform(post(path() + "/productos/" + UUID.randomUUID() + "/vista-previa")
                .header("Authorization", token("REVENDEDOR")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\":\"USD\",\"precioOriginal\":\"100\",\"incluyeEnvio\":true,\"version\":-1,\"precioPublicoArs\":1}"))
                .andExpect(status().isOk());
        verify(service).previewProducto(eq(tienda), any(), argThat(input -> input.precioOriginal().intValueExact() == 100 && "USD".equals(input.moneda())));
    }
    @Test void rechazaCotizacionYEnvioNegativos() throws Exception {
        mvc.perform(post(path() + "/vista-previa").header("Authorization", token("REVENDEDOR"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"cotizacionUsd\":0,\"version\":-1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path() + "/categorias").header("Authorization", token("REVENDEDOR"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Mediano\",\"costoArs\":-1,\"version\":-1}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
