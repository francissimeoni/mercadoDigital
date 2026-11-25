# Acta: Roles, Seguridad JWT y Notificaciones Internas

## 1. Seguridad y roles

Se implemento Spring Security con JWT firmado (HMAC SHA-256) para autenticar requests.

### Endpoints publicos
- `POST /auth/login`
- `POST /webhooks/psp`

### Endpoints protegidos por rol
- `REVENDEDOR`:
  - `GET /catalogo`
  - `/pedido/**`
  - `/revendedores/**`
- `PROVEEDOR`:
  - `/proveedores/**`
- `LOGISTICA_MD`:
  - `/logistica/**`
  - `/envios/**`
- `ADMIN_MD`:
  - `/rma/**`

### Login
- Endpoint: `POST /auth/login`
- Request:
```json
{
  "email": "usuario@md.com",
  "password": "secreto"
}
```
- Response:
```json
{
  "token": "<JWT_DE_EJEMPLO>",
  "email": "usuario@md.com",
  "issuedAt": "2026-03-07T10:00:00"
}
```

### Modelo de usuario
`Usuario` incorpora `passwordHash` y mantiene:
- `email`
- `activo`
- `roles`

## 2. Notificaciones internas

Se agrego la tabla `notificaciones` via migracion Flyway:
- `V7__notificaciones.sql`

Columnas:
- `id`
- `usuario_id`
- `tipo`
- `titulo`
- `mensaje`
- `leida`
- `created_at`
- `updated_at`

Adicionalmente en la misma migracion se agrega `usuarios.password_hash`.

## 3. Servicio y endpoints de notificaciones

### Servicio
`NotificationService` con metodo:
- `createNotification(usuarioId, tipo, titulo, mensaje)`

### Endpoints
- `GET /notificaciones/{usuarioId}`: lista notificaciones por usuario (orden desc por fecha)
- `POST /notificaciones/{id}/leer`: marca notificacion como leida

## 4. Eventos conectados a notificaciones

Se generan notificaciones en:
- `PEDIDO_CREADO`
- `PAGO_CONFIRMADO`
- `ENVIO_LISTO_PARA_RETIRO`
- `ENVIO_DESPACHADO`
- `RMA_SOLICITADO`

## 5. Tests agregados

- `NotificacionServiceTest`
- `SecurityAccessTest`

Cobertura principal:
- creacion/listado/marcado de notificaciones
- acceso publico a `/auth/login` y `/webhooks/psp`
- enforcement de roles en `/catalogo` y `/rma/**`
