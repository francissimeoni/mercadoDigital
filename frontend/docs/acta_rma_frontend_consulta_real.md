# Acta - RMA Frontend con Consulta Real

Fecha: 2026-03-10

## Objetivo
Actualizar el modulo RMA del frontend para dejar de depender de snapshots en sesion y pasar a lectura real persistente desde backend.

## Endpoints usados
- `GET /rma`
- `GET /rma/{id}`
- `POST /rma/solicitar`
- `POST /rma/{id}/aprobar`
- `POST /rma/{id}/recibir-en-deposito`
- `POST /rma/{id}/resolver`

## Cambios implementados

### API
Se actualizo `src/api/rmaApi.ts` con:
- `getRmas(params?)`
- `getRmaDetalle(rmaId)`

### Hooks
Se actualizo `src/hooks/useRma.ts` para:
- usar queries reales en lista y detalle
- invalidar/refrescar lista y detalle despues de solicitar, aprobar, recibir en deposito y resolver
- mantener `RmaResponse` como contrato unico

### Paginas
`RmaPage`:
- reemplaza la bandeja basada en sesion por listado real desde backend
- mantiene la solicitud de RMA y apertura por ID

`RmaDetallePage`:
- carga detalle real con `GET /rma/{id}`
- mantiene acciones reales existentes
- luego de cada accion refresca el estado real

## Restricciones cumplidas
- Sin librerias nuevas
- Sin inventar campos fuera de `RmaResponse`
- Sin duplicar logica backend
- Mantiene loading, error y vacio con estilo operativo actual

## Validacion
- `npm.cmd run build`
