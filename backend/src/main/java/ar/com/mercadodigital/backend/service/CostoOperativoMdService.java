package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.AdminCostoMdCategoriaRequest;
import ar.com.mercadodigital.backend.dto.request.AdminCostoMdConfigRequest;
import ar.com.mercadodigital.backend.dto.response.AdminCostoMdCategoriaResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCostoMdConfigResponse;
import ar.com.mercadodigital.backend.entity.CategoriaPaquete;
import ar.com.mercadodigital.backend.entity.ConfiguracionCostoMdCategoria;
import ar.com.mercadodigital.backend.entity.TipoCalculoCostoMd;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.ConfiguracionCostoMdCategoriaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CostoOperativoMdService {

    private final ConfiguracionCostoMdCategoriaRepository configuracionCostoMdCategoriaRepository;

    @Transactional(readOnly = true)
    public AdminCostoMdConfigResponse getConfiguracionAdmin() {
        return toConfigResponse(ensureDefaultsExist());
    }

    @Transactional
    public AdminCostoMdConfigResponse actualizarConfiguracion(AdminCostoMdConfigRequest request) {
        List<ConfiguracionCostoMdCategoria> configuraciones = ensureDefaultsExist();
        Map<String, ConfiguracionCostoMdCategoria> byCategoria = configuraciones.stream()
                .collect(Collectors.toMap(ConfiguracionCostoMdCategoria::getCategoriaPaquete, Function.identity()));

        validarDuplicados(request.getItems());
        for (AdminCostoMdCategoriaRequest item : request.getItems()) {
            String categoria = normalizarCategoria(item.getCategoriaPaquete()).name();
            ConfiguracionCostoMdCategoria configuracion = byCategoria.get(categoria);
            if (configuracion == null) {
                configuracion = new ConfiguracionCostoMdCategoria();
                configuracion.setCategoriaPaquete(categoria);
            }
            TipoCalculoCostoMd tipoCalculo = normalizarTipoCalculo(item.getTipoCalculo());
            configuracion.setMonto(normalizarMonto(item.getMonto(), "monto", tipoCalculo));
            configuracion.setTipoCalculo(tipoCalculo);
            byCategoria.put(categoria, configuracionCostoMdCategoriaRepository.save(configuracion));
        }

        return toConfigResponse(byCategoria.values().stream().toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getMontoParaCategoria(String categoriaPaquete) {
        return getMontoParaCategoria(categoriaPaquete, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMontoParaCategoria(String categoriaPaquete, BigDecimal baseCalculo) {
        CategoriaPaquete categoria = tryNormalizarCategoria(categoriaPaquete);
        if (categoria == null) {
            return BigDecimal.ZERO;
        }
        ConfiguracionCostoMdCategoria configuracion = ensureDefaultsExist().stream()
                .filter(item -> categoria.name().equals(item.getCategoriaPaquete()))
                .findFirst()
                .orElse(null);
        if (configuracion == null) {
            return BigDecimal.ZERO;
        }
        return resolverMonto(configuracion, baseCalculo);
    }

    private List<ConfiguracionCostoMdCategoria> ensureDefaultsExist() {
        Map<String, ConfiguracionCostoMdCategoria> existentes = configuracionCostoMdCategoriaRepository.findAll().stream()
                .collect(Collectors.toMap(ConfiguracionCostoMdCategoria::getCategoriaPaquete, Function.identity()));

        for (CategoriaPaquete categoria : EnumSet.allOf(CategoriaPaquete.class)) {
            if (!existentes.containsKey(categoria.name())) {
                ConfiguracionCostoMdCategoria nueva = new ConfiguracionCostoMdCategoria();
                nueva.setCategoriaPaquete(categoria.name());
                nueva.setMonto(BigDecimal.ZERO);
                nueva.setTipoCalculo(TipoCalculoCostoMd.MONTO_FIJO);
                existentes.put(categoria.name(), configuracionCostoMdCategoriaRepository.save(nueva));
            }
        }

        return existentes.values().stream()
                .sorted(java.util.Comparator.comparing(ConfiguracionCostoMdCategoria::getCategoriaPaquete))
                .toList();
    }

    private void validarDuplicados(List<AdminCostoMdCategoriaRequest> items) {
        Map<CategoriaPaquete, Long> conteo = items.stream()
                .map(item -> normalizarCategoria(item.getCategoriaPaquete()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        conteo.forEach((categoria, cantidad) -> {
            if (cantidad > 1) {
                throw new BadRequestException("categoriaPaquete duplicada en la solicitud: " + categoria.name());
            }
        });
    }

    private CategoriaPaquete normalizarCategoria(String valor) {
        CategoriaPaquete categoria = tryNormalizarCategoria(valor);
        if (categoria == null) {
            throw new BadRequestException("categoriaPaquete invalida: " + valor);
        }
        return categoria;
    }

    private CategoriaPaquete tryNormalizarCategoria(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (normalizado.isBlank()) {
            return null;
        }
        try {
            return CategoriaPaquete.valueOf(normalizado);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BigDecimal normalizarMonto(BigDecimal monto, String campo, TipoCalculoCostoMd tipoCalculo) {
        if (monto == null) {
            throw new BadRequestException(campo + " es obligatorio");
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(campo + " no puede ser negativo");
        }
        if (TipoCalculoCostoMd.PORCENTAJE.equals(tipoCalculo) && monto.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException(campo + " no puede superar 100 cuando el tipo de calculo es PORCENTAJE");
        }
        return monto;
    }

    private TipoCalculoCostoMd normalizarTipoCalculo(TipoCalculoCostoMd tipoCalculo) {
        return tipoCalculo != null ? tipoCalculo : TipoCalculoCostoMd.MONTO_FIJO;
    }

    private BigDecimal resolverMonto(ConfiguracionCostoMdCategoria configuracion, BigDecimal baseCalculo) {
        TipoCalculoCostoMd tipo = normalizarTipoCalculo(configuracion.getTipoCalculo());
        if (tipo == TipoCalculoCostoMd.MONTO_FIJO) {
            return configuracion.getMonto();
        }
        BigDecimal base = baseCalculo != null ? baseCalculo.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        return base.multiply(configuracion.getMonto())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private AdminCostoMdConfigResponse toConfigResponse(List<ConfiguracionCostoMdCategoria> configuraciones) {
        return AdminCostoMdConfigResponse.builder()
                .items(configuraciones.stream()
                        .sorted(java.util.Comparator.comparing(ConfiguracionCostoMdCategoria::getCategoriaPaquete))
                        .map(this::toItemResponse)
                        .toList())
                .build();
    }

    private AdminCostoMdCategoriaResponse toItemResponse(ConfiguracionCostoMdCategoria configuracion) {
        return AdminCostoMdCategoriaResponse.builder()
                .id(configuracion.getId())
                .categoriaPaquete(configuracion.getCategoriaPaquete())
                .monto(configuracion.getMonto())
                .tipoCalculo(normalizarTipoCalculo(configuracion.getTipoCalculo()))
                .createdAt(configuracion.getCreatedAt())
                .updatedAt(configuracion.getUpdatedAt())
                .build();
    }
}
