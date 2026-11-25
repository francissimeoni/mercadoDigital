package ar.com.mercadodigital.backend.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.NotificacionResponse;
import ar.com.mercadodigital.backend.entity.Notificacion;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.NotificacionRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public NotificacionResponse createNotification(UUID usuarioId, String tipo, String titulo, String mensaje) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
        return createNotificationForUser(usuario, tipo, titulo, mensaje);
    }

    @Transactional
    public NotificacionResponse createNotificationForUser(Usuario usuario, String tipo, String titulo, String mensaje) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResourceNotFoundException("Usuario no encontrado para crear notificacion");
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setLeida(false);
        Notificacion saved = notificacionRepository.save(notificacion);
        return toResponse(saved);
    }

    @Transactional
    public void createNotificationsForUsers(Collection<Usuario> usuarios, String tipo, String titulo, String mensaje) {
        Set<UUID> usuariosNotificados = new LinkedHashSet<>();
        for (Usuario usuario : usuarios) {
            if (usuario == null || usuario.getId() == null || !usuariosNotificados.add(usuario.getId())) {
                continue;
            }
            createNotificationForUser(usuario, tipo, titulo, mensaje);
        }
    }

    @Transactional
    public void createNotificationsForRole(String rolNombre, String tipo, String titulo, String mensaje) {
        createNotificationsForUsers(usuarioRepository.findActivosByRolNombre(rolNombre), tipo, titulo, mensaje);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> getByUsuarioId(UUID usuarioId) {
        return notificacionRepository.findByUsuario_IdOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificacionResponse marcarComoLeida(UUID id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada: " + id));
        notificacion.setLeida(true);
        Notificacion saved = notificacionRepository.save(notificacion);
        return toResponse(saved);
    }

    private NotificacionResponse toResponse(Notificacion notificacion) {
        return NotificacionResponse.builder()
                .id(notificacion.getId())
                .usuarioId(notificacion.getUsuario().getId())
                .tipo(notificacion.getTipo())
                .titulo(notificacion.getTitulo())
                .mensaje(notificacion.getMensaje())
                .leida(notificacion.isLeida())
                .createdAt(notificacion.getCreatedAt())
                .build();
    }
}
