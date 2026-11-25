package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import ar.com.mercadodigital.backend.dto.request.*;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.*;

@ExtendWith(MockitoExtension.class)
class CatalogoCierreAnteriorTest {
    static final UUID TIENDA = UUID.fromString("00000000-0000-0000-0000-000000000002");
    @Spy CatalogoModoService modo = new CatalogoModoService(TIENDA.toString());
    @Mock RevendedorOwnershipService ownership;
    @Mock RevendedorProductoRepository productos;
    @Mock RevendedorRepository revendedores;
    @Mock RevendedorMarkupRepository markups;
    @Mock PlatformTransactionManager transacciones;
    @InjectMocks RevendedorProductoService fichas;
    @InjectMocks AdminRevendedorProductoImportService importador;
    @InjectMocks RevendedorPricingV1Service pricing;

    @Test void ajusteMasivoAntiguoSeRechazaAntesDeModificarPrecios() {
        assertThrows(OperacionEnConflictoException.class, () -> fichas.ajustarPreciosMasivamente(TIENDA, new RevendedorAjusteMasivoPreciosRequest()));
        verify(ownership).validarOwnership(TIENDA); verifyNoInteractions(productos, revendedores);
    }
    @Test void importacionAntiguaNoCreaFilasNiCantidadesFicticias() {
        assertThrows(OperacionEnConflictoException.class, () -> importador.importar(TIENDA, new AdminRevendedorProductoImportRequest()));
        verifyNoInteractions(productos, revendedores, transacciones);
    }
    @Test void escriturasPricingV1NoCambianPreciosDeCatalogo() {
        assertThrows(OperacionEnConflictoException.class, () -> pricing.actualizarConfiguracion(TIENDA, new RevendedorPricingConfigRequest()));
        assertThrows(OperacionEnConflictoException.class, () -> pricing.upsertOverride(TIENDA, UUID.randomUUID(), new RevendedorMarkupOverrideRequest()));
        assertThrows(OperacionEnConflictoException.class, () -> pricing.desactivarOverride(TIENDA, UUID.randomUUID()));
        verifyNoInteractions(revendedores, markups, productos);
    }
    @Test void restriccionesNoSeAplicanALaTiendaAnteriorNiAlProveedor() {
        UUID otra = UUID.randomUUID();
        assertDoesNotThrow(() -> modo.rechazarPreciosAnteriores(otra));
        assertDoesNotThrow(() -> modo.rechazarImportacionAnterior(otra));
        assertDoesNotThrow(() -> modo.rechazarNuevaCompra(null));
    }
    @Test void configuracionPrivadaInformaElModoRealDelServidor() {
        var revendedor = new ar.com.mercadodigital.backend.entity.Revendedor(); revendedor.setId(TIENDA);
        when(revendedores.findById(TIENDA)).thenReturn(java.util.Optional.of(revendedor));
        assertTrue(pricing.getConfiguracion(TIENDA).isModoCatalogo());
        verifyNoInteractions(productos, markups);
    }
}
