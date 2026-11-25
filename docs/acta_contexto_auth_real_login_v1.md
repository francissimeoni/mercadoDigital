# Acta - Contexto Auth Real Login v1

## Objetivo

Eliminar la dependencia de perfiles demo hardcodeados en frontend para resolver `revendedorId`, `proveedorId` y `clienteId` despues del login.

## Decision

- el backend pasa a devolver contexto operativo real en `POST /auth/login`
- el frontend deja de inferir entidades por email demo
- `Revendedor` queda vinculado formalmente con `Usuario`

## Alcance implementado

- migracion `V15__revendedor_usuario_auth_context.sql`
- relacion `revendedores.usuario_id`
- `AuthResponse` ahora expone:
  - `revendedorId`
  - `proveedorId`
  - `clienteId`
- `AuthService` resuelve esas entidades desde repositorios reales
- seed local vincula:
  - usuario revendedor -> revendedor
  - usuario cliente -> relacion `ClienteRevendedor`
  - usuario proveedor -> proveedor
- frontend consume esos ids reales desde la respuesta de login

## Resultado

- el frontend ya no depende de un mapa demo por email
- el contexto operativo queda alineado con el backend como fuente de verdad
- mejora el hardening de auth y reduce un gap importante del entorno demo

## Verificacion

- `./mvnw.cmd test` en backend: OK
- `npm run build` en frontend: OK
