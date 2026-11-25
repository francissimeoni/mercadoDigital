# Acta - liberacion de split en RETIRADO v1

Fecha: 2026-05-09

## Decision

La liberacion del split se ejecuta cuando el envio pasa a `RETIRADO`.

No se libera en `PAGO_CONFIRMADO`.

## Regla operativa

Al marcar `RETIRADO`:

- se libera automaticamente la liquidacion del proveedor
- se consolida auditoria de liberacion para todas las partes del split logico:
  - proveedor
  - revendedor
  - MD operativo
  - envio
  - seguro
  - PSP

## Implementacion

- `EnvioService.marcarRetirado(...)` mantiene disparo de liberacion automatica.
- `LiquidacionProveedorService.liberarAutomaticamentePorRetiro(...)`:
  - conserva `PROVEEDOR_LIBERADO_AUTOMATICO`
  - agrega `SPLIT_LIBERADO_PARTES_RETIRADO` con resumen economico consolidado.

## Alcance

Este cierre corresponde a liquidacion interna/auditable del split logico.
No implica aun payout automatico productivo PSP a cuentas externas.
