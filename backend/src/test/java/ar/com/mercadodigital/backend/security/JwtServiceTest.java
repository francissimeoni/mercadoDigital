package ar.com.mercadodigital.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class JwtServiceTest {

    @Test
    void accessYRefreshNoPuedenIntercambiarse() {
        JwtService service = new JwtService(new ObjectMapper(), "un-secreto-de-prueba-suficientemente-largo", 60, 120);
        UUID usuarioId = UUID.randomUUID();

        String access = service.generarToken(usuarioId, "tienda@test.com", List.of("REVENDEDOR"));
        String refresh = service.generarRefreshToken(usuarioId, "tienda@test.com");

        assertEquals(usuarioId, service.validarYParsear(access).usuarioId());
        assertEquals(usuarioId, service.validarRefreshToken(refresh).usuarioId());
        assertThrows(IllegalArgumentException.class, () -> service.validarYParsear(refresh));
        assertThrows(IllegalArgumentException.class, () -> service.validarRefreshToken(access));
    }
}
