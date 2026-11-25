# Acta - Implementacion Carrito Frontend

Fecha: 2026-03-09

## Objetivo
Implementar modulo de carrito en frontend conectado a backend por `POST /carrito/items` y `GET /carrito`.

## Cambios implementados
- Nuevo API client en `src/api/carritoApi.ts`:
  - `addItemToCarrito(payload)`
  - `getCarrito()`
- Nuevos hooks TanStack Query en `src/hooks/useCarrito.ts`:
  - `useCarrito()`
  - `useAgregarAlCarrito()`
- Conexion del boton "Agregar al carrito" en `ProductoDetallePage`.
- Nueva pagina `src/pages/CarritoPage.tsx` con estados:
  - loading
  - error con reintento
  - carrito vacio
  - listado de items
- Nuevo componente `src/components/carrito/CarritoItem.tsx`.
- Nueva ruta protegida `/carrito` y acceso desde sidebar.

## Restricciones respetadas
- No se recalculan precios en frontend.
- No se maneja stock en frontend.
- Backend queda como fuente de verdad.
- Se usa TanStack Query para lectura/mutacion.

## Validacion
- Build frontend ejecutado con `npm.cmd run build`.
