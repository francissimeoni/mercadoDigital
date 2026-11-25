# Acta - Proveedor despachante y asignacion de envio en split v1

Fecha: 2026-05-07

## Objetivo

Permitir definir en el alta de proveedor si despacha mercaderia para que, al crear pedidos nuevos, el costo de envio se asigne automaticamente al proveedor dentro del split logico.

## Cambios realizados

- Migracion nueva:
  - `backend/src/main/resources/db/migration/V30__proveedor_despacha_mercaderia.sql`
  - agrega `proveedores.despacha_mercaderia` (`BIT(1) NOT NULL DEFAULT b'0'`)
- Entidad:
  - `Proveedor` suma `despachaMercaderia`
- Alta admin de usuario/proveedor:
  - `AdminUsuarioCreateRequest` suma campo opcional `proveedorDespachante`
  - `AdminUsuarioService` persiste ese valor al crear el proveedor
- Split de pedido:
  - `PedidoService.buildSplitDraft(...)` ahora:
    - mantiene `monto_envio` como componente explicito
    - si el proveedor tiene `despacha_mercaderia = true`, suma ese envio a `monto_proveedor`

## Regla funcional que queda activa

- Proveedor no despachante (`false`):
  - `monto_proveedor` no incluye envio
  - envio queda imputado fuera del proveedor (linea `monto_envio`)
- Proveedor despachante (`true`):
  - `monto_proveedor` incluye envio
  - `monto_envio` se sigue guardando para trazabilidad economica

## Compatibilidad y convenciones

- Se mantiene Flyway como fuente de verdad.
- Cambio backward compatible con proveedores existentes (`DEFAULT false`).
- No se rompen endpoints existentes; el nuevo campo del request es opcional.

## Nota de alcance

Esta etapa resuelve la imputacion automatica en split logico para pedidos nuevos.
No implementa todavia payout automatico externo por PSP.
