# Acta - Implementacion Pedidos del Revendedor Frontend

Fecha: 2026-03-09

## Objetivo
Implementar modulo de pedidos del revendedor con listado real y detalle real usando endpoints del backend.

## Endpoints reales usados
- `GET /revendedores/{revendedorId}/pedidos`
- `GET /pedido/{id}`

## DTOs backend reutilizados
- `PedidoRevendedorResponse` para listado.
- `PedidoResponse` y `PedidoItemResponse` para detalle.

## Cambios implementados
- Extension de `pedidoApi`:
  - `getPedidosRevendedor(revendedorId)`
  - `getPedidoDetalle(pedidoId)`
- Nuevo hook `src/hooks/usePedidos.ts`:
  - `usePedidos(revendedorId)`
  - `usePedidoDetalle(pedidoId)`
- Reemplazo de `PedidosPage` por listado funcional:
  - pedidoId visible
  - fecha
  - estado
  - total
  - accion "Ver detalle"
  - estados loading/error/vacio
- Nueva pagina `PedidoDetallePage`:
  - items del pedido
  - precio congelado por item (`precioItemFinal`)
  - estado actual del pedido/pago/envio
  - totales retornados por backend
- Nueva ruta protegida:
  - `/pedidos/:pedidoId`

## Restricciones cumplidas
- Sin recalculo de importes en frontend.
- Sin duplicacion de logica de negocio.
- Sin exponer datos de proveedor en UI del revendedor.
- Cambios minimos y compilables.

## Validacion
- Build frontend ejecutado con `npm.cmd run build`.
