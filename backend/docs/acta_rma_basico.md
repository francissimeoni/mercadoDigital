# Acta: Flujo RMA Basico

## Estados RMA
- `SOLICITADO`
- `APROBADO`
- `EN_TRANSITO_DEVOLUCION`
- `RECIBIDO_EN_DEPOSITO`
- `RESUELTO`
- `RECHAZADO`

## Endpoints
- `POST /rma/solicitar`
  - request:
    - `pedidoItemId`
    - `motivo`
    - `descripcion`

- `POST /rma/{id}/aprobar`

- `POST /rma/{id}/recibir-en-deposito`

- `POST /rma/{id}/resolver`
  - request:
    - `resolucionFinal`
    - `observacionesInspeccion`
    - `responsableProbable`

## Reglas implementadas
- Solo se permite solicitar RMA cuando el `pedido_item` pertenece a pedido con envio:
  - `DESPACHADO` o `ENTREGADO`
- Transiciones:
  - `SOLICITADO -> APROBADO`
  - `APROBADO|EN_TRANSITO_DEVOLUCION -> RECIBIDO_EN_DEPOSITO`
  - `RECIBIDO_EN_DEPOSITO -> RESUELTO|RECHAZADO` (segun `resolucionFinal`)
- Si la transicion no corresponde al estado actual:
  - `409 Conflict`

## Auditoria por transicion
- `RMA_SOLICITADO`
- `RMA_APROBADO`
- `RMA_RECIBIDO_EN_DEPOSITO`
- `RMA_RESUELTO`
- `RMA_RECHAZADO`

## Validaciones
- `pedido_item` inexistente: `404 Not Found`
- falta `motivo` o `resolucionFinal`: `400 Bad Request`
- transicion invalida: `409 Conflict`

## Persistencia adicional
- Se agregaron en `rmas`:
  - `descripcion`
  - `observaciones_inspeccion`
  - `resolucion_final`
