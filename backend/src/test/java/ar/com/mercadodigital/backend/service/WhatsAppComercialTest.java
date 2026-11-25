package ar.com.mercadodigital.backend.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ar.com.mercadodigital.backend.exception.BadRequestException;

class WhatsAppComercialTest {
    @ParameterizedTest @CsvSource({"3482670015,AR,true,+5493482670015", "0351 15 5123456,AR,true,+5493515123456",
            "+54 11 2345 6789,AR,true,+541123456789", "612345678,ES,false,+34612345678", "2025550123,US,false,+12025550123"})
    void interpretaPaisExplicitoYConfirmacionMovil(String input, String pais, boolean movil, String expected) {
        assertEquals(expected, WhatsAppComercial.normalizar(input, pais, movil));
    }
    @Test void paisNoPermiteNumeroIncompletoNiTextoNiExtension() {
        for (String n : new String[]{"1234567", "tel:3482670015", "3482670015 ext 1", "++5493482670015"})
            assertThrows(BadRequestException.class, () -> WhatsAppComercial.normalizar(n,"AR",true));
        assertThrows(BadRequestException.class, () -> WhatsAppComercial.normalizar("3482670015","ZZ",true));
    }
    @ParameterizedTest @CsvSource({"+54 9 11 1234-5678,+5491112345678", "0054 9 351 1234567,+5493511234567",
            "011 15 1234 5678,+5491112345678", "0351 15 1234567,+5493511234567", "+34 612 345 678,+34612345678",
            "54 11 1234 5678,+541112345678"})
    void normalizaSinInventarDigitos(String input, String expected) {
        assertEquals(expected, WhatsAppComercial.normalizar(input));
        assertEquals(expected, WhatsAppComercial.normalizar(WhatsAppComercial.normalizar(input)));
    }
    @ParameterizedTest @ValueSource(strings = {"123", "1112345678", "+54 9 11 123", "+54 9 11 1234567", "tel:5491112345678", "++5491112345678", "+0012345678"})
    void rechazaFormatoInvalidoOAmbiguo(String input) { assertThrows(BadRequestException.class, () -> WhatsAppComercial.normalizar(input)); }
    @Test void permitePendienteSinFabricarContacto() { assertNull(WhatsAppComercial.normalizar(null)); assertNull(WhatsAppComercial.normalizar(" ")); }
}
