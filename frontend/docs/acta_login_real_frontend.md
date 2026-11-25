# Acta - Implementacion Login Real Frontend

Fecha: 2026-03-09

## Objetivo
Reemplazar login mock por autenticacion real con backend y manejo minimo de sesion JWT.

## Endpoint real usado
- `POST /auth/login`

## DTOs reutilizados
- `LoginRequest`:
  - `email`
  - `password`
- `AuthResponse`:
  - `token`
  - `email`
  - `issuedAt`

## Cambios implementados
- Nuevo API client `src/api/authApi.ts`.
- Nuevos tipos `src/types/auth.ts`.
- Nuevo helper de sesion `src/auth/authSession.ts` para:
  - parseo minimo de JWT (`uid`, `roles`, `exp`)
  - persistencia de sesion
  - limpieza de sesion
  - validacion de expiracion
- Implementacion de contexto/hook de auth:
  - `src/auth/AuthContext.tsx`
  - `src/auth/useAuth.ts`
- `LoginPage` conectada a backend real:
  - submit real de credenciales
  - loading/error
  - guardado de sesion
- `ProtectedRoute` ajustada para sesion real.
- `axiosClient` usa token desde sesion valida.
- `MainLayout`:
  - muestra usuario y roles
  - boton salir
  - links visibles segun rol real cuando aplica:
    - `REVENDEDOR`
    - `PROVEEDOR`
    - `LOGISTICA_MD`
    - `ADMIN_MD`

## Compatibilidad local/dev
- Se mantiene compatibilidad con seed local.
- Como `AuthResponse` no devuelve ids operativos de dominio, se aplica resolucion minima y explicita para usuarios demo por email:
  - `revendedor.demo@md.local` -> `revendedorId` demo + `clienteId` demo
  - `proveedor.demo@md.local` -> `proveedorId` demo
- `usuarioId` y `roles` salen del JWT real.

## Restricciones cumplidas
- Sin librerias extra.
- Cambios chicos y compilables.
- Sin inventar contratos de backend.
- Sin romper rutas existentes.
