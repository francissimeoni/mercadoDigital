# Checklist Pruebas Manuales - Auth y Roles

Fecha: 2026-03-10

## Preparacion
- Levantar backend local con usuarios demo seed.
- Levantar frontend local.
- Limpiar `localStorage` antes de cada prueba si hace falta.

## Revendedor
- Login con `revendedor.demo@md.local`
- Verificar redireccion a `/catalogo`
- Verificar links visibles:
  - `Catalogo`
  - `Carrito`
  - `Pedidos`
  - `Notificaciones`
- Intentar abrir manualmente:
  - `/proveedor/pedidos`
  - `/logistica/envios`
  - `/rma`
- Verificar redireccion a `/catalogo`

## Proveedor
- Login con `proveedor.demo@md.local`
- Verificar redireccion a `/proveedor/pedidos`
- Verificar links visibles:
  - `Proveedor`
  - `Notificaciones`
- Intentar abrir manualmente:
  - `/catalogo`
  - `/carrito`
  - `/pedidos`
  - `/logistica/envios`
  - `/rma`
- Verificar redireccion a `/proveedor/pedidos`

## Logistica
- Login con `logistica.demo@md.local`
- Verificar redireccion a `/logistica/envios`
- Verificar links visibles:
  - `Logistica`
  - `Notificaciones`
- Intentar abrir manualmente:
  - `/catalogo`
  - `/carrito`
  - `/pedidos`
  - `/proveedor/pedidos`
  - `/rma`
- Verificar redireccion a `/logistica/envios`

## Admin
- Login con `admin.demo@md.local`
- Verificar redireccion a `/rma`
- Verificar links visibles:
  - `RMA`
  - `Notificaciones`
- Intentar abrir manualmente:
  - `/catalogo`
  - `/carrito`
  - `/pedidos`
  - `/proveedor/pedidos`
  - `/logistica/envios`
- Verificar redireccion a `/rma`

## Expiracion / sesion invalida
- Con una sesion activa, forzar token vencido o borrar/alterar `authSession` en `localStorage`
- Navegar o refrescar
- Verificar limpieza de sesion
- Verificar redireccion automatica a `/login`

## 401 backend
- Con una sesion activa, invalidar token en backend o manipularlo localmente
- Ejecutar una accion que pegue a backend
- Verificar limpieza de sesion
- Verificar redireccion automatica a `/login`
