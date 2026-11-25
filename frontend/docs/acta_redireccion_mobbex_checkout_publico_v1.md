# Acta - Redireccion Mobbex checkout publico v1

Fecha: 2026-03-12

## Objetivo

Cerrar la salida real desde el checkout publico hacia Mobbex consumiendo `checkoutUrl` devuelto por backend, sin confirmar pagos desde frontend y manteniendo al webhook como unica verdad del pago.

## Archivos agregados o modificados

Agregado:

- `frontend/docs/acta_redireccion_mobbex_checkout_publico_v1.md`

Modificados:

- `frontend/src/storefront/pages/StorefrontCheckoutPage.tsx`
- `frontend/src/storefront/types/storefrontPedido.ts`

## Endpoint consumido

Se sigue consumiendo:

- `POST /public/pedido/{pedidoId}/pago`

Contrato usado:

- `pedidoId`
- `estadoPedido`
- `estadoPago`
- `metodoPago`
- `monto`
- `referenciaPedido`
- `referenciaPago`
- `checkoutUrl`

## Uso de checkoutUrl

`checkoutUrl` queda tipado en frontend y se usa como disparador real de salida al PSP.

Regla:

- si `checkoutUrl` existe y es una URL `http/https` valida, el checkout programa una redireccion real a Mobbex
- si no existe, la pagina mantiene el comportamiento honesto de mostrar referencias y estado pendiente

## Estrategia UX de redireccion

Se elige una redireccion casi inmediata, con una confirmacion visual breve.

Comportamiento:

- backend prepara el pago
- frontend muestra estado `Redirigiendo al pago seguro`
- se recuerda que el pedido sigue pendiente hasta webhook
- luego se ejecuta `window.location.assign(checkoutUrl)`

Motivo:

- evita una salida abrupta sin contexto
- sigue siendo rapida y consistente con un checkout real
- no agrega un paso manual extra cuando backend ya devolvio URL utilizable

## Manejo de loading, error y reintento

Loading:

- `Preparando pago PSP...` durante la mutacion
- `Redirigiendo al pago seguro` cuando ya existe `checkoutUrl` y la salida fue programada

Error:

- `FeedbackAlert` de error si falla `POST /public/pedido/{pedidoId}/pago`
- no se pierde `pedidoId`
- no se pierde `trackingToken`
- no se pierde el estado persistido del checkout

Reintento:

- si falla la preparacion, el usuario puede volver a ejecutar `Preparar pago PSP`
- si el usuario vuelve desde Mobbex sin pagar, no se dispara una redireccion automatica desde estado restaurado
- en ese caso se muestra CTA para retomar manualmente `Ir al checkout seguro`

## Como se evita doble click o doble preparacion

- el boton de preparacion se deshabilita durante `isPending`
- la UI bloquea nueva preparacion si ya existe `pagoPreparado`
- la UI tambien bloquea acciones mientras `isRedirectingToCheckout = true`

Esto mantiene consistencia con la idempotencia backend y evita dobles clics confusos.

## Relacion con trackingToken

El checkout sigue mostrando el CTA de tracking a:

- `/pedido/{trackingToken}`

Decision:

- el link de tracking sigue visible antes de salir al PSP
- tambien queda disponible cuando el usuario vuelve sin haber pagado

Esto da una salida publica segura para consultar el pedido sin login, aun si el pago todavia sigue pendiente.

## Que pasa si el usuario vuelve sin pagar todavia

- el pedido sigue mostrado como creado
- el pago sigue mostrado como pendiente
- no se infiere pago confirmado
- no hay polling
- se puede reabrir Mobbex con la URL ya preparada
- se puede consultar tracking desde el token ya generado

## Impacto sobre la arquitectura frontend actual

- no se toca la app autenticada
- no se agrega una nueva pagina intermedia
- `StorefrontCheckoutPage` sigue orquestando el flujo publico
- se conserva la separacion entre:
  - creacion del pedido
  - preparacion PSP
  - confirmacion real por webhook

## Que queda pendiente para retorno post-pago

- pantalla o callback de retorno especifico desde Mobbex
- UX mas rica al volver desde PSP
- polling o refresco controlado si se decide en otra etapa
- conciliacion visual automatica con tracking luego del pago confirmado
