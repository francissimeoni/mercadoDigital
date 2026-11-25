# Acta - Proveedor alta de producto con imagenes en el mismo flujo v1

Fecha: 2026-04-27

## Objetivo

Eliminar la friccion del alta manual de productos proveedor, permitiendo cargar hasta 3 imagenes opcionales en el mismo paso de creacion, sin depender de guardar primero y luego ir al bloque separado de media.

## Alcance implementado

### Backend

- `ProveedorProductoUpsertRequest` ahora admite `imagenes` opcional:
  - lista de URLs
  - maximo 3 elementos
- `ProveedorProductoService` ahora:
  - valida `imagenes` (maximo 3, sin vacios, esquema `http/https`)
  - al crear producto, persiste stock y luego adjunta automaticamente las imagenes como media tipo `IMAGEN`
  - define la primera imagen como principal y el resto con orden correlativo

### Frontend

- `ProveedorProductosPage` ahora permite, en modo alta:
  - cargar hasta 3 URLs de imagen opcionales en el mismo formulario de creacion
  - enviar esas URLs junto al `POST /proveedores/{proveedorId}/productos`
- se limpia el estado de URLs de alta al crear producto o al reiniciar formulario.

## Validacion tecnica

- backend tests:
  - `ProveedorControllerTest` OK
  - `ProveedorProductoServiceTest` OK
  - se agrego cobertura para:
    - adjuntar imagenes iniciales en alta
    - rechazar mas de 3 imagenes
- frontend build:
  - `npm run build` OK

## Impacto funcional

- el proveedor puede publicar un producto nuevo con imagenes iniciales en una sola operacion.
- se reduce tiempo operativo y pasos manuales repetitivos en carga diaria.
- no se rompe el circuito existente de media manual posterior (agregar/eliminar desde el bloque de media sigue disponible).

## Estado

- pendiente "alta proveedor con imagenes en el mismo flujo": **cerrado v1**
