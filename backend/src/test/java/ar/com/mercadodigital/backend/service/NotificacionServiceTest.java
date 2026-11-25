package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.NotificacionResponse;
import ar.com.mercadodigital.backend.entity.Notificacion;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.repository.NotificacionRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createNotificationGuardaNotificacion() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocation -> {
            Notificacion n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        NotificacionResponse response = notificationService.createNotification(
                usuarioId, "PEDIDO_CREADO", "Pedido creado", "Se creo el pedido");

        assertEquals(usuarioId, response.getUsuarioId());
        assertEquals("PEDIDO_CREADO", response.getTipo());
        assertTrue(!response.isLeida());
    }

    @Test
    void marcarComoLeidaActualizaEstado() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Notificacion notificacion = new Notificacion();
        notificacion.setId(id);
        notificacion.setUsuario(usuario);
        notificacion.setTipo("PAGO_CONFIRMADO");
        notificacion.setTitulo("Pago confirmado");
        notificacion.setMensaje("ok");
        notificacion.setLeida(false);

        when(notificacionRepository.findById(id)).thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificacionResponse response = notificationService.marcarComoLeida(id);
        assertTrue(response.isLeida());
    }

    @Test
    void getByUsuarioIdDevuelveLista() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Notificacion n = new Notificacion();
        n.setId(UUID.randomUUID());
        n.setUsuario(usuario);
        n.setTipo("ENVIO_DESPACHADO");
        n.setTitulo("Envio");
        n.setMensaje("Despachado");
        n.setLeida(false);

        when(notificacionRepository.findByUsuario_IdOrderByCreatedAtDesc(usuarioId)).thenReturn(List.of(n));

        List<NotificacionResponse> response = notificationService.getByUsuarioId(usuarioId);
        assertEquals(1, response.size());
        assertEquals("ENVIO_DESPACHADO", response.get(0).getTipo());
    }
}
