# Acta - Endpoints de Consulta RMA Backend

Fecha: 2026-03-10

## Objetivo
Completar el contrato backend del modulo RMA agregando endpoints de consulta sin romper las transiciones ya implementadas.

## Endpoints agregados

### `GET /rma`
Devuelve lista de RMAs visibles para el usuario actual.

Soporta filtro opcional:
- `pedidoId`

Visibilidad actual:
- `ADMIN_MD`: ve todos los RMAs
- otros roles: quedan limitados a RMAs creados por su usuario

### `GET /rma/{id}`
Devuelve detalle completo del RMA visible para el usuario actual.

## Contrato reutilizado
Se reutiliza `RmaResponse` tanto para listado como para detalle.

## Cambios implementados
- `RmaController` ahora expone lectura de lista y detalle.
- `RmaService` incorpora consultas visibles por rol y por usuario actual.
- `RMARepository` agrega consultas con `EntityGraph` para evitar problemas de carga lazy en lectura.
- Se soporta filtro opcional por `pedidoId` en la lista.

## Tests agregados/ajustados
- `RmaControllerTest`
  - lista devuelve 200
  - detalle devuelve 200
- `RmaServiceTest`
  - admin puede listar RMAs
  - usuario no admin puede consultar detalle visible propio

## Restricciones cumplidas
- Cambios chicos
- Endpoints existentes no modificados
- Tests backend en verde

## Validacion
- `./mvnw -q test`
