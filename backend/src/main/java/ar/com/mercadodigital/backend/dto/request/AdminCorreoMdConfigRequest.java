package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCorreoMdConfigRequest {

    @Email
    @NotBlank
    @Size(max = 180)
    private String fromEmail;

    @NotBlank
    @Size(max = 180)
    private String fromName;

    @Email
    @Size(max = 180)
    private String replyTo;

    @Size(max = 180)
    private String smtpHost;

    private Integer smtpPort;

    @Size(max = 180)
    private String smtpUsername;

    @Size(max = 255)
    private String smtpPassword;

    private Boolean smtpAuth;

    private Boolean smtpStartTls;
}
