# Acta: RMA v2 mail reemplazo reenviado v1

Fecha: 2026-04-07

## Objetivo

Cerrar un hueco operativo del circuito de reposicion manual: avisar al cliente cuando el reemplazo ya fue reenviado y dejarle visible el tracking real en el correo.

## Decision aplicada

Se agrega un mail transaccional adicional para RMA solo en el hito:

1. `Reposicion reenviada al cliente`

No reemplaza el tracking canonico ni la mini bitacora del caso. Solo refuerza el momento de mayor valor para el cliente: cuando el reemplazo efectivamente sale de MD.

## Implementacion

- Se agrega el evento `EventoRmaReposicionReenviada`.
- `RmaService` publica ese evento al registrar `RMA_REPOSICION_REENVIADA_CLIENTE`.
- `NotificacionPostCompraListener` lo escucha luego del commit.
- `NotificacionPostCompraService` envia el mail con branding del storefront correcto.
- `PlantillaEmailPostCompraService` renderiza asunto y cuerpo con:
  - tracking canonico del pedido
  - tracking real del reemplazo
  - nombre del transportista si fue informado

## Criterio funcional

- Se mantiene la politica sobria de mails de postventa.
- El nuevo mail no abre un circuito logistico separado.
- El tracking publico sigue siendo la superficie principal del caso.
- El correo existe para reducir incertidumbre justo en el despacho del reemplazo.

## Verificacion

- Backend:
  - `./mvnw.cmd test "-Dtest=RmaServiceTest,NotificacionPostCompraServiceTest,PlantillaEmailPostCompraServiceTest,PublicPedidoTrackingServiceTest"`
  - OK

## Estado resultante

La reposicion manual trazada queda mas operativa:

- admin registra el reenvio
- el cliente recibe aviso automatico
- el correo muestra tracking real del reemplazo
- el caso sigue concentrado en el mismo enlace canonico
