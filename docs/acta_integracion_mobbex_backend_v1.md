# Acta - Integracion Mobbex backend v1

Fecha: 2026-03-12

## Objetivo

Integrar Mobbex como PSP real para el flujo de compra publica ya existente, reutilizando:

- `POST /public/pedido/directo`
- `POST /public/pedido/{pedidoId}/pago`
- `Pago` en estado `PENDIENTE`
- confirmacion exclusiva por webhook

Sin confirmar pagos desde frontend y sin abrir un circuito paralelo.

## Configuracion agregada

Se agrega configuracion externa bajo prefijo `mobbex`:

- `mobbex.base-url`
- `mobbex.checkout-path`
- `mobbex.api-key`
- `mobbex.access-token`
- `mobbex.return-url`
- `mobbex.webhook-url`
- `mobbex.currency`

En `application.yml` quedaron mapeadas a variables de entorno:

- `MOBBEX_BASE_URL`
- `MOBBEX_CHECKOUT_PATH`
- `MOBBEX_API_KEY`
- `MOBBEX_ACCESS_TOKEN`
- `MOBBEX_RETURN_URL`
- `MOBBEX_WEBHOOK_URL`
- `MOBBEX_CURRENCY`

### Donde colocar la API key

La `api key` de Mobbex debe cargarse fuera del codigo, via:

- variable de entorno `MOBBEX_API_KEY`

Y el access token del proveedor via:

- variable de entorno `MOBBEX_ACCESS_TOKEN`

No se escriben secretos en codigo, tests, logs ni documentacion.

## Estrategia de integracion

Se agrega `MobbexClient` como adaptador backend del proveedor.

Responsabilidad:

- crear el checkout real en Mobbex a partir del pedido publico ya creado
- reutilizar:
  - `referenciaPedido = pedidoId`
  - `referenciaPago = Pago.pspReferencia`
  - `monto = Pago.monto`
- devolver `checkoutUrl` para que frontend pueda redirigir sin refactor grande posterior

La respuesta publica elegida para `POST /public/pedido/{pedidoId}/pago` ahora incluye:

- `pedidoId`
- `estadoPedido`
- `estadoPago`
- `metodoPago`
- `monto`
- `referenciaPedido`
- `referenciaPago`
- `checkoutUrl`

## Politica de idempotencia

La preparacion de PSP sigue siendo idempotente.

Reglas:

- `Pago.pspReferencia` sigue siendo la referencia estable del pago
- si el pedido ya tiene `pspCheckoutUrl`, se reutiliza y no se vuelve a llamar a Mobbex
- si todavia no tiene `pspCheckoutUrl`, se crea una sola preparacion real y se persiste en `Pago.pspCheckoutUrl`

Para soportar esto se agrega:

- columna `pagos.psp_checkout_url`
- migracion `V12__pago_psp_checkout_url.sql`

Con esta persistencia se evita crear multiples checkouts inconsistentes frente a reintentos del endpoint publico.

## Relacion con webhook

El webhook sigue siendo la unica verdad del pago.

No cambia la regla de negocio:

- el endpoint publico solo prepara checkout
- el pedido permanece en `CREADO`
- el pago permanece en `PENDIENTE`
- solo webhook confirma el pago y mueve el pedido a `PAGADO`

## Adaptacion del webhook de Mobbex

Se conserva `WebhookPspService` como servicio central de confirmacion.

Se agrega una entrada tecnica:

- `POST /webhooks/psp/mobbex`

Este endpoint:

- recibe el payload real de Mobbex
- adapta su estructura al DTO interno `WebhookPspRequest`
- delega en `WebhookPspService`

Mapeo usado:

- `idEventoPsp` se arma desde `data.payment.id`
- `monto` se toma de `data.payment.total`
- `referenciaPedido` via query param en la `webhookUrl`
- `referenciaPago` via query param en la `webhookUrl`
- estado aprobado de Mobbex se traduce a:
  - `tipoEvento = PAGO_CONFIRMADO`
  - `estado = CONFIRMADO`

La URL de webhook enviada a Mobbex se construye con query params para no depender de ids operativos expuestos al frontend y para mantener correlacion estable con el flujo publico existente.

## Archivos agregados o modificados

Backend:

- `backend/src/main/java/ar/com/mercadodigital/backend/config/MobbexProperties.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/MobbexClient.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/MobbexWebhookAdapter.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/PublicPedidoPagoPspService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/controller/WebhookPspController.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/config/SecurityConfig.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/dto/response/PublicPedidoPagoPspResponse.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/entity/Pago.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/exception/IntegracionPspException.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/exception/GlobalExceptionHandler.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/V12__pago_psp_checkout_url.sql`

Tests:

- `backend/src/test/java/ar/com/mercadodigital/backend/service/PublicPedidoPagoPspServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/service/MobbexClientTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/service/MobbexWebhookAdapterTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/PublicPedidoPagoPspControllerTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/WebhookPspControllerTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/SecurityAccessTest.java`

## Que queda pendiente del lado frontend

El frontend ya estaba preparado para `checkoutUrl`.

Pendiente funcional:

- redirigir automaticamente con `window.location.href = checkoutUrl` cuando la UX de tienda lo decida
- resolver UX de retorno desde `returnUrl`
- mostrar estados post-redireccion si se agregan callbacks o polling complementario

No queda pendiente ninguna confirmacion de pago en frontend porque esa responsabilidad sigue prohibida.

## Resultado

El backend ya inicia checkout real en Mobbex, devuelve `checkoutUrl` utilizable por storefront, reutiliza la misma preparacion ante reintentos y mantiene la confirmacion del pago exclusivamente por webhook.
