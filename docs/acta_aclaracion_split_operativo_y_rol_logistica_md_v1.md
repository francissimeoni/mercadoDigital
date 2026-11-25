# Acta - Aclaracion split operativo y rol LOGISTICA_MD v1

Fecha: 2026-05-09

## Motivo

Durante la validacion funcional surgio una duda recurrente:

- si `LOGISTICA_MD` debe tener OAuth propio para cobrar `costo operativo MD`
- y como se reparte exactamente el bloque economico del pedido

## Decision

Se confirma el criterio vigente:

1. `LOGISTICA_MD` es rol operativo (estado y ejecucion logistica), no rol merchant PSP.
2. No corresponde OAuth Mercado Pago de `LOGISTICA_MD` en el esquema actual.
3. El cobro PSP sigue atado al merchant comercial (revendedor) mientras no exista split/payout real PSP productivo.
4. La verdad economica interna se conserva en split logico por item.

## Semantica consolidada del split logico

Por item:

- `monto_proveedor`: costo proveedor (mas envio cuando el proveedor despacha mercaderia)
- `monto_md`: costo operativo MD
- `monto_revendedor`: margen revendedor
- `monto_envio`: componente logistico trazable
- `monto_seguro`: componente economico de MD
- `monto_psp`: costo PSP

## Alcance tecnico real hoy

- checkout y webhook Mercado Pago resuelven token por revendedor (con fallback global si aplica)
- existe soporte de cuenta Mercado Pago por proveedor para evolucion futura
- no existe flujo de cobro PSP por usuario de `LOGISTICA_MD`

## Proximo paso recomendado

Si negocio decide automatizar reparto real en PSP:

1. definir waterfall legal/fiscal final,
2. definir receptores de payout por tipo de componente (`proveedor`, `md`, `revendedor`),
3. recien ahi implementar split/payout real productivo.
