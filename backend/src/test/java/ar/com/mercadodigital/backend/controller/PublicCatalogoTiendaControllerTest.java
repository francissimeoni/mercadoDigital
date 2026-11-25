package ar.com.mercadodigital.backend.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ar.com.mercadodigital.backend.config.SecurityConfig;
import ar.com.mercadodigital.backend.security.*;
import ar.com.mercadodigital.backend.dto.response.*;
import ar.com.mercadodigital.backend.service.*;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;

@WebMvcTest(controllers = PublicCatalogoTiendaController.class, properties = {
        "app.security.enabled=true", "app.security.jwt-secret=catalogo-publico-local-test"})
@Import({SecurityConfig.class, JwtFilter.class, JwtService.class})
class PublicCatalogoTiendaControllerTest {
    @Autowired MockMvc mvc;
    @MockBean StorefrontService storefronts;
    @MockBean PublicCatalogoService catalogo;
    @MockBean CatalogoPublicoService independiente;
    @MockBean CatalogoModoService modo;
    UUID tienda = UUID.randomUUID(), producto = UUID.randomUUID();
    String host = "tienda.test.invalid";
    void preparar() {
        when(storefronts.resolverPorHost(host)).thenReturn(StorefrontResponse.builder().revendedorId(tienda).dominio(host).build());
    }
    @Test void listadoAnonimoResuelveHostEIgnoraIdInyectado() throws Exception {
        preparar(); when(catalogo.getCatalogo(tienda)).thenReturn(List.of());
        mvc.perform(get("/public/catalogo/tienda").param("host", host).param("revendedorId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        verify(catalogo).getCatalogo(tienda);
    }
    @Test void contactoAnonimoSoloConDatosComercialesDeLaTiendaResuelta() throws Exception {
        preparar(); when(modo.activo(tienda)).thenReturn(true);
        when(independiente.contacto(tienda, producto, host)).thenReturn(new PublicCatalogoContactoResponse(producto,
                "Producto", "SKU", "128000.00", null, host, List.of(new PublicCatalogoContactoResponse.Contacto("responsable", "Tienda", true, "+5491112345678"))));
        mvc.perform(get("/public/catalogo/tienda/productos/" + producto + "/contacto").param("host", host).param("asesorId", "otra-tienda"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.precioPublicoArs").value("128000.00"))
                .andExpect(jsonPath("$.contactos[0].responsable").value(true))
                .andExpect(jsonPath("$.cotizacionUsd").doesNotExist()).andExpect(jsonPath("$.contactos[0].email").doesNotExist());
        verify(independiente).contacto(tienda, producto, host);
    }
    @Test void hostDesconocidoNoPermiteConsultarPorId() throws Exception {
        when(storefronts.resolverPorHost(host)).thenThrow(new ResourceNotFoundException("No disponible"));
        mvc.perform(get("/public/catalogo/tienda").param("host", host).param("revendedorId", tienda.toString())).andExpect(status().isNotFound());
        verifyNoInteractions(catalogo, independiente);
    }
    @Test void contactoNuevoNoSeExponeConModoApagado() throws Exception {
        preparar(); mvc.perform(get("/public/catalogo/tienda/productos/" + producto + "/contacto").param("host", host)).andExpect(status().isNotFound());
        verifyNoInteractions(independiente);
    }
}
