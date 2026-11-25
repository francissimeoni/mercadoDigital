# Acta: RMA v2 mail reemplazo entregado v1

Fecha: 2026-04-07

## Objetivo

Cerrar la comunicacion final del circuito manual de reposicion cuando el reemplazo ya fue entregado al cliente.

## Decision aplicada

Se agrega un mail transaccional adicional al marcar:

1. `Reposicion entregada al cliente`

Esto complementa el aviso previo de `reemplazo reenviado` y deja el cierre completo de la reposicion sin abrir un circuito logistico aparte.

## Implementacion

- Se agrega el evento `EventoRmaReposicionEntregada`.
- `RmaService` lo publica al registrar `RMA_REPOSICION_ENTREGADA_CLIENTE`.
- `NotificacionPostCompraListener` escucha el evento luego del commit.
- `NotificacionPostCompraService` envia el mail con branding correcto.
- `PlantillaEmailPostCompraService` informa entrega final y reutiliza el enlace canonico del pedido.

## Verificacion

- Backend:
  - `./mvnw.cmd test "-Dtest=RmaServiceTest,EnvioServiceTest,NotificacionPostCompraServiceTest,PlantillaEmailPostCompraServiceTest,PublicPedidoTrackingServiceTest"`
  - OK

## Estado resultante

La comunicacion del reemplazo queda mas cerrada:

- mail cuando sale el reemplazo
- mail cuando la entrega final queda confirmada
- el tracking del pedido sigue siendo la fuente principal del caso
