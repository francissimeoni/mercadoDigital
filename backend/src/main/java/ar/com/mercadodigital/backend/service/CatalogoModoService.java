package ar.com.mercadodigital.backend.service;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;

/** Lista explícita por tienda. Vacía por defecto; no depende del navegador ni de datos legados. */
@Component
public class CatalogoModoService {
    private final Set<UUID> revendedores;

    public CatalogoModoService(@Value("${catalogo.publico.revendedores:}") String habilitados) {
        revendedores = Arrays.stream(habilitados.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean activo(UUID revendedorId) {
        return revendedorId != null && revendedores.contains(revendedorId);
    }

    public void rechazarNuevaCompra(UUID revendedorId) {
        if (activo(revendedorId)) {
            throw new OperacionEnConflictoException(
                    "Esta tienda recibe consultas por WhatsApp. No admite nuevas compras dentro de la plataforma.");
        }
    }

    public void rechazarPreciosAnteriores(UUID revendedorId) {
        if (activo(revendedorId)) {
            throw new OperacionEnConflictoException(
                    "Esta tienda usa precios de catálogo. Configurá moneda, cotización y envío en Catálogo; el ajuste anterior no está disponible.");
        }
    }

    public void rechazarImportacionAnterior(UUID revendedorId) {
        if (activo(revendedorId)) {
            throw new OperacionEnConflictoException(
                    "El importador anterior no admite la configuración de catálogo. Usá la carga unificada de productos del catálogo.");
        }
    }
}
