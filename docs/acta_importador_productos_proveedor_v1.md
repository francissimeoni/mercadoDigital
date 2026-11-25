# Acta - Importador productos proveedor v1

Fecha: 2026-03-18

## Objetivo

Implementar un importador backend v1 para que un `PROVEEDOR` cargue masivamente su catalogo desde un archivo real `CSV` o `XLSX`, sin abrir todavia un modulo nuevo de sucursales ni de inventario distribuido.

## Decision de modelo

Se mantiene el modelo ya vigente:

- `Producto` sigue representando la oferta concreta del proveedor
- no se crea `ProductoCanonico`
- no se crea un modulo de stock por sucursal
- el backend sigue siendo fuente de verdad

El stock se consolida en v1 como:

- `stock = ROS + MZA + CBA + LUG`

## Endpoint creado

Bajo `/proveedores/**`:

- `POST /proveedores/{proveedorId}/productos/importacion`

Request:

- `multipart/form-data`
- campo `archivo`

Formatos soportados:

- `.csv`
- `.xlsx`

## Mapeo implementado

- `Codigo` -> `sku`
- `Descripcion` -> `nombre`
- `Descripcion` tambien se persiste en `descripcion` para no perder texto completo del archivo
- `precio` -> `precioCosto`
- `ROS + MZA + CBA + LUG` -> `stock`

Campos auxiliares del archivo:

- `IVA`
- `Grupo`
- `Rubro`
- `Part Number`

En esta v1 no bloquean la importacion si no existe un campo persistente donde guardarlos.

## Reglas operativas

- el proveedor solo importa sobre sus propios productos
- `ADMIN_MD` conserva acceso total por la regla ya existente del modulo
- la importacion hace `upsert` por `sku` dentro del proveedor
- si el producto ya existe:
  - actualiza `nombre`
  - actualiza `descripcion`
  - actualiza `precioCosto`
  - actualiza stock consolidado
  - lo deja `activo=true`
  - conserva `productoCanonicoId` si ya estaba asociado
  - conserva `categoriaPaquete` si ya estaba definida
- si el producto no existe:
  - crea `Producto` nuevo
  - deja `productoCanonicoId = null`
  - usa `categoriaPaquete = CHICO` como fallback operativo v1

## Respuesta

Se devuelve un resumen con:

- `filasLeidas`
- `creados`
- `actualizados`
- `rechazados`
- `errores`

Cada error de fila informa:

- `fila`
- `sku`
- `mensaje`

## Implementacion tecnica

Se agregaron:

- `ProveedorProductoImportService`
- `ProveedorProductoImportResponse`
- `ProveedorProductoImportErrorResponse`

Y se extendio:

- `ProveedorController`
- `ProductoRepository`
- `pom.xml` con soporte de parseo `CSV/XLSX`

La importacion se procesa fila por fila con transaccion independiente para que un error puntual no invalide todo el archivo.

## Decisiones tomadas

- no se toco frontend
- no se abrio modulo de sucursales
- no se invento canonizacion
- no se agregaron migraciones de modelo porque v1 reutiliza tablas existentes
- no se desactivan automaticamente productos ausentes del archivo en esta v1

## Validacion

Comando ejecutado:

- `./mvnw -q -DskipTests compile`

## Pendientes para v2

- desactivacion o conciliacion de productos ausentes del archivo
- soporte de metadata auxiliar persistente (`IVA`, `Grupo`, `Rubro`, `Part Number`) si el negocio lo necesita
- heuristica o dato real para `categoriaPaquete` en vez de fallback fijo
- soporte de stock por sucursal como modulo explicito
- reportes mas ricos de importacion con preview/dry-run
