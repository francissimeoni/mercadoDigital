package ar.com.mercadodigital.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.config.SecurityConfig;
import ar.com.mercadodigital.backend.dto.response.EnvioResponse;
import ar.com.mercadodigital.backend.security.JwtFilter;
import ar.com.mercadodigital.backend.security.JwtService;
import ar.com.mercadodigital.backend.service.EnvioService;
import ar.com.mercadodigital.backend.service.LiquidacionProveedorService;
import ar.com.mercadodigital.backend.service.RotuloEnvioService;

@WebMvcTest(controllers = { EnvioController.class }, properties = {
        "app.security.enabled=true",
        "app.security.jwt-secret=test-secret"
})
@Import({ SecurityConfig.class, JwtFilter.class, JwtService.class })
class SecurityProveedorEnvioAccessTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private EnvioService envioService;
    @MockBean
    private LiquidacionProveedorService liquidacionProveedorService;
    @MockBean
    private RotuloEnvioService rotuloEnvioService;

    @Test
    void listoParaRetiroExigeRolProveedor() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(envioService.marcarListoParaRetiro(pedidoId)).thenReturn(EnvioResponse.builder()
                .pedidoId(pedidoId)
                .estadoEnvio("LISTO_PARA_RETIRO")
                .tracking(null)
                .transportistaNombre("N/A")
                .costoEnvio(BigDecimal.ZERO)
                .build());

        String tokenProveedor = jwtService.generarToken(
                UUID.randomUUID(),
                "proveedor@test.com",
                List.of("PROVEEDOR"));
        mockMvc.perform(post("/envios/{pedidoId}/listo-para-retiro", pedidoId)
                        .header("Authorization", "Bearer " + tokenProveedor))
                .andExpect(status().isOk());

        String tokenLogistica = jwtService.generarToken(
                UUID.randomUUID(),
                "logistica@test.com",
                List.of("LOGISTICA_MD"));
        mockMvc.perform(post("/envios/{pedidoId}/listo-para-retiro", pedidoId)
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isForbidden());
    }
}
