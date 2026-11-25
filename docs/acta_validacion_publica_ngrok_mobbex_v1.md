# Acta - Validacion publica ngrok Mobbex v1

Fecha: 2026-03-17

## Objetivo

Cerrar la validacion operativa real del flujo publico con Mobbex sandbox usando una URL publica accesible para webhook, sin agregar nueva logica de negocio y sin confirmar pagos desde frontend.

## Estado del codigo al inicio

El codigo ya estaba preparado correctamente para una validacion publica:

- `MOBBEX_WEBHOOK_URL`, `MOBBEX_RETURN_URL`, `MOBBEX_BASE_URL`, `MOBBEX_CHECKOUT_PATH`, `MOBBEX_API_KEY`, `MOBBEX_ACCESS_TOKEN` y `MOBBEX_CURRENCY` se toman desde configuracion externa
- `POST /webhooks/psp/mobbex` esta expuesto sin auth
- la `webhookUrl` que se envia a Mobbex se construye desde la URL configurada y agrega:
  - `referenciaPedido`
  - `referenciaPago`
- el frontend no confirma pagos
- el tracking publico sigue leyendo estado real desde backend

Referencias:

- [application.yml](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/resources/application.yml#L36)
- [application-local.yml](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/resources/application-local.yml#L5)
- [SecurityConfig.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/config/SecurityConfig.java#L30)
- [WebhookPspController.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/controller/WebhookPspController.java#L22)
- [MobbexClient.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/service/MobbexClient.java#L42)
- [PublicPedidoPagoPspService.java](/c:/Users/Usuario/Desktop/MercadoDigital/backend/src/main/java/ar/com/mercadodigital/backend/service/PublicPedidoPagoPspService.java#L28)

## Configuracion usada

Configuracion efectiva prevista para sandbox:

- `MOBBEX_BASE_URL=https://api.mobbex.com`
- `MOBBEX_CHECKOUT_PATH=/p/checkout`
- `MOBBEX_RETURN_URL=http://localhost:5173/pago/retorno`
- `MOBBEX_WEBHOOK_URL=https://<url-publica>/webhooks/psp/mobbex`
- `MOBBEX_CURRENCY=ARS`
- `MOBBEX_API_KEY=<sandbox>`
- `MOBBEX_ACCESS_TOKEN=<sandbox>`

En esta sesion no habia cargadas variables `MOBBEX_*`.

## URL publica usada

No disponible.

Motivo:

- en esta maquina no estaba instalado `ngrok`
- tampoco habia un tunel alternativo disponible (`cloudflared` o `localtunnel`)

## Verificaciones realizadas

Verificacion de configuracion:

- backend toma `MOBBEX_WEBHOOK_URL` y `MOBBEX_RETURN_URL` desde propiedades externas
- `MobbexClient` usa:
  - header `x-api-key`
  - header `x-access-token`
  - `return_url`
  - `webhook`
  - `currency`
  - `checkout-path`
  - `base-url`

Verificacion de seguridad:

- `POST /webhooks/psp/mobbex` queda accesible sin auth por `permitAll`

Verificacion de correlacion:

- la URL de webhook se arma desde `MOBBEX_WEBHOOK_URL`
- se anexan `referenciaPedido` y `referenciaPago`
- no depende de `localhost` salvo que asi se configure la variable

Verificacion de tracking:

- el tracking publico sigue mostrando `estadoPedido` y `estadoPago` desde backend
- no hay confirmacion local desde frontend

## Resultado real de la prueba

No se logro ejecutar una prueba publica real completa en esta sesion.

Puntos no cerrados en la prueba real:

- no se obtuvo URL publica accesible para webhook
- no se ejecuto compra sandbox real en Mobbex
- no se recibio webhook real de Mobbex
- no se pudo evidenciar cambio real a:
  - `pedido = PAGADO`
  - `pago = CONFIRMADO`
- no se pudo evidenciar tracking actualizado por webhook real

## Evidencia de webhook recibido o no

No hay evidencia de webhook real recibido en esta sesion.

Estado:

- webhook real: no validado
- causa inmediata: sin URL publica activa y sin credenciales sandbox cargadas en el entorno

## Ajustes realizados

No fue necesario tocar logica de negocio ni contratos.

No se realizaron cambios de codigo adicionales en esta etapa.

## Evidencia tecnica si verificada

Se validaron tests relacionados con PSP, webhook y seguridad:

- `MobbexClientTest`
- `MobbexWebhookAdapterTest`
- `PublicPedidoPagoPspServiceTest`
- `WebhookPspServiceTest`
- `WebhookPspControllerTest`
- `SecurityAccessTest`

Tambien se valido:

- `frontend`: `npm.cmd run build`

Lectura funcional confirmada:

- webhook publico abierto en seguridad
- controlador `POST /webhooks/psp/mobbex` operativo
- mapping Mobbex -> `WebhookPspRequest` consistente
- idempotencia de reintento de `POST /public/pedido/{pedidoId}/pago` mantenida por `pspCheckoutUrl`

## Procedimiento operativo real para cerrar la validacion

1. cargar credenciales sandbox reales en variables de entorno:
   - `MOBBEX_API_KEY`
   - `MOBBEX_ACCESS_TOKEN`
2. levantar backend con perfil local
3. levantar frontend en `http://localhost:5173`
4. instalar o disponer de `ngrok`
5. abrir tunel:
   - `ngrok http 8080`
6. tomar la URL HTTPS publica entregada por ngrok
7. exportar:
   - `MOBBEX_BASE_URL=https://api.mobbex.com`
   - `MOBBEX_CHECKOUT_PATH=/p/checkout`
   - `MOBBEX_RETURN_URL=http://localhost:5173/pago/retorno`
   - `MOBBEX_WEBHOOK_URL=https://<url-publica-ngrok>/webhooks/psp/mobbex`
   - `MOBBEX_CURRENCY=ARS`
8. reiniciar backend para que tome la nueva configuracion
9. crear pedido publico desde storefront
10. preparar PSP con `POST /public/pedido/{pedidoId}/pago`
11. verificar que el checkout enviado a Mobbex use la URL publica real en `webhook`
12. completar pago sandbox
13. verificar retorno a `/pago/retorno`
14. verificar recepcion de webhook en backend
15. confirmar:
   - pedido antes del webhook: `CREADO`
   - pago antes del webhook: `PENDIENTE`
   - pedido despues del webhook: `PAGADO`
   - pago despues del webhook: `CONFIRMADO`
   - tracking publico actualizado
16. reintentar `POST /public/pedido/{pedidoId}/pago` y comprobar que no genera inconsistencias

## Que falto para cerrar en esta sesion

- instalar/configurar un tunel publico real
- cargar credenciales sandbox reales en variables de entorno
- ejecutar compra manual real en Mobbex sandbox
- recolectar evidencia operativa:
  - pedido previo al pago
  - retorno al storefront
  - webhook recibido
  - pedido `PAGADO`
  - pago `CONFIRMADO`
  - tracking actualizado

## Conclusion

El sistema esta listo en codigo para validacion publica con `ngrok` o equivalente.

Lo que impidio cerrar la prueba real en esta sesion no fue una incompatibilidad funcional detectada en backend/frontend, sino la ausencia de:

- URL publica activa para webhook
- credenciales sandbox cargadas en el entorno

No quedan cambios de negocio pendientes para intentar la validacion real; queda ejecutar el procedimiento operativo en una sesion con tunel publico y credenciales sandbox disponibles.
