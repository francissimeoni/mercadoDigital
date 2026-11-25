# Acta hardening tracking multistorefront host v1

## Fecha
- 2026-04-07

## Objetivo
- Cerrar el riesgo de aislamiento entre storefronts en el tracking publico por token.

## Hallazgo validado
- Un `trackingToken` valido podia abrirse desde otro storefront si se reutilizaba la ruta `/pedido/:trackingToken`.
- El backend resolvia el pedido solo por token y el frontend aplicaba branding segun el host actual.
- Resultado: exposicion cruzada de datos operativos/comerciales con identidad visual ajena.

## Cambio aplicado
- `GET /public/pedido/{trackingToken}` ahora exige `storefrontHost`.
- Backend valida que el host recibido coincida con el `dominio` del revendedor asociado al pedido.
- Si el host no coincide, responde `404`.
- Frontend envia siempre el `tenant.host` activo al consultar tracking publico y retorno post-pago.

## Resultado esperado
- El tracking solo se resuelve bajo el storefront correcto.
- Un enlace abierto desde otro tenant ya no expone el pedido.
- El mail sigue apuntando al dominio del storefront correcto y ahora el backend tambien lo hace cumplir.

## Verificacion
- Backend: `./mvnw.cmd test` OK, 178 tests.
- Frontend: `npm run build` OK.
