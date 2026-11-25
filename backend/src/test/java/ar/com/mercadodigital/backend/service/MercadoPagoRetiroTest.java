package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.dto.request.PublicPedidoDirectoRequest;
import ar.com.mercadodigital.backend.repository.*;

class MercadoPagoRetiroTest {
    private MercadoPagoClient retirado() {
        var props = new MercadoPagoProperties();
        props.setAccessToken("TEST-only");
        props.setClientId("test");
        props.setClientSecret("test");
        props.setReturnUrl("https://example.invalid/return");
        props.setWebhookUrl("https://example.invalid/webhook");
        return new MercadoPagoClient(RestClient.builder(), props);
    }

    @Test void ningunTransportePuedeLlamarAlProveedorInclusoConCredenciales() {
        var client = retirado();
        assertFalse(client.isEnabled());
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
                () -> client.crearPreferencia(null, null, "token")).getStatusCode());
        assertThrows(ResponseStatusException.class, () -> client.obtenerPago("1", "token"));
        assertThrows(ResponseStatusException.class, () -> client.intercambiarAuthorizationCode("code", "state"));
        assertThrows(ResponseStatusException.class, () -> client.crearPayout("token", "order", null));
        assertThrows(ResponseStatusException.class, () -> client.validarMetodoPago(null));
        assertThrows(ResponseStatusException.class, () -> client.validarMetodoPago("PSP"));
        assertDoesNotThrow(() -> client.validarMetodoPago("TRANSFERENCIA"));
    }

    @Test void checkoutCacheadoTambienSeRechazaAntesDeConsultarOModificarPedido() {
        var pedidos = mock(PedidoRepository.class);
        var cuentas = mock(RevendedorMercadoPagoService.class);
        var service = new PublicPedidoPagoPspService(pedidos, retirado(), cuentas);
        assertThrows(ResponseStatusException.class, () -> service.prepararCheckout(UUID.randomUUID()));
        verifyNoInteractions(pedidos, cuentas);
    }

    @Test void compraDirectaSeRechazaAntesDeCrearClienteOPedido() {
        var service = mock(PublicPedidoDirectoService.class, CALLS_REAL_METHODS);
        var modo = mock(CatalogoModoService.class);
        var usuarios = mock(UsuarioRepository.class);
        var pedidos = mock(PedidoService.class);
        ReflectionTestUtils.setField(service, "modoCatalogo", modo);
        ReflectionTestUtils.setField(service, "mercadoPagoClient", retirado());
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarios);
        ReflectionTestUtils.setField(service, "pedidoService", pedidos);
        var request = new PublicPedidoDirectoRequest();
        request.setRevendedorId(UUID.randomUUID());
        request.setRevendedorProductoId(UUID.randomUUID());
        request.setMetodoPago("PSP");
        assertThrows(ResponseStatusException.class, () -> service.crearPedidoDirecto(request));
        verifyNoInteractions(usuarios, pedidos);
    }

    @Test void oauthNoGeneraEstadoNiVinculaCuentas() {
        var state = mock(MercadoPagoOAuthStateService.class);
        var cuentas = mock(RevendedorMercadoPagoService.class);
        var service = new MercadoPagoOAuthService(new MercadoPagoProperties(), state, retirado(), cuentas,
                mock(ProveedorMercadoPagoService.class), mock(MercadoPagoMdConfigService.class));
        assertThrows(ResponseStatusException.class, service::generarAuthorizationLinkMd);
        assertThrows(ResponseStatusException.class, () -> service.procesarCallback("code", "state"));
        verifyNoInteractions(state, cuentas);
    }

    @Test void noHayFallbackGlobalConCredencialesConservadas() {
        var props = new MercadoPagoProperties();
        props.setAccessToken("TEST-preserved");
        var accounts = mock(RevendedorMercadoPagoAccountRepository.class);
        var service = new RevendedorMercadoPagoService(mock(RevendedorRepository.class), accounts, props);
        assertFalse(service.resolveForRevendedor(UUID.randomUUID()).readyForCheckout());
        assertNull(service.resolveForMercadoPagoUserId("collector").accessToken());
        assertFalse(service.toResponse(null).isReadyForCheckout());
        verifyNoInteractions(accounts);
    }
}
