# Acta - Inicio PSP compra publica v1

Fecha: 2026-03-11

## Objetivo

Preparar en backend el inicio concreto del checkout PSP para la compra publica directa ya existente, reutilizando el esquema actual de `Pago` y `WebhookPsp` sin marcar pagos desde frontend ni romper la confirmacion real por webhook.

## Estrategia elegida para iniciar PSP

Se adopto un segundo endpoint publico especifico:

- `POST /public/pedido/{pedidoId}/pago`

Motivo:

- mantiene separado el momento de crear pedido del momento de preparar PSP
- evita mezclar demasiada responsabilidad en `POST /public/pedido/directo`
- permite reintentar la preparacion de checkout de forma idempotente sin recrear el pedido
- encaja mejor con la integracion actual, donde el webhook ya trabaja con:
  - `referenciaPedido`
  - `referenciaPago`
  - `monto`

La preparacion PSP elegida es un `payload` de inicializacion compatible con el esquema existente del webhook.

## Contrato agregado

Endpoint:

- `POST /public/pedido/{pedidoId}/pago`

Response:

- `pedidoId`
- `estadoPedido`
- `estadoPago`
- `metodoPago`
- `monto`
- `referenciaPedido`
- `referenciaPago`

### Contrato que consumira frontend

El frontend debe usar:

- `referenciaPedido = pedidoId`
- `referenciaPago = referencia PSP estable generada por backend`
- `monto = total a pagar`

Este contrato queda listo para ser usado por la futura integracion concreta con el PSP o su adaptador frontend/backend, sin inventar confirmacion de pago.

## Reutilizacion de la integracion PSP existente

No se creo una integracion paralela.

Se reutiliza:

- `Pago.metodo = PSP`
- `Pago.estadoPago = PENDIENTE`
- `Pago.pspReferencia`
- `WebhookPspService`

Decision clave:

- `Pago.pspReferencia` pasa a persistir una referencia estable de inicio PSP cuando el checkout publico se prepara por primera vez
- el endpoint es idempotente: si la referencia ya existe, la reutiliza
- el webhook existente sigue funcionando sin cambio de verdad

La expectativa operativa es que la futura integracion PSP reutilice esa misma `referenciaPago` al confirmar por webhook.

## Validaciones aplicadas

Se responde `404 Not Found` cuando:

- el pedido no existe
- el pedido existe pero no corresponde a compra publica directa

Se responde `409 Conflict` cuando:

- el pedido no esta en `CREADO`
- el pago no existe
- el pago no esta en `PENDIENTE`
- el metodo de pago no es `PSP`

## Como se valida que corresponda al flujo publico esperado

Se agrego `origenPedido` en `Pedido` con dos valores:

- `OPERATIVO`
- `PUBLICO_DIRECTO`

La compra publica directa ahora persiste `Pedido.origenPedido = PUBLICO_DIRECTO`.

El endpoint publico de preparacion PSP solo admite pedidos con ese origen.

Esto evita exponer por error pedidos operativos/autenticados a la superficie publica.

## Como se evita duplicacion o reinicio inconsistente del pago

La preparacion de PSP es idempotente:

- si `Pago.pspReferencia` no existe, se genera una sola vez
- si ya existe, se devuelve la misma referencia

No se recrea `Pago`.
No se cambia el estado a `CONFIRMADO`.
No se mueve el pedido fuera de `CREADO`.

## Relacion con el webhook existente

No se modifica la verdad del pago:

- frontend/storefront solo inicia PSP
- `WebhookPspService` sigue siendo el unico punto que confirma el pago
- el pedido permanece en `CREADO`
- el pago permanece en `PENDIENTE`
- solo el webhook lo pasa a `PAGADO` / `CONFIRMADO`

## Impacto sobre compatibilidad

- no se rompio `POST /public/pedido/directo`
- no se rompio `WebhookPspController` ni `WebhookPspService`
- no se altero la semantica actual de confirmacion de pago
- `/public/**` ya estaba permitido por seguridad, por lo que no hizo falta abrir una excepcion adicional fuera de la politica existente

## Tests agregados o ajustados

Se agregaron tests minimos para:

- preparacion exitosa de checkout PSP
- reutilizacion idempotente de referencia PSP existente
- pedido inexistente
- pedido en estado invalido
- pedido no publico
- acceso publico sin token

Archivos:

- `backend/src/test/java/ar/com/mercadodigital/backend/service/PublicPedidoPagoPspServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/PublicPedidoPagoPspControllerTest.java`
- ajuste en `backend/src/test/java/ar/com/mercadodigital/backend/controller/SecurityAccessTest.java`

## Pendiente para la siguiente etapa

- integrar el adaptador concreto del PSP desde frontend o backend segun el proveedor real elegido
- usar este contrato para abrir la URL/session/preference real del PSP
- retorno UX post-redireccion
- seguimiento de pedido post-pago

## Resultado

El backend ya entrega un dato concreto y consistente para iniciar el checkout PSP de un pedido publico directo, manteniendo idempotencia, validacion de origen publico y compatibilidad total con la confirmacion posterior por webhook.
