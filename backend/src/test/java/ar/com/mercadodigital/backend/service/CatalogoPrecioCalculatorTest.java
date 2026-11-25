package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CatalogoPrecioCalculatorTest {
    private final CatalogoPrecioCalculator calculator = new CatalogoPrecioCalculator();
    private BigDecimal n(String value) { return new BigDecimal(value); }

    @ParameterizedTest
    @CsvSource({"ARS,100000,true,0,100000.00", "ARS,100000,false,8000,108000.00",
            "USD,100,true,0,120000.00", "USD,100,false,8000,128000.00"})
    void ejemplosAcordados(String moneda, String precio, boolean incluido, String envio, String esperado) {
        var result = calculator.calcular(moneda, n(precio), incluido, n("1200"), n(envio));
        assertEquals("LISTO", result.estado());
        assertEquals(n(esperado), result.precioPublicoArs());
    }

    @Test void redondeaUnaSolaVezAlFinalHalfUp() {
        var result = calculator.calcular("USD", n("1.01"), false, n("1.005"), n("0.01"));
        assertEquals(n("1.03"), result.precioPublicoArs());
        assertEquals(n("1.01505"), result.baseArs());
    }
    @Test void arsNoDependeDeCotizacion() {
        assertEquals(n("100.00"), calculator.calcular("ARS", n("100"), true, null, null).precioPublicoArs());
    }
    @Test void envioIncluidoIgnoraCategoriaAnterior() {
        assertEquals(n("100.00"), calculator.calcular("ARS", n("100"), true, null, n("8000")).precioPublicoArs());
    }
    @Test void faltantesNoSeConviertenEnPrecioCero() {
        var result = calculator.calcular("USD", n("100"), false, null, null);
        assertEquals("PENDIENTE", result.estado()); assertNull(result.precioPublicoArs());
        assertEquals(2, result.pendientes().size());
    }
    @Test void configuracionSinCompletarNoInfiereEnvio() {
        var result = calculator.calcular(null, null, null, null, null);
        assertEquals("PENDIENTE", result.estado()); assertEquals(3, result.pendientes().size());
    }
    @ParameterizedTest @CsvSource({"0", "-1"})
    void cotizacionInvalida(String quote) {
        assertEquals("PENDIENTE", calculator.calcular("USD", n("100"), true, n(quote), null).estado());
    }
    @Test void permiteEnvioSinCosto() {
        assertEquals(n("100.00"), calculator.calcular("ARS", n("100"), false, null, BigDecimal.ZERO).precioPublicoArs());
    }
    @Test void rechazaDesbordeDelPrecioFinal() {
        var result = calculator.calcular("USD", n("99999999999999999.99"), true, n("1200"), null);
        assertEquals("PENDIENTE", result.estado()); assertNull(result.precioPublicoArs());
    }
}
