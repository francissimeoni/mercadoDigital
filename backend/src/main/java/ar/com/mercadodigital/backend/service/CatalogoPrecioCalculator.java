package ar.com.mercadodigital.backend.service;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;
import ar.com.mercadodigital.backend.dto.CatalogoAdminDto.Precio;

@Component
public class CatalogoPrecioCalculator {
    public Precio calcular(String moneda, BigDecimal precio, Boolean incluyeEnvio, BigDecimal cotizacion, BigDecimal costoEnvio) {
        List<String> pendientes = new ArrayList<>();
        if (!Set.of("ARS", "USD").contains(moneda == null ? "" : moneda)) pendientes.add("Elegí ARS o USD.");
        if (precio == null || precio.signum() <= 0) pendientes.add("El precio original debe ser mayor que cero.");
        if (incluyeEnvio == null) pendientes.add("Indicá si el precio ya incluye envío.");
        if ("USD".equals(moneda) && (cotizacion == null || cotizacion.signum() <= 0)) pendientes.add("Configurá la cotización del dólar de esta tienda.");
        if (Boolean.FALSE.equals(incluyeEnvio) && (costoEnvio == null || costoEnvio.signum() < 0)) pendientes.add("Elegí una categoría de envío válida.");
        if (!pendientes.isEmpty()) return new Precio("PENDIENTE", List.copyOf(pendientes), moneda, precio, "USD".equals(moneda) ? cotizacion : null, null, null, null);
        BigDecimal base = "USD".equals(moneda) ? precio.multiply(cotizacion) : precio;
        BigDecimal envio = Boolean.TRUE.equals(incluyeEnvio) ? BigDecimal.ZERO : costoEnvio;
        BigDecimal total = base.add(envio).setScale(2, RoundingMode.HALF_UP);
        if (total.signum() <= 0) return new Precio("PENDIENTE", List.of("El precio final redondeado debe ser mayor que cero."), moneda, precio, cotizacion, null, null, null);
        if (total.precision() - total.scale() > 17) return new Precio("PENDIENTE", List.of("El precio final supera el importe admitido."), moneda, precio, cotizacion, null, null, null);
        return new Precio("LISTO", List.of(), moneda, precio, "USD".equals(moneda) ? cotizacion : null, base, envio, total);
    }
}
