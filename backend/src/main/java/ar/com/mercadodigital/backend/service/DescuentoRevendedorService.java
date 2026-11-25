package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Service;

import ar.com.mercadodigital.backend.entity.Revendedor;

@Service
public class DescuentoRevendedorService {

    public DescuentoAplicado resolveDescuento(Revendedor revendedor, String codigoIngresado, BigDecimal markupDisponible) {
        if (revendedor == null || codigoIngresado == null || codigoIngresado.isBlank()) {
            return new DescuentoAplicado(null, BigDecimal.ZERO);
        }

        String codigoConfigurado = normalizarCodigo(revendedor.getCodigoDescuento());
        String codigoNormalizado = normalizarCodigo(codigoIngresado);
        if (codigoConfigurado == null || !codigoConfigurado.equals(codigoNormalizado)) {
            return new DescuentoAplicado(null, BigDecimal.ZERO);
        }

        BigDecimal montoConfigurado = nvl(revendedor.getDescuentoFijo());
        BigDecimal markup = nvl(markupDisponible);
        BigDecimal descuentoAplicado = montoConfigurado.min(markup).max(BigDecimal.ZERO);
        if (descuentoAplicado.signum() <= 0) {
            return new DescuentoAplicado(null, BigDecimal.ZERO);
        }

        return new DescuentoAplicado(codigoConfigurado, descuentoAplicado);
    }

    public String normalizarCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
        return normalizado.isBlank() ? null : normalizado;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public record DescuentoAplicado(
            String codigoAplicado,
            BigDecimal monto) {
    }
}
