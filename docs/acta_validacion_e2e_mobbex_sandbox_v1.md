# Acta - Validacion E2E Mobbex sandbox v1

Fecha: 2026-03-17

## Objetivo

Validar y dejar cerrado el flujo end-to-end de compra publica con Mobbex sandbox en Mercado Digital, manteniendo:

- backend como fuente de verdad
- frontend sin confirmacion de pagos
- webhook como unica via de confirmacion real

## Flujo revisado

Flujo esperado:

1. storefront crea pedido con `POST /public/pedido/directo`
2. storefront prepara PSP con `POST /public/pedido/{pedidoId}/pago`
3. backend crea checkout real en Mobbex
4. storefront redirige a `checkoutUrl`
5. Mobbex devuelve al storefront por `returnUrl`
6. Mobbex notifica al backend por webhook
7. backend confirma pago y pedido
8. tracking publico refleja el estado real

## Configuracion revisada

Configuracion actual de Mobbex en backend:

- headers requeridos:
  - `x-api-key`
  - `x-access-token`
- base URL:
  - configurable por `MOBBEX_BASE_URL`
  - default: `https://api.mobbex.com`
- checkout path:
  - configurable por `MOBBEX_CHECKOUT_PATH`
  - default: `/p/checkout`
- return URL:
  - configurable por `MOBBEX_RETURN_URL`
- webhook URL:
  - configurable por `MOBBEX_WEBHOOK_URL`
- currency:
  - configurable por `MOBBEX_CURRENCY`
  - default: `ARS`

Referencia de codigo:

- [application-local.yml](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/resources/application-local.yml#L5)
- [MobbexClient.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/service/MobbexClient.java#L29)
- [MobbexWebhookAdapter.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/service/MobbexWebhookAdapter.java#L17)
- [WebhookPspService.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/service/WebhookPspService.java#L46)
- [PublicPedidoTrackingService.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/service/PublicPedidoTrackingService.java#L31)

## Configuracion local dejada lista

Variables de entorno a usar para sandbox:

- `MOBBEX_API_KEY`
- `MOBBEX_ACCESS_TOKEN`
- `MOBBEX_BASE_URL`
- `MOBBEX_CHECKOUT_PATH`
- `MOBBEX_RETURN_URL`
- `MOBBEX_WEBHOOK_URL`
- `MOBBEX_CURRENCY`

Defaults locales seguros que quedaron configurados:

- `MOBBEX_BASE_URL=https://api.mobbex.com`
- `MOBBEX_CHECKOUT_PATH=/p/checkout`
- `MOBBEX_RETURN_URL=http://localhost:5173/pago/retorno`
- `MOBBEX_WEBHOOK_URL=http://localhost:8080/webhooks/psp/mobbex`
- `MOBBEX_CURRENCY=ARS`

Las credenciales ya no quedan hardcodeadas en `application-local.yml`.

## Ajustes realizados

Se hizo un unico ajuste real y minimo:

- se eliminaron credenciales sandbox hardcodeadas de [application-local.yml](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/resources/application-local.yml#L1)
- se dejo toda la configuracion local de Mobbex basada en variables de entorno
- se corrigio el `return-url` local por defecto para que apunte a `/pago/retorno` y no a `/`

No se agrego logica nueva de negocio.

## Resultado de la validacion

Validado en este entorno:

- configuracion backend de Mobbex revisada y consistente con las actas previas
- headers, payload y correlacion de checkout revisados
- mapping del webhook Mobbex al contrato interno revisado
- confirmacion exclusiva por webhook mantenida
- tracking publico sigue leyendo estados reales desde backend
- idempotencia de reintento de `POST /public/pedido/{pedidoId}/pago` mantenida por reutilizacion de `pspCheckoutUrl`
- frontend compila
- tests backend criticos del flujo PSP/webhook pasan

Comandos verificados:

- `npm.cmd run build` en `frontend`
- `./mvnw -q "-Dtest=MobbexClientTest,MobbexWebhookAdapterTest,PublicPedidoPagoPspServiceTest,WebhookPspServiceTest" test` en `backend`

## Resultado de la prueba manual real

No se pudo cerrar una validacion manual real completa del flujo sandbox desde este entorno de trabajo.

Estado real de la prueba:

- no queda evidenciado aqui un pago sandbox manual completado extremo a extremo
- no queda evidenciado aqui el retorno real desde Mobbex luego de completar tarjeta sandbox
- no queda evidenciado aqui el arribo real del webhook automatico a backend local

## Webhook: validado o no

El webhook no pudo validarse end-to-end en local.

Motivo principal:

- `MOBBEX_WEBHOOK_URL` apuntando a `localhost` no es accesible desde Mobbex sandbox

Consecuencia:

- el pedido puede volver al storefront por `returnUrl`
- pero debe seguir `PENDIENTE` hasta que llegue el webhook real
- sin URL publica no puede comprobarse la confirmacion automatica real del pago

## Limitaciones encontradas en local

- backend local no expone una URL publica accesible por Mobbex
- el webhook automatico no puede entrar a `localhost`
- por eso no se puede demostrar en local puro:
  - pago aprobado sandbox
  - pedido pendiente hasta webhook
  - transicion posterior a `PAGADO`
  - tracking actualizado luego del webhook real

## Pasos concretos para validar con ngrok o equivalente

1. levantar backend local en `http://localhost:8080`
2. levantar frontend en `http://localhost:5173`
3. exponer backend con `ngrok http 8080`
4. tomar la URL publica HTTPS entregada por ngrok
5. exportar variables:
   - `MOBBEX_API_KEY=<sandbox>`
   - `MOBBEX_ACCESS_TOKEN=<sandbox>`
   - `MOBBEX_BASE_URL=https://api.mobbex.com`
   - `MOBBEX_CHECKOUT_PATH=/p/checkout`
   - `MOBBEX_RETURN_URL=http://localhost:5173/pago/retorno`
   - `MOBBEX_WEBHOOK_URL=https://<tu-subdominio-ngrok>/webhooks/psp/mobbex`
   - `MOBBEX_CURRENCY=ARS`
6. reiniciar backend para tomar las variables
7. crear pedido desde storefront
8. ejecutar `POST /public/pedido/{pedidoId}/pago`
9. completar pago en Mobbex sandbox
10. verificar retorno a `/pago/retorno`
11. confirmar que inicialmente el tracking puede seguir `PENDIENTE` hasta webhook
12. revisar logs del backend y tabla `psp_webhooks`
13. confirmar luego:
   - `pedido.estadoPedido = PAGADO`
   - `pago.estadoPago = CONFIRMADO`
   - tracking publico con estado real actualizado
14. reintentar `POST /public/pedido/{pedidoId}/pago` y verificar que reutiliza el mismo `checkoutUrl` sin inconsistencias

## Escenarios manuales a cerrar en la validacion publica

Pendientes de validar en entorno accesible:

- pago sandbox aprobado
- retorno al storefront
- pedido sigue pendiente hasta webhook
- tracking muestra estado correcto luego del webhook
- reintento de `POST /public/pedido/{pedidoId}/pago` no crea inconsistencias

## Pendiente para produccion

- usar `MOBBEX_RETURN_URL` y `MOBBEX_WEBHOOK_URL` publicos reales
- asegurar manejo de secretos en variables o secret manager
- validar firma/origen del webhook si Mobbex lo exige para produccion
- ejecutar una prueba end-to-end real en entorno accesible y dejar evidencia operativa

## Conclusion

El sistema quedo mejor preparado para la validacion E2E real:

- sin secretos hardcodeados
- con `return-url` local alineado a la ruta implementada
- con backend, webhook y tracking manteniendo la fuente de verdad correcta

La validacion definitiva del pago confirmado por webhook sigue pendiente de exponer el backend mediante `ngrok` o una URL publica equivalente.
