package ar.com.mercadodigital.backend.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MercadoPagoRetiradoFilterTest {
    @ParameterizedTest
    @ValueSource(strings = {"/webhooks/psp/mercadopago", "/webhooks/psp", "/public/mercadopago/oauth/callback",
        "/admin/configuracion/mercadopago-md", "/admin/revendedores/id/mercadopago/oauth-link",
        "/admin/proveedores/id/mercadopago", "/revendedores/id/mercadopago", "/public/pedido/token/reanudar-pago",
        "/public/pedido/id/pago", "/admin/configuracion/%6dercadopago-md"})
    void rutasRetiradasNoLleganAControladores(String path) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        var response = new MockHttpServletResponse();
        var reached = new AtomicBoolean();
        new MercadoPagoRetiradoFilter().doFilter(request, response, (req, res) -> reached.set(true));
        assertEquals(410, response.getStatus());
        assertTrue(response.getContentAsString().contains("MERCADOPAGO_RETIRADO"));
        assertFalse(reached.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/public/storefront/auth/google/callback", "/public/storefront",
        "/public/transferencias/token", "/public/pedido/token", "/revendedores/id/catalogo/configuracion"})
    void otrasRutasSiguenSuCadenaNormal(String path) throws Exception {
        var reached = new AtomicBoolean();
        new MercadoPagoRetiradoFilter().doFilter(new MockHttpServletRequest("GET", path),
                new MockHttpServletResponse(), (req, res) -> reached.set(true));
        assertTrue(reached.get());
    }
}
