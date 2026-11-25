# Acta - Implementacion Logistica Frontend

Fecha: 2026-03-09

## Objetivo
Implementar bandeja operativa de Logistica MD en frontend con flujo real de retiros, despachos y entregas.

## Endpoints reales usados
- `GET /logistica/retiros-pendientes`
- `GET /logistica/despachos-pendientes`
- `POST /envios/{pedidoId}/retirado`
- `POST /envios/{pedidoId}/despachar`
- `POST /envios/{pedidoId}/entregar`
- `GET /envios/{pedidoId}`
- `GET /envios/{pedidoId}/rotulo` (PDF)

## DTOs reutilizados
- `PedidoLogisticaResponse`
- `EnvioResponse`
- `DespacharEnvioRequest`

## Cambios implementados
- Nuevo API client `src/api/logisticaApi.ts`.
- Nuevos hooks TanStack Query `src/hooks/useLogistica.ts`:
  - `useRetirosPendientes()`
  - `useDespachosPendientes()`
  - hooks de mutacion para `retirado`, `despachar`, `entregar`
  - `useEnvioLogistica(pedidoId)` para tracking/estado actual
  - `useDescargarRotulo()`
- Nueva pagina `src/pages/logistica/LogisticaEnviosPage.tsx` con:
  - bandeja de retiros pendientes
  - bandeja de despachos pendientes
  - operacion por pedido seleccionado
  - tracking visible si existe
  - descarga de rotulo PDF
  - estados loading/error/vacio
- Nuevos componentes:
  - `src/components/logistica/LogisticaRetiroItem.tsx`
  - `src/components/logistica/LogisticaDespachoItem.tsx`
- Ruta protegida agregada:
  - `/logistica/envios`
- Acceso agregado en layout.

## Restricciones cumplidas
- Sin duplicar logica de negocio backend.
- Sin inventar estados operativos.
- Secuencia logistica respetada por estados retornados de backend.
- Tailwind simple y cambios chicos.

## Validacion
- Build frontend ejecutado con `npm.cmd run build`.
