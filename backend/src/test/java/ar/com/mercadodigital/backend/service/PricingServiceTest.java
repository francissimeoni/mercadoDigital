package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PricingServiceTest {

    @Test
    void calcularSinSeguroConPsp() {
        PricingService service = buildService("0.08", "0.02");

        PricingService.PricingDetalle detalle = service.calcularPrecioItem(
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("20000"),
                new BigDecimal("10000"),
                false);

        assertEquals(0, new BigDecimal("130435").compareTo(detalle.precioItemFinal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(detalle.montoSeguro()));
        assertEquals(0, new BigDecimal("10435").compareTo(detalle.costoPsp()));
    }

    @Test
    void calcularConSeguroConPsp() {
        PricingService service = buildService("0.08", "0.02");

        PricingService.PricingDetalle detalle = service.calcularPrecioItem(
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("20000"),
                new BigDecimal("10000"),
                true);

        assertEquals(0, new BigDecimal("130435").compareTo(detalle.precioItemFinal()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(detalle.montoSeguro()));
        assertEquals(0, new BigDecimal("10435.00").compareTo(detalle.costoPsp()));
    }

    @Test
    void calcularConPspCero() {
        PricingService service = buildService("0.00", "0.02");

        PricingService.PricingDetalle detalle = service.calcularPrecioItem(
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);

        assertEquals(0, new BigDecimal("1000").compareTo(detalle.precioItemFinal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(detalle.costoPsp()));
    }

    @Test
    void redondeosConMathRound() {
        PricingService service = buildService("0.00", "0.02");

        PricingService.PricingDetalle casoA = service.calcularPrecioItem(
                new BigDecimal("1000.2"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);
        PricingService.PricingDetalle casoB = service.calcularPrecioItem(
                new BigDecimal("1000.5"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);
        PricingService.PricingDetalle casoC = service.calcularPrecioItem(
                new BigDecimal("1000.9"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);

        assertEquals(0, new BigDecimal("1000").compareTo(casoA.precioItemFinal()));
        assertEquals(0, new BigDecimal("1001").compareTo(casoB.precioItemFinal()));
        assertEquals(0, new BigDecimal("1001").compareTo(casoC.precioItemFinal()));
    }

    @Test
    void calcularCostoOperativoNoAplicaMarkupPeroSiPsp() {
        PricingService service = buildService("0.08", "0.02");

        PricingService.PricingDetalle detalle = service.calcularCostoOperativoItem(
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("5000"),
                false);

        assertEquals(0, new BigDecimal("108696").compareTo(detalle.precioItemFinal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(detalle.margenRevendedor()));
        assertEquals(0, new BigDecimal("8696").compareTo(detalle.costoPsp()));
    }

    @Test
    void calcularConCostoMdAumentaBaseComercial() {
        PricingService service = buildService("0.08", "0.02");

        PricingService.PricingDetalle detalle = service.calcularPrecioItem(
                new BigDecimal("1000"),
                new BigDecimal("150"),
                new BigDecimal("0.10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);

        assertEquals(0, new BigDecimal("1265").compareTo(detalle.subtotalSinPsp()));
        assertEquals(0, new BigDecimal("1375").compareTo(detalle.precioItemFinal()));
        assertEquals(0, new BigDecimal("110").compareTo(detalle.costoPsp()));
    }

    private PricingService buildService(String pspFee, String seguroTransporte) {
        PspConfigService pspConfigService = Mockito.mock(PspConfigService.class);
        when(pspConfigService.getPspFeeFactor()).thenReturn(new BigDecimal(pspFee));
        when(pspConfigService.getSeguroTransporteFactor()).thenReturn(new BigDecimal(seguroTransporte));
        return new PricingService(pspConfigService);
    }
}
