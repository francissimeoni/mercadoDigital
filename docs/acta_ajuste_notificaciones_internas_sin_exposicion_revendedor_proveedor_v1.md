# Acta - ajuste notificaciones internas sin exposicion revendedor-proveedor v1

Fecha: 2026-04-07

## Motivo

Durante la validacion multi-storefront y operativa se detecto una desalineacion con la especificacion funcional:

- el proveedor no debe quedar expuesto al revendedor si no es estrictamente necesario
- las notificaciones internas deben respetar ese principio

## Regla reafirmada

- proveedor y revendedor no se exponen entre si
- las notificaciones internas se entienden como disparos administrados por MD/Admin
- cada hito relevante debe dejar auditoria visible para admin

## Matriz operativa ajustada

- `PEDIDO_CREADO`
  - in-app: proveedor, revendedor
  - auditoria admin: si
- `PAGO_CONFIRMADO`
  - in-app: revendedor
  - auditoria admin: si
- `ENVIO_LISTO_PARA_RETIRO`
  - in-app: revendedor, logistica
  - auditoria admin: si
- `ENVIO_RETIRADO`
  - in-app: revendedor
  - auditoria admin: si
- `ENVIO_DESPACHADO`
  - in-app: revendedor
  - auditoria admin: si
- `ENVIO_ENTREGADO`
  - in-app: revendedor
  - auditoria admin: si

## Implementacion aplicada

- se elimino la notificacion interna a proveedor en `PAGO_CONFIRMADO`
- se agrego notificacion interna a `LOGISTICA_MD` en `ENVIO_LISTO_PARA_RETIRO`
- se mantuvieron notificaciones al revendedor en hitos comerciales y logisticos relevantes
- se agregaron eventos de auditoria admin para cada bloque de notificaciones internas disparadas

## Resultado

La operacion queda mas alineada con la especificacion de negocio:

- no se refuerza un canal directo proveedor ↔ revendedor
- admin conserva trazabilidad de los disparos internos
- backend sigue como fuente de verdad del evento
