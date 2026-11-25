package ar.com.mercadodigital.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.AdminCostoMdCategoriaResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCostoMdConfigResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCategoriaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.service.CategoriaProductoService;
import ar.com.mercadodigital.backend.service.CorreoMdConfigService;
import ar.com.mercadodigital.backend.service.CostoOperativoMdService;
import ar.com.mercadodigital.backend.service.MercadoPagoMdConfigService;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthService;
import ar.com.mercadodigital.backend.service.PspConfigService;

@WebMvcTest(AdminConfiguracionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminConfiguracionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CostoOperativoMdService costoOperativoMdService;
    @MockBean
    private CategoriaProductoService categoriaProductoService;
    @MockBean
    private PspConfigService pspConfigService;
    @MockBean
    private CorreoMdConfigService correoMdConfigService;
    @MockBean
    private MercadoPagoMdConfigService mercadoPagoMdConfigService;
    @MockBean
    private MercadoPagoOAuthService mercadoPagoOAuthService;

    @Test
    void getCostoMdDevuelve200() throws Exception {
        when(costoOperativoMdService.getConfiguracionAdmin()).thenReturn(response());

        mockMvc.perform(get("/admin/configuracion/costo-md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].categoriaPaquete").value("CHICO"))
                .andExpect(jsonPath("$.items[0].monto").value(1200.00));
    }

    @Test
    void upsertCostoMdDevuelve200() throws Exception {
        when(costoOperativoMdService.actualizarConfiguracion(any())).thenReturn(response());

        mockMvc.perform(put("/admin/configuracion/costo-md")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "categoriaPaquete": "CHICO", "monto": 1200.00 },
                                    { "categoriaPaquete": "MEDIANO", "monto": 2400.00 },
                                    { "categoriaPaquete": "VOLUMINOSO", "monto": 3900.00 }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[2].categoriaPaquete").value("VOLUMINOSO"));
    }

    @Test
    void categoriasProductoAdminSePuedenListarYCrear() throws Exception {
        UUID categoriaId = UUID.randomUUID();
        when(categoriaProductoService.getCategoriasAdmin()).thenReturn(List.of(
                AdminCategoriaProductoResponse.builder()
                        .id(categoriaId)
                        .nombre("Electrohogar")
                        .slug("electrohogar")
                        .activa(true)
                        .sugerida(false)
                        .build()));
        when(categoriaProductoService.crearCategoriaAdmin(any())).thenReturn(
                AdminCategoriaProductoResponse.builder()
                        .id(categoriaId)
                        .nombre("Electrohogar")
                        .slug("electrohogar")
                        .activa(true)
                        .sugerida(false)
                        .build());

        mockMvc.perform(get("/admin/configuracion/categorias-producto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("electrohogar"));

        mockMvc.perform(post("/admin/configuracion/categorias-producto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Electrohogar", "activa": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Electrohogar"));
    }

    @Test
    void mercadopagoMdDevuelve200() throws Exception {
        when(mercadoPagoMdConfigService.getEstado()).thenReturn(AdminRevendedorMercadoPagoResponse.builder()
                .estado("VINCULADA")
                .readyForCheckout(true)
                .mercadoPagoUserId("collector-md")
                .payoutEmail("ventas@mercado-digital.com.ar")
                .build());

        mockMvc.perform(get("/admin/configuracion/mercadopago-md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VINCULADA"))
                .andExpect(jsonPath("$.payoutEmail").value("ventas@mercado-digital.com.ar"));
    }

    private AdminCostoMdConfigResponse response() {
        return AdminCostoMdConfigResponse.builder()
                .items(List.of(
                        item("CHICO", "1200.00"),
                        item("MEDIANO", "2400.00"),
                        item("VOLUMINOSO", "3900.00")))
                .build();
    }

    private AdminCostoMdCategoriaResponse item(String categoria, String monto) {
        return AdminCostoMdCategoriaResponse.builder()
                .id(UUID.randomUUID())
                .categoriaPaquete(categoria)
                .monto(new BigDecimal(monto))
                .createdAt(LocalDateTime.of(2026, 4, 6, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 6, 10, 0))
                .build();
    }
}
