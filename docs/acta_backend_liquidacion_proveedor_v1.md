# Acta - Backend liquidacion proveedor v1

Fecha: 2026-04-06

## Objetivo

Implementar la base backend para la liberacion economica al proveedor segun la decision funcional ya tomada:

- liberacion automatica al pasar envio a `RETIRADO`
- liberacion manual excepcional por logistica MD con motivo obligatorio
- trazabilidad auditable

## Cambios realizados

- nuevo enum `LiquidacionProveedorEstado`
- nuevas columnas en `splits_pedido_item`:
  - `estado_liquidacion_proveedor`
  - `liberado_proveedor_at`
  - `motivo_liberacion_manual`
- nuevo service `LiquidacionProveedorService`
- integracion automatica desde `EnvioService.marcarRetirado(...)`
- nuevos endpoints en `EnvioController`:
  - `GET /envios/{pedidoId}/liquidacion-proveedor`
  - `POST /envios/{pedidoId}/liberar-proveedor`

## Criterio operativo que queda implementado

- mientras el pedido no fue liberado, el split queda en `PENDIENTE`
- cuando logistica MD marca `RETIRADO`, el proveedor pasa a `LIBERADO_AUTOMATICO`
- si hay una excepcion operativa, logistica puede liberar manualmente con motivo y queda `LIBERADO_MANUAL`
- toda liberacion genera auditoria

## Alcance de esta etapa

Esta etapa deja lista la base de estado y auditoria.

No implementa todavia:

- payout real automatizado
- conciliacion financiera externa
- UI final para operar la liberacion manual
- reglas contables completas de cancelacion/RMA posteriores a la liberacion

## Verificacion

Se valido:

- `./mvnw -q test` en verde

## Conclusion

La liberacion economica al proveedor ya no depende solo de una decision documental. El backend ahora tiene una base real, auditable y acoplada al hito logistico `RETIRADO`, con escape manual justificado para excepciones.
