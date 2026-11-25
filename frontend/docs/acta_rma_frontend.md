# Acta - Implementacion RMA Frontend

Fecha: 2026-03-09

## Objetivo
Implementar modulo RMA en frontend usando endpoints reales del backend, con flujo basico operativo.

## Endpoints reales usados
- `POST /rma/solicitar`
- `POST /rma/{id}/aprobar`
- `POST /rma/{id}/recibir-en-deposito`
- `POST /rma/{id}/resolver`

## DTOs reutilizados
- `SolicitarRmaRequest`
- `ResolverRmaRequest`
- `RmaResponse`

## Cambios implementados
- Nuevo API client `src/api/rmaApi.ts`.
- Nuevos tipos `src/types/rma.ts`.
- Nuevos hooks TanStack Query `src/hooks/useRma.ts`:
  - `useRmas()`
  - `useRmaDetalle(rmaId)`
  - `useSolicitarRma()`
  - `useAprobarRma()`
  - `useRecibirRmaEnDeposito()`
  - `useResolverRma()`
- Nueva pagina `src/pages/rma/RmaPage.tsx`:
  - solicitud de RMA
  - apertura de detalle por `rmaId`
  - bandeja de RMAs operados en sesion
  - loading/error/vacio
- Nueva pagina `src/pages/rma/RmaDetallePage.tsx`:
  - visualizacion de detalle disponible
  - acciones de aprobar, recibir y resolver
  - loading/error + reintento
- Rutas protegidas agregadas:
  - `/rma`
  - `/rma/:rmaId`
- Acceso agregado en layout.

## Nota de contrato backend
El backend actual no expone endpoint GET para listado o detalle de RMA.
Por eso, el frontend muestra una bandeja de RMAs operados en sesion y actualiza detalle con las respuestas de acciones reales.

## Restricciones cumplidas
- Sin duplicar logica de negocio.
- Sin inventar estados operativos.
- Estados y transiciones dependen del backend.
- Tailwind simple y cambios chicos.
