package ar.com.mercadodigital.backend.service.postcompra;

import ar.com.mercadodigital.backend.service.postcompra.EmailSender;
import ar.com.mercadodigital.backend.service.postcompra.MensajeEmail;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.post-compra-email", name = "transport", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(MensajeEmail message) {
        JavaMailSender sender = resolveSender(message);
        MimeMessage mimeMessage = sender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(
                    StringUtils.hasText(message.getTextBody()) ? message.getTextBody() : "",
                    StringUtils.hasText(message.getHtmlBody()) ? message.getHtmlBody() : "");

            String fromEmail = StringUtils.hasText(message.getFromEmail()) ? message.getFromEmail().trim() : null;
            String fromName = StringUtils.hasText(message.getFromName()) ? message.getFromName().trim() : null;

            if (fromEmail != null) {
                if (fromName != null) {
                    helper.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));
                } else {
                    helper.setFrom(fromEmail);
                }
            }

            if (StringUtils.hasText(message.getReplyTo())) {
                helper.setReplyTo(message.getReplyTo().trim());
            }

            sender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo enviar email SMTP a " + message.getTo(), ex);
        }
    }

    private JavaMailSender resolveSender(MensajeEmail message) {
        if (!StringUtils.hasText(message.getSmtpHost())) {
            return this.javaMailSender;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(message.getSmtpHost().trim());
        if (message.getSmtpPort() != null) {
            sender.setPort(message.getSmtpPort());
        }
        sender.setUsername(StringUtils.hasText(message.getSmtpUsername()) ? message.getSmtpUsername().trim() : null);
        sender.setPassword(StringUtils.hasText(message.getSmtpPassword()) ? message.getSmtpPassword() : null);
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(Boolean.TRUE.equals(message.getSmtpAuth())));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(Boolean.TRUE.equals(message.getSmtpStartTls())));
        properties.put("mail.smtp.starttls.required", "false");
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "5000");
        properties.put("mail.smtp.writetimeout", "5000");
        boolean sslImplicito = message.getSmtpPort() != null && message.getSmtpPort() == 465;
        properties.put("mail.smtp.ssl.enable", Boolean.toString(sslImplicito));
        if (sslImplicito && StringUtils.hasText(message.getSmtpHost())) {
            properties.put("mail.smtp.ssl.trust", message.getSmtpHost().trim());
        }
        return sender;
    }
}
