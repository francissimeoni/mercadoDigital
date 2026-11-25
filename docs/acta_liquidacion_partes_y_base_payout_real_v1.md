# Acta - liquidacion por partes y base payout real v1

Fecha: 2026-05-09

## Objetivo

Destrabar pruebas de reparto mostrando liquidacion consolidada por partes al `RETIRADO` y dejar preparada la base tecnica para payout real PSP futuro.

## Implementacion

### 1) Liquidacion por partes (fase pruebas)

- nuevo endpoint:
  - `GET /envios/{pedidoId}/liquidacion-partes`
- respuesta incluye:
  - estado de liberacion
  - fecha de liberacion
  - totales: proveedor, revendedor, md, envio, seguro, psp
  - total de componentes

### 2) Base tecnica payout real (fase futura)

- nueva interfaz:
  - `SplitPayoutGateway`
- implementacion inicial:
  - `SplitPayoutGatewayNoop`
- al liberar en `RETIRADO`:
  - se invoca el gateway
  - se audita:
    - `SPLIT_PAYOUT_REAL_PENDIENTE` (base lista / no-op)
    - `SPLIT_PAYOUT_REAL_ERROR` (si falla futura implementacion)

## Regla vigente

- la liberacion del split sigue en `RETIRADO`
- no se mueve a `PAGO_CONFIRMADO`

## Alcance

Esta etapa mejora trazabilidad y visibilidad de reparto interno.
El payout real productivo en PSP entre cuentas externas queda para etapa posterior.
