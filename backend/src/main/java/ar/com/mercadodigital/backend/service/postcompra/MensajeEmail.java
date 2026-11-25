package ar.com.mercadodigital.backend.service.postcompra;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MensajeEmail {

    private final String to;
    private final String subject;
    private final String htmlBody;
    private final String textBody;
    private final String fromName;
    private final String fromEmail;
    private final String replyTo;
    private final String smtpHost;
    private final Integer smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final Boolean smtpAuth;
    private final Boolean smtpStartTls;
    private final Map<String, String> metadata;
}
