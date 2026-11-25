# Acta - Categorias de producto admin + proveedor v1

Fecha: 2026-04-27

## Objetivo

Incorporar una taxonomia comercial de categorias de producto separada de `categoriaPaquete` (logistica), con este flujo:

- admin gestiona categorias comerciales
- proveedor selecciona categoria al crear/editar producto
- si no existe, proveedor puede sugerirla desde el mismo flujo

## Alcance implementado

### Backend

- nueva tabla `categorias_producto` y relacion opcional en `productos`:
  - migracion `V26__categorias_producto_y_vinculo_en_producto.sql`
- nuevo dominio:
  - entidad `CategoriaProducto`
  - repositorio `CategoriaProductoRepository`
  - servicio `CategoriaProductoService`
- nuevas APIs admin bajo `/admin/configuracion`:
  - `GET /categorias-producto`
  - `POST /categorias-producto`
  - `PUT /categorias-producto/{id}`
- nuevas APIs proveedor:
  - `GET /proveedores/{proveedorId}/categorias-producto` (solo activas)
  - `POST /proveedores/{proveedorId}/categorias-producto/sugerencias`
- productos proveedor ahora soportan categoria comercial:
  - request `ProveedorProductoUpsertRequest` agrega `categoriaProductoId`
  - response `ProveedorProductoResponse` agrega:
    - `categoriaProductoId`
    - `categoriaProductoNombre`

### Frontend

- admin configuracion (`/admin/configuracion`) suma bloque de categorias comerciales:
  - alta de categoria
  - listado con estado
  - activar / desactivar
- proveedor productos (`/proveedor/productos`) ahora:
  - permite seleccionar categoria comercial en el formulario
  - permite sugerir categoria nueva sin salir del alta
  - mantiene `categoriaPaquete` como dato logistico independiente

## Validacion tecnica

- backend tests:
  - `AdminConfiguracionControllerTest` OK
  - `ProveedorControllerTest` OK
  - `ProveedorProductoServiceTest` OK
- frontend build:
  - `npm run build` OK

## Impacto funcional

- ahora existe una categoria comercial administrable desde admin para productos proveedor.
- proveedor deja de depender de valores hardcodeados para categoria comercial.
- si una categoria no existe, se puede sugerir en runtime y queda registrada para revision/activacion desde admin.

## Estado

- categorias comerciales admin/proveedor v1: **operativo**
