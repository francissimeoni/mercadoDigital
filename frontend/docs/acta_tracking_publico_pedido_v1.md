# Acta - Tracking publico pedido v1

Fecha: 2026-03-12

## Objetivo

Implementar en frontend la pantalla publica de seguimiento de pedido por token, consumiendo `GET /public/pedido/{trackingToken}` sin mezclar login cliente ni superficies operativas autenticadas.

## Archivos agregados o modificados

Agregados:

- `src/storefront/api/storefrontTrackingApi.ts`
- `src/storefront/hooks/usePedidoTracking.ts`
- `src/storefront/pages/StorefrontPedidoTrackingPage.tsx`
- `frontend/docs/acta_tracking_publico_pedido_v1.md`

Modificados:

- `src/storefront/types/storefrontPedido.ts`
- `src/storefront/pages/StorefrontCheckoutPage.tsx`
- `src/routes/AppRouter.tsx`

## Endpoint consumido

Se consume el endpoint backend:

- `GET /public/pedido/{trackingToken}`

## Ruta publica agregada

Se agrega la ruta publica:

- `/pedido/:trackingToken`

## Datos mostrados al cliente

La pagina renderiza el contrato publico devuelto por backend:

- `pedidoId`
- `estadoPedido`
- `estadoPago`
- `metodoPago`
- `fechaCreacion`
- `demoraEstimadaDias`
- `direccionEnvio`
- `totalProducto`
- `totalEnvio`
- `totalSeguro`
- `totalFinal`
- `items[{ nombreProducto, cantidad, imagenPrincipal }]`
- `envio{ estadoEnvio, trackingLogistica }`

No se recalcula informacion ni se exponen datos internos.

## Manejo de loading, error y success

Loading:

- skeleton publico dentro del layout del storefront

Error:

- `404` -> mensaje orientado a cliente final indicando que no se encontro un pedido publico para ese token
- error de red u otro backend -> mensaje de fallo temporal con opcion de reintento

Success:

- resumen del pedido
- estado comercial/pago
- items comprados
- bloque de envio
- resumen economico

## Integracion con checkout publico

Se cierra una integracion minima y directa:

- cuando `POST /public/pedido/directo` devuelve `trackingToken`, el checkout exitoso lo muestra
- se agrega CTA visible a `/pedido/{trackingToken}`
- tambien se muestra la ruta textual para que el cliente tenga un camino claro aun sin navegacion automatica

## Decisiones UX cerradas

1. Integracion del link de tracking en checkout:
   - CTA explicita en la pantalla de pedido creado, sin redirigir automaticamente

2. Layout visual:
   - usa `PublicStoreLayout` para mantener branding y separacion respecto de `MainLayout`

3. Representacion del `404`:
   - mensaje publico simple, sin detalles tecnicos ni internos

4. Representacion de estados:
   - tarjetas y bloques cortos para pedido, pago y envio, evitando UI de backoffice

5. Dependencia de tenant:
   - solo visual via `PublicStoreLayout`; la consulta de tracking usa unicamente el token

## Impacto sobre la arquitectura frontend actual

- se extiende la capa `src/storefront` con API, hook y pagina propios de tracking
- se mantiene separacion total entre storefront publico y app autenticada
- el router publico gana una nueva ruta sin alterar `MainLayout` ni `ProtectedRoute`

## Pendiente para la siguiente etapa

- retorno post-PSP
- notificacion cliente por mail o WhatsApp
- mejoras de seguimiento post-compra
- login/panel cliente
- RMA publico
