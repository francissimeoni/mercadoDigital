# Acta - fix checkout persistido post pago v1

Fecha: 2026-04-07

## Situacion detectada

En storefront publico, luego de completar un pedido y volver desde la pantalla de retorno del PSP, el navegador podia conservar en `sessionStorage` un checkout persistido asociado al mismo `revendedorId` y `productoCanonicoId`.

Efecto observado:

- al volver a abrir el checkout del mismo producto, la UI podia rehidratar un pedido ya creado y pagado
- el usuario quedaba atado a un flujo viejo en lugar de iniciar una compra nueva
- en escenario multi-storefront, aunque ya estaba aislado por `revendedorId`, seguia existiendo riesgo de reusar estado viejo dentro del tenant correcto

## Ajuste aplicado

Se agrego limpieza dirigida del checkout persistido cuando el retorno post-pago confirma por tracking publico que el pedido ya no esta `PENDIENTE`.

Implementacion:

- nuevo helper `clearCheckoutStateByTrackingToken(...)` para remover cualquier snapshot del tenant actual cuyo `pedidoCreado.trackingToken` coincida
- uso de ese helper en `StorefrontPagoRetornoPage` antes de redirigir a `/pedido/:trackingToken`

Archivos:

- `frontend/src/storefront/utils/storefrontCheckoutState.ts`
- `frontend/src/storefront/pages/StorefrontPagoRetornoPage.tsx`

## Resultado esperado

- una vez conciliado el pago por backend/webhook, el navegador deja de conservar ese checkout como reanudable
- al volver a entrar al checkout del mismo producto, el cliente inicia una compra nueva
- el tracking sigue siendo la unica fuente de verdad para revisar el pedido ya confirmado

## Verificacion tecnica

- `npm run build` en frontend: OK

## Impacto en roadmap

Este ajuste reduce uno de los dos pendientes funcionales inmediatos que habian quedado abiertos tras la validacion multi-storefront:

- checkout persistido: corregido
- layout roto de logistica: pendiente
