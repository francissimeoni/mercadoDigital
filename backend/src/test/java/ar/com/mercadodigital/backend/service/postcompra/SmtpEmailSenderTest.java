package ar.com.mercadodigital.backend.service.postcompra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private SmtpEmailSender smtpEmailSender;

    @Test
    void enviaMimeMessageConRemitenteYReplyTo() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(System.getProperties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        MensajeEmail message = MensajeEmail.builder()
                .to("cliente@test.com")
                .subject("Asunto demo")
                .htmlBody("<p>hola</p>")
                .textBody("hola")
                .fromName("Mi Tienda")
                .fromEmail("ventas@mitienda.com")
                .replyTo("ayuda@mitienda.com")
                .metadata(Map.of("pedidoId", "123"))
                .build();

        smtpEmailSender.send(message);

        verify(javaMailSender).send(mimeMessage);
        assertEquals("Asunto demo", mimeMessage.getSubject());
        assertEquals("cliente@test.com", ((InternetAddress) mimeMessage.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        assertEquals("ventas@mitienda.com", ((InternetAddress) mimeMessage.getFrom()[0]).getAddress());
        assertEquals("Mi Tienda", ((InternetAddress) mimeMessage.getFrom()[0]).getPersonal());

        Address[] replyTo = mimeMessage.getReplyTo();
        assertEquals("ayuda@mitienda.com", ((InternetAddress) replyTo[0]).getAddress());
    }
}
