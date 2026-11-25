# Acta: RMA v2 mails postventa sobrios v1

Fecha: 2026-04-07

## Objetivo

Cerrar la comunicacion transaccional del caso de postventa sin contaminar al cliente con correos por cada microestado interno.

## Decision aplicada

Se implementa una politica de 3 mails para RMA/postventa:

1. `Caso recibido`
2. `Ya puedes enviar el producto`
3. `Tu caso fue resuelto`

El resto del seguimiento del caso queda concentrado en el mismo link canonico del pedido/tracking.

## Implementacion

- Se agregan eventos de dominio para mails de postventa:
  - `EventoRmaSolicitado`
  - `EventoRmaEnvioSolicitado`
  - `EventoRmaCerrado`
- `RmaService` publica esos eventos en:
  - apertura del caso
  - `Solicitar envio`
  - cierre por `RESUELTO`, `REEMBOLSADO` o `RECHAZADO`
- `NotificacionPostCompraListener` escucha esos eventos luego del commit.
- `NotificacionPostCompraService` envia los tres correos usando branding del storefront correcto.
- `PlantillaEmailPostCompraService` suma plantillas sobrias orientadas a:
  - confirmar recepcion del reclamo
  - avisar que el rotulo ya esta disponible
  - informar el cierre del caso con resolucion y, si aplica, compensacion

## Criterio UX

- No se envian mails por `recibido en deposito` ni por otras microtransiciones internas.
- El tracking publico sigue siendo la superficie principal para ver el estado del reclamo.
- El mail solo se usa cuando:
  - confirma recepcion
  - pide una accion concreta del cliente
  - comunica la resolucion final

## Verificacion

- Backend:
  - `./mvnw.cmd test "-Dtest=RmaServiceTest,NotificacionPostCompraServiceTest,PlantillaEmailPostCompraServiceTest,PublicPedidoTrackingServiceTest,PublicPedidoTrackingControllerTest,EnvioServiceTest"`
  - OK
- Frontend:
  - `npm run build`
  - OK

## Estado resultante

El circuito de postventa queda mejor balanceado:

- menos ruido de mails
- mas trazabilidad en el mismo link del pedido
- branding correcto por storefront
- cobertura automatizada del disparo y del contenido base de las plantillas
