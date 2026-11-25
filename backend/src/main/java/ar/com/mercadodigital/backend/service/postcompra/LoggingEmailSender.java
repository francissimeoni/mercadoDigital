package ar.com.mercadodigital.backend.service.postcompra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.post-compra-email", name = "transport", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(MensajeEmail message) {
        log.info("Email transaccional preparado. to={}, subject={}, metadata={}",
                message.getTo(),
                message.getSubject(),
                message.getMetadata());
    }
}
