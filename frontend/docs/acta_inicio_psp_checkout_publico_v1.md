# Acta - Inicio PSP checkout publico v1

Fecha: 2026-03-12

## Objetivo

Implementar en frontend la etapa MT11 para iniciar la preparacion concreta del checkout PSP desde el flujo de checkout publico directo ya existente, consumiendo `POST /public/pedido/{pedidoId}/pago` sin simular confirmacion de pago ni romper la verdad operativa basada en webhook.

## Archivos agregados o modificados

Agregados:

- `src/storefront/api/storefrontPagoApi.ts`
- `src/storefront/hooks/usePrepararPagoPsp.ts`
- `frontend/docs/acta_inicio_psp_checkout_publico_v1.md`

Modificados:

- `src/storefront/types/storefrontPedido.ts`
- `src/storefront/pages/StorefrontCheckoutPage.tsx`

## Endpoint consumido

Se consume el endpoint backend:

- `POST /public/pedido/{pedidoId}/pago`

## Estrategia UX elegida

Se eligio preparacion PSP por accion explicita del usuario luego de crear el pedido.

Motivo:

- mantiene separadas las etapas de crear pedido y preparar pago
- hace visible el estado intermedio real del flujo
- evita disparos duplicados o confusos mientras la integracion final del PSP todavia no expone URL o SDK desde backend
- respeta la idempotencia del endpoint sin dar una sensacion falsa de pago automatico

Persistencia elegida:

- `sessionStorage` por checkout (`revendedorId + productoCanonicoId`) para rehidratar pedido creado y referencias PSP ante refresh

Motivo:

- hoy no existe una superficie publica de detalle de pedido para reconstruir el estado desde backend sin mezclar login o tracking
- evita perder `referenciaPedido` y `referenciaPago` si el usuario refresca la ruta

## Manejo de loading, error y success

### Creacion del pedido

- loading: boton `Creando pedido...`
- error: `FeedbackAlert` con mensaje backend/local
- success: resumen del pedido creado con contrato real de `POST /public/pedido/directo`

### Preparacion del pago PSP

- loading: boton `Preparando pago PSP...`
- error: `FeedbackAlert` independiente del paso de creacion
- success: bloque informativo con:
  - `pedidoId`
  - `estadoPedido`
  - `estadoPago`
  - `metodoPago`
  - `monto`
  - `referenciaPedido`
  - `referenciaPago`

Preparacion futura:

- si en una etapa posterior backend devuelve `checkoutUrl`, la pagina ya deja preparado el punto de redireccion real sin cambiar el contrato tipado actual

## Representacion del estado pendiente de pago

La pagina muestra explicitamente:

- pedido creado
- pago pendiente
- preparacion PSP pendiente / en curso / lista

No se muestra el pedido como pagado.
No se simula confirmacion.
No se recalculan montos.

## Que parte ya quedo conectada

Quedo conectado:

- consumo real de `POST /public/pedido/{pedidoId}/pago`
- continuidad desde pedido creado hacia preparacion PSP
- visualizacion del contrato real devuelto por backend
- bloqueo visual de reinicio confuso una vez obtenidas las referencias
- rehidratacion liviana del estado del checkout tras refresh

## Que parte sigue pendiente

Sigue pendiente para etapas posteriores:

- URL concreta de redireccion o SDK final del PSP, si backend/proveedor lo exponen
- retorno post-redireccion
- conciliacion visual post-pago
- tracking por token
- panel/login de cliente final

## Impacto sobre la arquitectura frontend actual

- se agrega una capa especifica de pago publico bajo `src/storefront`
- `StorefrontCheckoutPage` sigue siendo la pagina orquestadora del flujo publico
- no se toca la app operativa autenticada
- se mantiene la separacion entre creacion del pedido, preparacion PSP y confirmacion real por webhook
