package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.request.AdminCostoMdCategoriaRequest;
import ar.com.mercadodigital.backend.dto.request.AdminCostoMdConfigRequest;
import ar.com.mercadodigital.backend.entity.ConfiguracionCostoMdCategoria;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.ConfiguracionCostoMdCategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CostoOperativoMdServiceTest {

    @Mock
    private ConfiguracionCostoMdCategoriaRepository configuracionCostoMdCategoriaRepository;

    @InjectMocks
    private CostoOperativoMdService costoOperativoMdService;

    @Test
    void getConfiguracionAdminCreaDefaultsFaltantes() {
        when(configuracionCostoMdCategoriaRepository.findAll()).thenReturn(List.of());
        when(configuracionCostoMdCategoriaRepository.save(any(ConfiguracionCostoMdCategoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = costoOperativoMdService.getConfiguracionAdmin();

        assertEquals(3, response.getItems().size());
        verify(configuracionCostoMdCategoriaRepository, times(3)).save(any(ConfiguracionCostoMdCategoria.class));
    }

    @Test
    void actualizarConfiguracionPersisteMontosPorCategoria() {
        ConfiguracionCostoMdCategoria chico = categoria("CHICO", BigDecimal.ZERO);
        ConfiguracionCostoMdCategoria mediano = categoria("MEDIANO", BigDecimal.ZERO);
        ConfiguracionCostoMdCategoria voluminoso = categoria("VOLUMINOSO", BigDecimal.ZERO);
        when(configuracionCostoMdCategoriaRepository.findAll()).thenReturn(List.of(chico, mediano, voluminoso));
        when(configuracionCostoMdCategoriaRepository.save(any(ConfiguracionCostoMdCategoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminCostoMdConfigRequest request = new AdminCostoMdConfigRequest();
        request.setItems(List.of(
                requestItem("CHICO", "1200.00"),
                requestItem("MEDIANO", "2400.00"),
                requestItem("VOLUMINOSO", "3900.00")));

        var response = costoOperativoMdService.actualizarConfiguracion(request);

        assertEquals(3, response.getItems().size());
        assertEquals(0, new BigDecimal("1200.00").compareTo(chico.getMonto()));
        assertEquals(0, new BigDecimal("2400.00").compareTo(mediano.getMonto()));
        assertEquals(0, new BigDecimal("3900.00").compareTo(voluminoso.getMonto()));
    }

    @Test
    void actualizarConfiguracionRechazaCategoriasDuplicadas() {
        when(configuracionCostoMdCategoriaRepository.findAll()).thenReturn(List.of());
        when(configuracionCostoMdCategoriaRepository.save(any(ConfiguracionCostoMdCategoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminCostoMdConfigRequest request = new AdminCostoMdConfigRequest();
        request.setItems(List.of(
                requestItem("CHICO", "1200.00"),
                requestItem("chico", "1300.00")));

        assertThrows(BadRequestException.class, () -> costoOperativoMdService.actualizarConfiguracion(request));
    }

    @Test
    void getMontoParaCategoriaDevuelveCeroSiEsInvalida() {
        assertEquals(0, BigDecimal.ZERO.compareTo(costoOperativoMdService.getMontoParaCategoria("NO_EXISTE")));
    }

    private ConfiguracionCostoMdCategoria categoria(String categoriaPaquete, BigDecimal monto) {
        ConfiguracionCostoMdCategoria configuracion = new ConfiguracionCostoMdCategoria();
        configuracion.setCategoriaPaquete(categoriaPaquete);
        configuracion.setMonto(monto);
        return configuracion;
    }

    private AdminCostoMdCategoriaRequest requestItem(String categoria, String monto) {
        AdminCostoMdCategoriaRequest item = new AdminCostoMdCategoriaRequest();
        item.setCategoriaPaquete(categoria);
        item.setMonto(new BigDecimal(monto));
        return item;
    }
}
