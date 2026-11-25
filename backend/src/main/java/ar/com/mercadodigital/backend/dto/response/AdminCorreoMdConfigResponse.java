package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCorreoMdConfigResponse {
    private UUID id;
    private String fromEmail;
    private String fromName;
    private String replyTo;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private boolean smtpConfigured;
    private boolean smtpAuth;
    private boolean smtpStartTls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
