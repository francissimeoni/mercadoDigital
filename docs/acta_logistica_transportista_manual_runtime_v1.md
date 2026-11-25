# Acta: logistica transportista manual runtime v1

Fecha: 2026-04-07

## Objetivo

Destrabar la operacion cuando la lista de transportistas no aparece cargada en runtime y evitar que RMA/logistica queden bloqueados por ese punto.

## Decision aplicada

El sistema pasa a aceptar dos caminos validos:

1. usar `transportistaId` desde la lista existente
2. enviar `transportistaNombre` manual y crear o reutilizar el transportista automaticamente

## Implementacion

- `DespacharEnvioRequest` y `ActualizarReposicionManualRequest` ahora aceptan `transportistaNombre`.
- `TransportistaService` suma `resolveOrCreate(...)`.
- `EnvioService` y `RmaService` resuelven o crean el transportista al operar.
- frontend de logistica y RMA deja de depender por completo de que la query de transportistas cargue bien:
  - si la lista falla, se muestra aviso claro
  - se puede seguir escribiendo nombre manual
  - el backend lo registra al despachar o reenviar

## Verificacion

- Backend:
  - `./mvnw.cmd test "-Dtest=RmaServiceTest,EnvioServiceTest,NotificacionPostCompraServiceTest,PlantillaEmailPostCompraServiceTest,PublicPedidoTrackingServiceTest"`
  - OK
- Frontend:
  - `npm.cmd run build`
  - OK

## Estado resultante

El combo de transportistas deja de ser un bloqueo operativo duro:

- si hay lista, se usa normal
- si no hay lista, la operacion sigue igual
- el transportista queda persistido para reutilizacion futura
