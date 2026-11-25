package ar.com.mercadodigital.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.config.PostCompraEmailProperties;
import ar.com.mercadodigital.backend.dto.request.AdminCorreoMdConfigRequest;
import ar.com.mercadodigital.backend.dto.request.AdminCorreoMdTestRequest;
import ar.com.mercadodigital.backend.dto.response.AdminCorreoMdConfigResponse;
import ar.com.mercadodigital.backend.entity.ConfiguracionCorreoMd;
import ar.com.mercadodigital.backend.repository.ConfiguracionCorreoMdRepository;
import ar.com.mercadodigital.backend.service.postcompra.EmailSender;
import ar.com.mercadodigital.backend.service.postcompra.MensajeEmail;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorreoMdConfigService {

    private static final String CLAVE_GLOBAL = "GLOBAL";

    private final ConfiguracionCorreoMdRepository configuracionCorreoMdRepository;
    private final PostCompraEmailProperties properties;
    private final EmailSender emailSender;

    @Transactional(readOnly = true)
    public AdminCorreoMdConfigResponse getConfiguracionAdmin() {
        return toResponse(ensureConfig());
    }

    @Transactional
    public AdminCorreoMdConfigResponse actualizarConfiguracion(AdminCorreoMdConfigRequest request) {
        ConfiguracionCorreoMd config = ensureConfig();
        config.setFromEmail(request.getFromEmail().trim());
        config.setFromName(request.getFromName().trim());
        config.setReplyTo(trimToNull(request.getReplyTo()));
        config.setSmtpHost(trimToNull(request.getSmtpHost()));
        config.setSmtpPort(request.getSmtpPort());
        config.setSmtpUsername(trimToNull(request.getSmtpUsername()));
        if (StringUtils.hasText(request.getSmtpPassword())) {
            config.setSmtpPassword(request.getSmtpPassword());
        }
        config.setSmtpAuth(Boolean.TRUE.equals(request.getSmtpAuth()));
        config.setSmtpStartTls(Boolean.TRUE.equals(request.getSmtpStartTls()));
        return toResponse(configuracionCorreoMdRepository.save(config));
    }

    @Transactional(readOnly = true)
    public ConfiguracionCorreoMd getConfigRuntime() {
        return ensureConfig();
    }

    @Transactional(readOnly = true)
    public void enviarTest(AdminCorreoMdTestRequest request) {
        ConfiguracionCorreoMd config = ensureConfig();
        emailSender.send(MensajeEmail.builder()
                .to(request.getToEmail().trim())
                .subject("Test correo MD - Mercado Digital")
                .textBody("Este es un correo de prueba de la configuracion interna MD.")
                .htmlBody(null)
                .fromEmail(config.getFromEmail())
                .fromName(config.getFromName())
                .replyTo(StringUtils.hasText(config.getReplyTo()) ? config.getReplyTo() : config.getFromEmail())
                .smtpHost(config.getSmtpHost())
                .smtpPort(config.getSmtpPort())
                .smtpUsername(config.getSmtpUsername())
                .smtpPassword(config.getSmtpPassword())
                .smtpAuth(config.isSmtpAuth())
                .smtpStartTls(config.isSmtpStartTls())
                .metadata(java.util.Map.of("tipoNotificacion", "correo-md-test"))
                .build());
    }

    private ConfiguracionCorreoMd ensureConfig() {
        return configuracionCorreoMdRepository.findByClave(CLAVE_GLOBAL)
                .orElseGet(() -> {
                    ConfiguracionCorreoMd nueva = new ConfiguracionCorreoMd();
                    nueva.setClave(CLAVE_GLOBAL);
                    nueva.setFromEmail(properties.getFromEmail());
                    nueva.setFromName(properties.getFromName());
                    nueva.setReplyTo(properties.getFromEmail());
                    nueva.setSmtpHost(null);
                    nueva.setSmtpPort(null);
                    nueva.setSmtpUsername(null);
                    nueva.setSmtpPassword(null);
                    nueva.setSmtpAuth(true);
                    nueva.setSmtpStartTls(true);
                    return configuracionCorreoMdRepository.save(nueva);
                });
    }

    private AdminCorreoMdConfigResponse toResponse(ConfiguracionCorreoMd config) {
        return AdminCorreoMdConfigResponse.builder()
                .id(config.getId())
                .fromEmail(config.getFromEmail())
                .fromName(config.getFromName())
                .replyTo(config.getReplyTo())
                .smtpHost(config.getSmtpHost())
                .smtpPort(config.getSmtpPort())
                .smtpUsername(config.getSmtpUsername())
                .smtpConfigured(StringUtils.hasText(config.getSmtpHost()) && StringUtils.hasText(config.getSmtpUsername()))
                .smtpAuth(config.isSmtpAuth())
                .smtpStartTls(config.isSmtpStartTls())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
