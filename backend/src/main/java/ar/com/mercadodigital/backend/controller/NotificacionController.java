package ar.com.mercadodigital.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.response.NotificacionResponse;
import ar.com.mercadodigital.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificationService notificationService;

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<NotificacionResponse>> getByUsuarioId(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(notificationService.getByUsuarioId(usuarioId));
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarComoLeida(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.marcarComoLeida(id));
    }
}
