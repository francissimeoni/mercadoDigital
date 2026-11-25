# Acta - Implementacion Proveedor Pedidos Frontend

Fecha: 2026-03-09

## Objetivo
Implementar panel de proveedor para gestionar pedidos pendientes de preparacion y marcar listo para retiro.

## Endpoints reales usados
- `GET /proveedores/{proveedorId}/pedidos-pendientes`
- `POST /envios/{pedidoId}/listo-para-retiro`

## DTOs reutilizados
- `PedidoProveedorResponse`
- `EnvioResponse`

## Cambios implementados
- Nuevo API client `src/api/proveedorApi.ts`:
  - `getPedidosProveedorPendientes(proveedorId)`
  - `marcarPedidoListoParaRetiro(pedidoId)`
- Nuevos hooks `src/hooks/useProveedorPedidos.ts`:
  - `usePedidosProveedor(proveedorId)`
  - `useMarcarListoParaRetiro(proveedorId)`
- Nuevo componente `src/components/proveedor/ProveedorPedidoItem.tsx`.
- Nueva pagina `src/pages/proveedor/ProveedorPedidosPage.tsx` con:
  - pedidoId
  - producto
  - cantidad
  - fecha
  - estado
  - accion `Marcar listo para retiro`
  - estados loading/error/vacio
- Nueva ruta protegida:
  - `/proveedor/pedidos`
- Link de acceso agregado en layout.

## Restricciones cumplidas
- Sin duplicar logica de backend.
- Sin inventar estados operativos.
- Sin recalculo de precios.
- Tailwind simple y cambios chicos.

## Validacion
- Build frontend ejecutado con `npm.cmd run build`.
