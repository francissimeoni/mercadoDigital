# Acta - Implementacion Notificaciones Frontend

Fecha: 2026-03-09

## Objetivo
Implementar modulo de notificaciones del revendedor en frontend usando endpoints reales del backend.

## Endpoints reales usados
- `GET /notificaciones/{usuarioId}` para listar.
- `POST /notificaciones/{id}/leer` para marcar como leida.

## DTO reutilizado
- `NotificacionResponse`:
  - `id`
  - `usuarioId`
  - `tipo`
  - `titulo`
  - `mensaje`
  - `leida`
  - `createdAt`

## Cambios implementados
- Nuevo API client:
  - `src/api/notificacionApi.ts`
  - `getNotificaciones(usuarioId)`
  - `marcarNotificacionComoLeida(id)`
- Nuevo hook:
  - `src/hooks/useNotificaciones.ts`
  - `useNotificaciones(usuarioId)`
  - `useMarcarNotificacionLeida(usuarioId)`
- Nueva tipificacion:
  - `src/types/notificacion.ts`
- Reemplazo de `NotificacionesPage` placeholder por pagina funcional:
  - lista real de notificaciones
  - tipo, titulo/mensaje, fecha
  - estado visual leida/no leida
  - accion "Marcar como leida"
  - accion contextual para pedidos
    - si se detecta `pedidoId` en contenido: navega a `/pedidos/:pedidoId`
    - si no: navega a `/pedidos` para notificaciones de tipo pedido
  - estados loading/error/vacio

## Restricciones cumplidas
- Sin duplicar logica del backend.
- Sin inventar endpoints.
- Sin exponer datos sensibles.
- Tailwind simple y cambios chicos.

## Validacion
- Build frontend ejecutado con `npm.cmd run build`.
