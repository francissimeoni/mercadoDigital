package ar.com.mercadodigital.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "configuracion_correo_md")
public class ConfiguracionCorreoMd extends BaseEntity {

    @Column(name = "clave", nullable = false, unique = true, length = 50)
    private String clave;

    @Column(name = "from_email", nullable = false, length = 180)
    private String fromEmail;

    @Column(name = "from_name", nullable = false, length = 180)
    private String fromName;

    @Column(name = "reply_to", length = 180)
    private String replyTo;

    @Column(name = "smtp_host", length = 180)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 180)
    private String smtpUsername;

    @Column(name = "smtp_password", length = 255)
    private String smtpPassword;

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth = true;

    @Column(name = "smtp_starttls", nullable = false)
    private boolean smtpStartTls = true;
}
