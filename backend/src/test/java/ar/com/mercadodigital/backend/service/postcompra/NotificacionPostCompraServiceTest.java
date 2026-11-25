package ar.com.mercadodigital.backend.service.postcompra;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionPostCompraServiceTest {

    @Mock
    private EmailSender emailSender;
    @Mock
    private BrandingPostCompraResolver brandingResolver;
    @Mock
    private PlantillaEmailPostCompraService plantillaService;
    @Mock
    private AuditoriaEventoRepository auditoriaEventoRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificacionPostCompraService service;

    private BrandingPostCompra branding;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        branding = new BrandingPostCompra(
                "Mi Tienda",
                "#0f172a",
                null,
                "https://mi-tienda.com",
                "ventas@mi-tienda.com",
                "no-reply@mi-tienda.com",
                "Mi Tienda",
                "smtp.mi-tienda.com",
                587,
                "mailer@mi-tienda.com",
                "secret",
                true,
                true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
    }

    @Test
    void enviaEmailCuandoLlegaEventoPedidoCreado() {
        EventoPedidoCreado evento = new EventoPedidoCreado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-1",
                "CREADO",
                new BigDecimal("1200.00"),
                "ARS",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarPedidoCreado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarPedidoCreado(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoPagoConfirmado() {
        EventoPagoConfirmado evento = new EventoPagoConfirmado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-2",
                "PAGADO",
                new BigDecimal("1500.00"),
                "ARS",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarPagoConfirmado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarPagoConfirmado(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoEnvioDespachado() {
        EventoEnvioDespachado evento = new EventoEnvioDespachado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-3",
                "PAGADO",
                "DESPACHADO",
                "TRK-123",
                "Andreani",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarEnvioDespachado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarEnvioDespachado(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoEnvioEntregado() {
        EventoEnvioEntregado evento = new EventoEnvioEntregado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-4",
                "PAGADO",
                "ENTREGADO",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarEnvioEntregado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarEnvioEntregado(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoRmaSolicitado() {
        EventoRmaSolicitado evento = new EventoRmaSolicitado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-1",
                "SOLICITADO",
                "Robot Smart",
                "No enciende",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarRmaSolicitado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarRmaSolicitado(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoRmaEnvioSolicitado() {
        EventoRmaEnvioSolicitado evento = new EventoRmaEnvioSolicitado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-2",
                "EN_TRANSITO_DEVOLUCION",
                "Robot Smart",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarRmaEnvioSolicitado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarRmaEnvioSolicitado(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoRmaCerrado() {
        EventoRmaCerrado evento = new EventoRmaCerrado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-3",
                "REEMBOLSADO",
                "Robot Smart",
                "REEMBOLSADO",
                new BigDecimal("1500.00"),
                "TRX-001",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarRmaCerrado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarRmaCerrado(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoRmaReposicionReenviada() {
        EventoRmaReposicionReenviada evento = new EventoRmaReposicionReenviada(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-4",
                "RESUELTO",
                "Robot Smart",
                "REP-123",
                "Andreani",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarRmaReposicionReenviada(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarRmaReposicionReenviada(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void enviaEmailCuandoLlegaEventoRmaReposicionEntregada() {
        EventoRmaReposicionEntregada evento = new EventoRmaReposicionEntregada(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-rma-5",
                "RESUELTO",
                "Robot Smart",
                "REP-123",
                "Andreani",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarRmaReposicionEntregada(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));

        service.notificarRmaReposicionEntregada(evento);

        verify(emailSender).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void falloDeEmailNoRompeYRegistraAuditoria() {
        EventoPedidoCreado evento = new EventoPedidoCreado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                "tracking-1",
                "CREADO",
                new BigDecimal("1200.00"),
                "ARS",
                LocalDateTime.now());

        when(brandingResolver.resolver(evento.revendedorId())).thenReturn(branding);
        when(plantillaService.renderizarPedidoCreado(any(), any()))
                .thenReturn(new ContenidoEmail("asunto", "<p>html</p>", "texto"));
        doThrow(new IllegalStateException("fallo sender")).when(emailSender).send(any(MensajeEmail.class));

        service.notificarPedidoCreado(evento);

        verify(auditoriaEventoRepository).save(any());
    }

    @Test
    void omiteEnvioSiFaltaTracking() {
        EventoPedidoCreado evento = new EventoPedidoCreado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@test.com",
                "Cliente Demo",
                null,
                "CREADO",
                new BigDecimal("1200.00"),
                "ARS",
                LocalDateTime.now());

        service.notificarPedidoCreado(evento);

        verify(emailSender, never()).send(any(MensajeEmail.class));
        verify(auditoriaEventoRepository).save(any());
    }
}
