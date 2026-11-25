package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.config.MercadoDigitalProperties;
import ar.com.mercadodigital.backend.dto.request.AdminPspConfigRequest;
import ar.com.mercadodigital.backend.dto.response.AdminPspConfigResponse;
import ar.com.mercadodigital.backend.entity.ConfiguracionPsp;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.ConfiguracionPspRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PspConfigService {

    private static final String CLAVE_GLOBAL = "GLOBAL";

    private final ConfiguracionPspRepository configuracionPspRepository;
    private final MercadoDigitalProperties properties;

    @Transactional(readOnly = true)
    public AdminPspConfigResponse getConfiguracionAdmin() {
        return toResponse(ensureConfig());
    }

    @Transactional
    public AdminPspConfigResponse actualizarConfiguracion(AdminPspConfigRequest request) {
        ConfiguracionPsp config = ensureConfig();
        config.setPspFeePorcentaje(normalizarPorcentaje(request.getPspFeePorcentaje()));
        config.setSeguroTransportePorcentaje(normalizarPorcentaje(request.getSeguroTransportePorcentaje()));
        return toResponse(configuracionPspRepository.save(config));
    }

    @Transactional(readOnly = true)
    public BigDecimal getPspFeeFactor() {
        ConfiguracionPsp config = ensureConfig();
        return config.getPspFeePorcentaje()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getSeguroTransporteFactor() {
        ConfiguracionPsp config = ensureConfig();
        return config.getSeguroTransportePorcentaje()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    private ConfiguracionPsp ensureConfig() {
        return configuracionPspRepository.findByClave(CLAVE_GLOBAL)
                .orElseGet(() -> {
                    ConfiguracionPsp nueva = new ConfiguracionPsp();
                    nueva.setClave(CLAVE_GLOBAL);
                    BigDecimal fallbackFactor = nvl(properties.getPspFee());
                    BigDecimal fallbackPorcentaje = fallbackFactor.multiply(BigDecimal.valueOf(100));
                    nueva.setPspFeePorcentaje(normalizarPorcentaje(fallbackPorcentaje));
                    BigDecimal seguroFallbackFactor = nvl(properties.getSeguroTransporte());
                    BigDecimal seguroFallbackPorcentaje = seguroFallbackFactor.multiply(BigDecimal.valueOf(100));
                    nueva.setSeguroTransportePorcentaje(normalizarPorcentaje(seguroFallbackPorcentaje));
                    return configuracionPspRepository.save(nueva);
                });
    }

    private BigDecimal normalizarPorcentaje(BigDecimal porcentaje) {
        BigDecimal value = nvl(porcentaje);
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("pspFeePorcentaje debe estar entre 0 y 100");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private AdminPspConfigResponse toResponse(ConfiguracionPsp config) {
        return AdminPspConfigResponse.builder()
                .id(config.getId())
                .pspFeePorcentaje(config.getPspFeePorcentaje())
                .seguroTransportePorcentaje(config.getSeguroTransportePorcentaje())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
