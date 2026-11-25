# Acta - mails cliente tracking hitos logisticos v1

Fecha: 2026-04-07

## Objetivo

Completar el circuito de mails del cliente final usando el tracking publico como enlace canonico y evitando ruido por microestados internos.

## Decision aplicada

Se agregaron mails transaccionales para cliente final en dos hitos logistico-operativos grandes:

- `ENVIO_DESPACHADO`
- `ENVIO_ENTREGADO`

Se mantuvieron los ya existentes:

- `PEDIDO_CREADO`
- `PAGO_CONFIRMADO`

## Criterio de experiencia

- el mail no reemplaza al tracking
- el tracking publico es la superficie principal de verdad
- cada mail reutiliza el mismo enlace `/pedido/:trackingToken`
- `ENTREGADO` ya orienta al cliente a postventa/garantia desde ese mismo link

## Implementacion

Backend:

- nuevos eventos postcompra:
  - `EventoEnvioDespachado`
  - `EventoEnvioEntregado`
- `EnvioService` publica esos eventos al despachar y entregar
- `NotificacionPostCompraListener` los escucha `AFTER_COMMIT`
- `NotificacionPostCompraService` envia mails con auditoria `ENVIADO|ERROR|OMITIDO`
- `PlantillaEmailPostCompraService` suma plantillas para:
  - pedido en camino
  - pedido entregado

## Archivos principales

- `backend/src/main/java/ar/com/mercadodigital/backend/service/EnvioService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/EventoEnvioDespachado.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/EventoEnvioEntregado.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/NotificacionPostCompraListener.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/NotificacionPostCompraService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/PlantillaEmailPostCompraService.java`

## Cobertura

Se agregaron/ajustaron pruebas para:

- disparo de eventos desde `EnvioService`
- render de plantillas `DESPACHADO` y `ENTREGADO`
- envio postcompra para ambos eventos

## Verificacion tecnica

- `./mvnw.cmd test` en backend: OK

## Resultado funcional

El cliente final ahora puede recibir:

- mail inicial con tracking
- mail de pago confirmado
- mail cuando el pedido ya va en camino
- mail cuando el pedido fue entregado

Todo apuntando al mismo tracking publico, que ademas queda preparado como punto de entrada a postventa.
