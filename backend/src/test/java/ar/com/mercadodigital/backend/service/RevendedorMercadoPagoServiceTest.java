package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.dto.request.AdminRevendedorMercadoPagoRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorMercadoPagoAccount;
import ar.com.mercadodigital.backend.repository.RevendedorMercadoPagoAccountRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;

@ExtendWith(MockitoExtension.class)
class RevendedorMercadoPagoServiceTest {

    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private RevendedorMercadoPagoAccountRepository revendedorMercadoPagoAccountRepository;

    private final MercadoPagoProperties mercadoPagoProperties = new MercadoPagoProperties();
    private RevendedorMercadoPagoService revendedorMercadoPagoService;

    @BeforeEach
    void setUp() {
        mercadoPagoProperties.setEnabled(true); // Solo cobertura historica.
        revendedorMercadoPagoService =
                new RevendedorMercadoPagoService(revendedorRepository, revendedorMercadoPagoAccountRepository, mercadoPagoProperties);
    }

    @Test
    void upsertGuardaCuentaYLaMarcaVinculada() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);

        AdminRevendedorMercadoPagoRequest request = new AdminRevendedorMercadoPagoRequest();
        request.setMercadoPagoUserId("collector-1");
        request.setPublicKey("APP_USR-demo");
        request.setAccessToken("seller-token");
        request.setRefreshToken("refresh-token");
        request.setTokenExpiresAt(LocalDateTime.now().plusMonths(5));
        request.setLiveMode(false);
        request.setScope("offline_access payments write");

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.empty());
        when(revendedorMercadoPagoAccountRepository.save(any(RevendedorMercadoPagoAccount.class))).thenAnswer(invocation -> {
            RevendedorMercadoPagoAccount account = invocation.getArgument(0);
            account.setId(UUID.randomUUID());
            return account;
        });

        AdminRevendedorMercadoPagoResponse response = revendedorMercadoPagoService.upsert(revendedorId, request);

        assertEquals("VINCULADA", response.getEstado());
        assertTrue(response.isReadyForCheckout());
        assertTrue(response.isAccessTokenConfigured());
        assertTrue(response.isRefreshTokenConfigured());
        assertEquals("collector-1", response.getMercadoPagoUserId());
    }

    @Test
    void resolveForRevendedorUsaFallbackGlobalSiNoHayCuentaLista() {
        UUID revendedorId = UUID.randomUUID();
        mercadoPagoProperties.setAccessToken("global-token");

        when(revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.empty());

        RevendedorMercadoPagoService.RevendedorMercadoPagoResolution resolution =
                revendedorMercadoPagoService.resolveForRevendedor(revendedorId);

        assertEquals("global-token", resolution.accessToken());
        assertTrue(resolution.usaCredencialesGlobalesFallback());
        assertTrue(resolution.readyForCheckout());
    }

    @Test
    void toResponseMarcaCuentaVencidaConFallback() {
        mercadoPagoProperties.setAccessToken("global-token");

        RevendedorMercadoPagoAccount account = new RevendedorMercadoPagoAccount();
        account.setMercadoPagoUserId("collector-1");
        account.setAccessToken("seller-token");
        account.setTokenExpiresAt(LocalDateTime.now().minusDays(1));

        AdminRevendedorMercadoPagoResponse response = revendedorMercadoPagoService.toResponse(account);

        assertEquals("VENCIDA_CON_FALLBACK", response.getEstado());
        assertTrue(response.isReadyForCheckout());
        assertTrue(response.isUsaCredencialesGlobalesFallback());
        assertFalse(response.isLiveMode());
    }

    @Test
    void desvincularEliminaLaCuentaPropiaYLiberaUnaNuevaVinculacion() {
        UUID revendedorId = UUID.randomUUID();
        RevendedorMercadoPagoAccount account = new RevendedorMercadoPagoAccount();
        account.setMercadoPagoUserId("collector-anterior");
        account.setAccessToken("seller-token");

        when(revendedorRepository.existsById(revendedorId)).thenReturn(true);
        when(revendedorMercadoPagoAccountRepository.findByRevendedor_Id(revendedorId))
                .thenReturn(Optional.of(account));

        AdminRevendedorMercadoPagoResponse response = revendedorMercadoPagoService.desvincular(revendedorId);

        verify(revendedorMercadoPagoAccountRepository).delete(account);
        assertEquals("PENDIENTE", response.getEstado());
        assertFalse(response.isAccessTokenConfigured());
    }
}
