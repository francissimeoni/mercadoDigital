# Acta - Implementacion Creacion de Pedido Frontend

Fecha: 2026-03-09

## Objetivo
Implementar confirmacion de carrito y creacion de pedido desde frontend usando endpoint real de backend.

## Endpoint real usado
- `POST /pedido/canonico`
- DTO backend reutilizado:
  - `CrearPedidoCanonicoRequest`
  - `PedidoItemCanonicoRequest`

## Cambios implementados
- Nuevo API client `src/api/pedidoApi.ts`:
  - `crearPedidoCanonico(payload)`
- Nuevo hook `src/hooks/useCrearPedido.ts` con TanStack Query.
- Carrito conectado con boton `Confirmar pedido` en `src/pages/CarritoPage.tsx`:
  - loading durante mutacion
  - error visible
  - exito con navegacion
- Nueva pantalla de confirmacion:
  - `src/pages/PedidoConfirmadoPage.tsx`
- Rutas agregadas:
  - `/pedidos/confirmado/:pedidoId`
- Integracion minima de `PedidosPage` para reflejar pedido creado via query param `?creado=...`.

## Restricciones cumplidas
- No se recalculan importes en frontend.
- No se mueve logica de negocio al frontend.
- Backend se mantiene como fuente de verdad.
- Cambios minimos, claros y compilables.

## Validacion
- Build frontend ejecutado con `npm.cmd run build`.
