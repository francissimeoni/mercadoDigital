# Acta - Migracion PSP a Mercado Pago Checkout Pro v1

Fecha: 2026-04-03

## Objetivo

Reemplazar la integracion trabada de Mobbex por Mercado Pago Checkout Pro para retomar validaciones reales del flujo publico sin cambiar las reglas de negocio centrales:

- frontend solo redirige
- webhook sigue siendo la unica verdad
- pedido queda `CREADO` hasta webhook
- pago queda `PENDIENTE` hasta webhook

## Decision tomada

Se migra el PSP del storefront publico a Mercado Pago Checkout Pro.

Se mantiene el contrato publico actual:

- `POST /public/pedido/{id}/pago`
- respuesta con `checkoutUrl`
- tracking publico sin confirmacion local

## Cambios realizados

- se reemplazo configuracion `mobbex.*` por `mercadopago.*`
- se agrego cliente backend para:
  - crear preferencia Checkout Pro
  - consultar pago real por `paymentId`
- se reemplazo el adapter de webhook por uno especifico de Mercado Pago
- se agrego endpoint publico:
  - `POST /webhooks/psp/mercadopago`
- el webhook de Mercado Pago ahora:
  - recibe notificacion
  - identifica `paymentId`
  - consulta el pago real en Mercado Pago
  - adapta al contrato interno `WebhookPspRequest`
  - confirma solo si `status = approved`
- no se fuerza `auto_return=approved` en la preferencia:
  - con `localhost` Mercado Pago devolvia error de validacion
  - se mantienen `back_urls` para retorno sin bloquear pruebas locales

## Contrato funcional vigente

Flujo esperado:

1. storefront crea pedido
2. backend crea preferencia de Mercado Pago
3. frontend redirige a `checkoutUrl`
4. Mercado Pago vuelve a `/pago/retorno`
5. Mercado Pago notifica al backend
6. backend consulta pago real
7. webhook confirma pedido y pago
8. tracking publico refleja el estado real

## Configuracion esperada

- `MERCADOPAGO_ACCESS_TOKEN`
- `MERCADOPAGO_PUBLIC_KEY`
- `MERCADOPAGO_RETURN_URL`
- `MERCADOPAGO_WEBHOOK_URL`
- `MERCADOPAGO_BASE_URL`
- `MERCADOPAGO_CURRENCY`

Defaults locales:

- `MERCADOPAGO_BASE_URL=https://api.mercadopago.com`
- `MERCADOPAGO_RETURN_URL=http://localhost:5173/pago/retorno`
- `MERCADOPAGO_WEBHOOK_URL=http://localhost:8080/webhooks/psp/mercadopago`
- `MERCADOPAGO_CURRENCY=ARS`

## Observaciones

- `MERCADOPAGO_PUBLIC_KEY` queda disponible para futuro uso frontend, pero hoy no es obligatoria para el flujo backend + redirect.
- sin URL publica para webhook, la vuelta al storefront puede funcionar igual pero el pedido seguira pendiente hasta que entre la notificacion real.

## Verificacion tecnica

Se valido:

- suite backend `./mvnw -q test` en verde
- contrato publico de `checkoutUrl` mantenido
- seguridad publica del webhook mantenida bajo `/webhooks/psp/**`
- llamada real de prueba a `POST /checkout/preferences` con credenciales `TEST`:
  - responde `init_point`
  - responde `sandbox_init_point`

## Pendiente inmediato

- probar creacion real de preferencia con credenciales `TEST`
- exponer webhook publico con `ngrok`, `cloudflared` o equivalente
- ejecutar compra de prueba completa con tarjeta sandbox
- verificar:
  - retorno a `/pago/retorno`
  - entrada del webhook
  - `pedido = PAGADO`
  - `pago = CONFIRMADO`
