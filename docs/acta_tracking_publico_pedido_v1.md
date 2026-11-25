# Acta - Tracking publico pedido v1

Fecha: 2026-03-12

## Objetivo

Implementar seguimiento publico de pedido por token para compras `PUBLICO_DIRECTO`, permitiendo consultar estado sin login y sin exponer datos internos del backend.

## Generacion del token

Se agrega `Pedido.trackingToken`.

Reglas:

- se genera automaticamente con `UUID.randomUUID()` al crear pedidos con `origenPedido = PUBLICO_DIRECTO`
- queda `null` para pedidos `OPERATIVOS`
- se persiste en columna nullable con indice unico

Motivo:

- evita usar ids operativos como mecanismo publico
- entrega un identificador no predecible
- mantiene separado el acceso publico del acceso autenticado operativo

## Migracion

Se agrega:

- `backend/src/main/resources/db/migration/V11__pedido_tracking_token.sql`

Cambios:

- columna `tracking_token` en `pedidos`
- unique index
- nullable para compatibilidad con pedidos historicos y operativos

## Endpoint publico

Se agrega:

- `GET /public/pedido/{trackingToken}`

Controller:

- `PublicPedidoTrackingController`

Service:

- `PublicPedidoTrackingService`

## Informacion expuesta

Se devuelve un DTO publico con:

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

## Informacion oculta

No se expone:

- proveedor
- `proveedorId`
- costos internos
- markup
- split
- stock interno
- ids operativos internos de abastecimiento

## Politica de respuesta

Se responde `404 Not Found` cuando:

- el token no existe
- el pedido existe pero no es `PUBLICO_DIRECTO`

Se responde `200 OK` cuando:

- el token es valido y pertenece a una compra publica directa

## Relacion con compra publica

La compra publica directa ahora tambien devuelve `trackingToken` en su response.

Esto mantiene continuidad natural con:

- `POST /public/pedido/directo`
- `POST /public/pedido/{pedidoId}/pago`
- `GET /public/pedido/{trackingToken}`

El token no confirma pagos ni altera el flujo PSP.
La verdad del pago sigue estando solo en webhook.

## Tests agregados o ajustados

- `backend/src/test/java/ar/com/mercadodigital/backend/service/PublicPedidoTrackingServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/PublicPedidoTrackingControllerTest.java`
- ajuste en `backend/src/test/java/ar/com/mercadodigital/backend/controller/SecurityAccessTest.java`
- ajuste en tests de compra publica y `PedidoService` para cubrir `trackingToken`

## Resultado

El backend ya permite consultar un pedido publico directo por token no predecible, con un contrato seguro para cliente final y sin exponer detalles sensibles del modelo operativo interno.
