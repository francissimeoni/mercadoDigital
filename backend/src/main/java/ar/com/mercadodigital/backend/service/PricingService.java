package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PspConfigService pspConfigService;

    public PricingDetalle calcularPrecioItem(BigDecimal costoProveedor,
            BigDecimal costoMd,
            BigDecimal markupPorcentaje,
            BigDecimal markupFijo,
            BigDecimal costoEnvio,
            boolean tieneSeguro) {
        return calcularPrecio(costoProveedor, costoMd, markupPorcentaje, markupFijo, costoEnvio, tieneSeguro);
    }

    public PricingDetalle calcularCostoOperativoItem(BigDecimal costoProveedor,
            BigDecimal costoMd,
            BigDecimal costoEnvio,
            boolean tieneSeguro) {
        return calcularPrecio(costoProveedor, costoMd, null, null, costoEnvio, tieneSeguro);
    }

    private PricingDetalle calcularPrecio(BigDecimal costoProveedor,
            BigDecimal costoMd,
            BigDecimal markupPorcentaje,
            BigDecimal markupFijo,
            BigDecimal costoEnvio,
            boolean tieneSeguro) {
        BigDecimal costo = nvl(costoProveedor);
        BigDecimal costoMdNormalizado = nvl(costoMd);
        BigDecimal pspFee = nvl(pspConfigService.getPspFeeFactor());
        BigDecimal seguroTasa = nvl(pspConfigService.getSeguroTransporteFactor());

        if (pspFee.compareTo(BigDecimal.ZERO) < 0 || pspFee.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("mercado-digital.psp-fee debe estar entre 0 (incl) y 1 (excl)");
        }

        BigDecimal costoBaseComercial = costo.add(costoMdNormalizado);
        BigDecimal margenRevendedor = calcularMargen(costoBaseComercial, markupPorcentaje, markupFijo);
        BigDecimal montoSeguro = tieneSeguro ? costo.multiply(seguroTasa) : BigDecimal.ZERO;
        BigDecimal subtotalSinPsp = costoBaseComercial.add(margenRevendedor);

        BigDecimal divisor = BigDecimal.ONE.subtract(pspFee);
        BigDecimal calculo = subtotalSinPsp.divide(divisor, 10, RoundingMode.HALF_UP);
        BigDecimal precioItemFinal = BigDecimal.valueOf(Math.round(calculo.doubleValue()));
        BigDecimal costoPsp = precioItemFinal.subtract(subtotalSinPsp);

        return new PricingDetalle(precioItemFinal, montoSeguro, costoPsp, margenRevendedor, subtotalSinPsp);
    }

    private BigDecimal calcularMargen(BigDecimal costo, BigDecimal markupPorcentaje, BigDecimal markupFijo) {
        if (markupPorcentaje != null && markupPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
            return costo.multiply(markupPorcentaje);
        }
        if (markupFijo != null && markupFijo.compareTo(BigDecimal.ZERO) > 0) {
            return markupFijo;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record PricingDetalle(
            BigDecimal precioItemFinal,
            BigDecimal montoSeguro,
            BigDecimal costoPsp,
            BigDecimal margenRevendedor,
            BigDecimal subtotalSinPsp) {
    }
}
