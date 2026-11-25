package ar.com.mercadodigital.backend.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import ar.com.mercadodigital.backend.config.SecurityConfig;
import ar.com.mercadodigital.backend.security.*;
import ar.com.mercadodigital.backend.service.*;
import ar.com.mercadodigital.backend.dto.CatalogoAdminDto.ProductoPreview;

@WebMvcTest(controllers = CatalogoFichaController.class, properties = {
        "app.security.enabled=true", "app.security.jwt-secret=catalogo-ficha-local-only"})
@Import({SecurityConfig.class, JwtFilter.class, JwtService.class})
class CatalogoFichaControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean CatalogoFichaService service;
    UUID tienda = UUID.randomUUID(), producto = UUID.randomUUID();
    String path() { return "/revendedores/" + tienda + "/catalogo-config/fichas"; }
    String token(String role) { return "Bearer " + jwt.generarToken(UUID.randomUUID(), "local@test.invalid", List.of(role)); }
    String body() { return """
        {"operacionId":"00000000-0000-0000-0000-000000000005","sku":"SKU","nombre":"Artículo",
        "activo":true,"promocion":false,"novedad":false,"versionFicha":-1,
        "precio":{"moneda":"ARS","precioOriginal":"99999999999999999.99","incluyeEnvio":true,"version":-1},
        "imagenes":[],"archivos":[]}
        """; }
    @Test void anonimoClienteYAsesorNoAcceden() throws Exception {
        mvc.perform(get(path() + "/" + producto)).andExpect(status().isForbidden());
        for (String role : List.of("CLIENTE", "ASESOR_VENTA")) mvc.perform(get(path() + "/" + producto)
                .header("Authorization", token(role))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void propietarioYAdminLleganAlServicioConAlcanceExplicito() throws Exception {
        for (String role : List.of("REVENDEDOR", "ADMIN_MD")) mvc.perform(get(path() + "/" + producto)
                .header("Authorization", token(role))).andExpect(status().isOk());
        verify(service, times(2)).obtener(tienda, producto);
    }
    @Test void ownershipSeTraduceA403() throws Exception {
        when(service.obtener(tienda, producto)).thenThrow(new AccessDeniedException("otra tienda"));
        mvc.perform(get(path() + "/" + producto).header("Authorization", token("REVENDEDOR"))).andExpect(status().isForbidden());
    }
    @Test void previewMantienePrecisionDecimalEnRespuesta() throws Exception {
        var calculo = new CatalogoPrecioCalculator().calcular("ARS", new BigDecimal("99999999999999999.99"), true, null, null);
        when(service.preview(eq(tienda), isNull(), any())).thenReturn(new ProductoPreview(calculo, "preview"));
        mvc.perform(post(path() + "/vista-previa").header("Authorization", token("REVENDEDOR"))
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isOk())
                .andExpect(jsonPath("$.calculo.precioPublicoArs").value("99999999999999999.99"));
    }
    @Test void altaMultipartNoExigePrecioAnteriorStockNiPaquete() throws Exception {
        mvc.perform(multipart(path()).file(new MockMultipartFile("ficha", "", "application/json", body().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .header("Authorization", token("REVENDEDOR"))).andExpect(status().isOk());
        verify(service).guardar(eq(tienda), isNull(), argThat(i -> i.precio().precioOriginal().toPlainString().equals("99999999999999999.99")), eq(List.of()), anyString());
    }
    @Test void contratoInvalidoNoLlegaAlServicio() throws Exception {
        for (String invalid : List.of(body().replace("\"incluyeEnvio\":true", "\"incluyeEnvio\":null"),
                body().replace("99999999999999999.99", "0"), body().replace("\"imagenes\":[]", "\"imagenes\":[null]")))
            mvc.perform(post(path() + "/vista-previa").header("Authorization", token("REVENDEDOR"))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
