# Acta - split payout real en RETIRADO y OAuth MD v1

Fecha: 2026-05-09

## Objetivo

Cerrar operativamente el reparto real de fondos al marcar `RETIRADO` y habilitar la configuracion admin de la cuenta Mercado Pago de MD para percibir su costo operativo.

## Cambios implementados

1. Configuracion Mercado Pago MD en admin:
- nuevo bloque en `Admin > Configuracion` para:
  - guardar credenciales Mercado Pago MD
  - definir `payoutEmail` de MD
  - generar OAuth de MD desde el mismo panel
- nuevos endpoints backend:
  - `GET /admin/configuracion/mercadopago-md`
  - `PUT /admin/configuracion/mercadopago-md`
  - `GET /admin/configuracion/mercadopago-md/oauth-link`

2. OAuth Mercado Pago para MD:
- `MercadoPagoOAuthStateService` incorpora subject `MERCADO_DIGITAL`
- callback OAuth redirige a `/admin/configuracion` cuando el subject es MD
- el callback persiste tokens en `configuracion_mercadopago_md`

3. Payout real en `RETIRADO`:
- al liberar split en `RETIRADO`, el backend intenta payout real con Mercado Pago (`/v1/payouts`)
- origen del payout: access token OAuth del revendedor (sin fallback global)
- destinos del payout:
  - proveedores (agrupado por email)
  - MD (`payoutEmail` configurado)
- reglas economicas:
  - proveedor cobra `monto_proveedor`
  - MD cobra `monto_md + monto_seguro + monto_envio` cuando el proveedor no despacha
  - si proveedor despacha, ese envio ya queda imputado en `monto_proveedor`

4. Persistencia de ejecucion:
- nueva tabla `split_payout_ejecuciones` con:
  - estado
  - intentos
  - payoutId/idempotencyKey
  - error/payload/fechas
- nuevo estado expuesto en `GET /envios/{pedidoId}/liquidacion-partes`

## Migraciones

- `V33__split_payout_real_y_config_mercadopago_md.sql`
  - crea `configuracion_mercadopago_md`
  - crea `split_payout_ejecuciones`

## Auditoria

Se consolida:
- `SPLIT_LIBERADO_PARTES_RETIRADO`
- `SPLIT_PAYOUT_REAL_ENVIADO`
- `SPLIT_PAYOUT_REAL_ERROR`

## Nota operativa

Para ejecutar payout real:
- el revendedor debe tener OAuth propio (no fallback global)
- MD debe tener OAuth configurado y `payoutEmail` cargado
