# Acta - Retorno post-pago Mobbex v1

Fecha: 2026-03-12

## Objetivo

Implementar en frontend el manejo del retorno post-pago desde Mobbex, sin confirmar pagos localmente y usando el tracking publico como superficie real de conciliacion visual basica.

## Archivos agregados o modificados

Agregados:

- `frontend/src/storefront/pages/StorefrontPagoRetornoPage.tsx`
- `frontend/src/storefront/utils/storefrontCheckoutState.ts`
- `frontend/docs/acta_retorno_post_pago_mobbex_v1.md`

Modificados:

- `frontend/src/storefront/pages/StorefrontCheckoutPage.tsx`
- `frontend/src/routes/AppRouter.tsx`

## Estrategia de retorno elegida

Se eligio una ruta publica especifica:

- `/pago/retorno`

Motivo:

- evita sobrecargar `StorefrontCheckoutPage`
- desacopla la vuelta desde Mobbex del formulario de compra
- permite una UX dedicada de verificacion sin mezclar creacion/preparacion del pedido

La deteccion del retorno no depende de parametros inventados de Mobbex.

Se usa:

- `trackingToken` persistido en `sessionStorage`
- el endpoint real `GET /public/pedido/{trackingToken}`

## Integracion con trackingToken

El estado persistido del checkout paso a centralizarse en una utilidad compartida.

Se persiste:

- `pedidoCreado`
- `pagoPreparado`
- `updatedAt`

En el retorno, frontend:

1. busca el checkout publico mas reciente con `trackingToken`
2. consulta tracking publico real
3. muestra estado honesto
4. deriva al usuario al seguimiento completo

## Manejo de estados pendiente y confirmado

Pendiente:

- mensaje: “Estamos verificando tu pago”
- se aclara que el PSP puede demorar en acreditar
- se recuerda que webhook es la unica verdad
- se mantiene CTA a `/pedido/{trackingToken}`

Confirmado:

- mensaje de estado actualizado
- se programa una derivacion suave a `/pedido/{trackingToken}`
- no se confirma nada localmente: solo se reacciona a lo que tracking devolvio

## Refresh automatico acotado

Se implemento una conciliacion automatica muy corta:

- 1 consulta inicial a tracking
- si sigue `PENDIENTE`, 1 refresh adicional luego de un delay corto

No hay:

- loops largos
- polling sostenido
- inferencia local de pago confirmado

## Inspeccion de returnUrl y parametros

La integracion backend ya usa `mobbex.return-url`, pero en esta etapa frontend no depende de query params de retorno para funcionar.

Decision:

- no se asumen parametros especificos de Mobbex si no estan garantizados
- la continuidad se resuelve con estado persistido local + tracking publico real

Para usar esta pantalla, la `returnUrl` de Mobbex debe apuntar al storefront en:

- `/pago/retorno`

## Impacto sobre la arquitectura frontend actual

- se agrega una pantalla publica nueva dentro de `src/storefront`
- se reutiliza la capa de tracking publico ya existente
- no se toca la app operativa autenticada
- se mantiene la separacion entre:
  - checkout publico
  - retorno PSP
  - tracking publico

## Que queda pendiente para etapas posteriores

- enriquecer la deteccion del retorno si el proveedor documenta parametros estables utiles
- callbacks o mensajes mas especificos segun resultado devuelto por PSP
- notificacion al cliente por mail o WhatsApp con link de tracking
- mejoras de post-compra y panel cliente
