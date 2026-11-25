package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.entity.CategoriaPaquete;
import ar.com.mercadodigital.backend.repository.TarifaTransportistaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CostoEnvioService {

    public static final int DEMORA_DEFAULT = 5;

    private final TarifaTransportistaRepository tarifaTransportistaRepository;

    @Transactional(readOnly = true)
    public TarifaEnvio resolveTarifa(String categoriaPaquete) {
        String categoriaNormalizada = normalizarCategoria(categoriaPaquete);
        if (categoriaNormalizada == null) {
            return new TarifaEnvio(null, BigDecimal.ZERO, DEMORA_DEFAULT);
        }

        return tarifaTransportistaRepository.findFirstByCategoriaPaqueteOrderByDemoraEstimadaDiasAsc(categoriaNormalizada)
                .map(tarifa -> new TarifaEnvio(
                        categoriaNormalizada,
                        tarifa.getPrecio() != null ? tarifa.getPrecio() : BigDecimal.ZERO,
                        tarifa.getDemoraEstimadaDias() != null ? tarifa.getDemoraEstimadaDias() : DEMORA_DEFAULT))
                .orElse(new TarifaEnvio(categoriaNormalizada, BigDecimal.ZERO, DEMORA_DEFAULT));
    }

    public BigDecimal getMontoParaCategoria(String categoriaPaquete) {
        return resolveTarifa(categoriaPaquete).monto();
    }

    public String resolverCategoriaDominante(String categoriaActual, String categoriaNueva) {
        CategoriaPaquete actual = parseCategoria(categoriaActual);
        CategoriaPaquete nueva = parseCategoria(categoriaNueva);

        if (actual == null) {
            return nueva != null ? nueva.name() : null;
        }
        if (nueva == null) {
            return actual.name();
        }
        return nueva.ordinal() > actual.ordinal() ? nueva.name() : actual.name();
    }

    private String normalizarCategoria(String categoriaPaquete) {
        CategoriaPaquete categoria = parseCategoria(categoriaPaquete);
        return categoria != null ? categoria.name() : null;
    }

    private CategoriaPaquete parseCategoria(String categoriaPaquete) {
        if (categoriaPaquete == null || categoriaPaquete.isBlank()) {
            return null;
        }
        try {
            return CategoriaPaquete.valueOf(categoriaPaquete.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record TarifaEnvio(
            String categoriaPaquete,
            BigDecimal monto,
            Integer demoraEstimadaDias) {
    }
}
