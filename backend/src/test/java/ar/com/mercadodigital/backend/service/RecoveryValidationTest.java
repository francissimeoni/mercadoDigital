package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import ar.com.mercadodigital.backend.dto.request.RevendedorAjusteMasivoPreciosRequest;
import ar.com.mercadodigital.backend.dto.request.RevendedorPrecioMasivoItemRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontClientePerfilRequest;

class RecoveryValidationTest {
    @Test
    void bulkPricesValidateNestedItemsAndAllowZero() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new RevendedorAjusteMasivoPreciosRequest();
            var item = new RevendedorPrecioMasivoItemRequest();
            item.setProductoId(UUID.randomUUID()); item.setPrecioFinal(new BigDecimal("-0.01"));
            request.setItems(List.of(item));
            assertEquals(1, validator.validate(request).size());
            item.setPrecioFinal(BigDecimal.ZERO);
            assertTrue(validator.validate(request).isEmpty());
            assertEquals(2, validator.getConstraintsForClass(RevendedorPrecioMasivoItemRequest.class).getConstraintsForProperty("precioFinal").getConstraintDescriptors().size());
        }
    }
    @Test
    void recoveredProfileConstraintsAreNotDuplicatedByDecompiler() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var descriptor = factory.getValidator().getConstraintsForClass(StorefrontClientePerfilRequest.class);
            assertEquals(2, descriptor.getConstraintsForProperty("nombre").getConstraintDescriptors().size());
            var request = new StorefrontClientePerfilRequest();
            request.setStorefrontHost("tienda.test"); request.setNombre("Cliente"); request.setTelefonoWhatsapp("123");
            request.setProvincia("Buenos Aires"); request.setLocalidad("La Plata"); request.setCodigoPostal("1900");
            request.setCalle("Calle"); request.setNumero("123");
            assertTrue(factory.getValidator().validate(request).isEmpty());
            request.setNombre("x".repeat(121));
            assertEquals(1, factory.getValidator().validate(request).size());
        }
    }
}
