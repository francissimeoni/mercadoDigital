# Acta: RMA v2 reposicion manual trazada v1

Fecha: 2026-04-07

## Criterio operativo

Para esta etapa no se modela un pedido nuevo ni un envio logistico independiente para la garantia con reemplazo.

La reposicion se resuelve dentro del mismo caso RMA, dejando un mini circuito manual pero auditable:

- solicitud al proveedor
- recepcion de la reposicion en MD
- reenvio al cliente con tracking real
- entrega final del reemplazo

## Objetivo

Cubrir la operacion real actual de MD:

- admin negocia la garantia con el proveedor
- admin registra cada hito manualmente
- el cliente puede seguir el reemplazo desde el tracking publico del caso

## Implementacion vigente

### Backend

El caso `RESUELTO` con reposicion habilita endpoints para:

- `POST /rma/{id}/reposicion/solicitar-proveedor`
- `POST /rma/{id}/reposicion/recibir-md`
- `POST /rma/{id}/reposicion/reenviar-cliente`
- `POST /rma/{id}/reposicion/entregar-cliente`

Cada paso deja auditoria y alimenta hitos visibles del caso.

Ademas, la API ahora expone una mini bitacora de reposicion por caso con:

- titulo del hito
- fecha
- observacion
- tracking si aplica
- transportista si aplica

En el reenvio al cliente se exige:

- `transportistaId`
- `tracking`

### Frontend admin

El detalle de RMA ya permite:

- registrar la solicitud al proveedor
- registrar la recepcion en MD
- cargar transportista y tracking del reenvio
- marcar la entrega final

### Tracking publico

El cliente ya puede ver:

- que la reposicion fue aprobada
- cuando MD recibe el reemplazo
- cuando el reemplazo fue reenviado
- el tracking del nuevo envio
- cuando el reemplazo se entrego
- la mini bitacora del reemplazo como bloque separado dentro del seguimiento

## Decision

Se adopta esta estrategia como v1 porque:

- refleja la operacion real actual
- deja trazabilidad suficiente
- evita complejizar temprano el modelo logistico

## Evolucion posible

Si el volumen o la complejidad lo justifican, la siguiente etapa seria modelar la reposicion como envio/logistica independiente, probablemente con entidad propia.

## Verificacion

- Backend:
  - `./mvnw.cmd test "-Dtest=RmaServiceTest,PublicPedidoTrackingServiceTest"`
  - OK
- Frontend:
  - `npm run build`
  - OK
