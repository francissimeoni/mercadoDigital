# Acta - Backend productos proveedor v1

Fecha: 2026-03-18

## Decision de modelo

Se confirma que `Producto` ya representa la oferta concreta del proveedor dentro del marketplace.

Por lo tanto, en esta v1:

- no se crea un nuevo concepto `OfertaProveedor`
- el proveedor gestiona su `Producto`
- la relacion con `ProductoCanonico` se mantiene nullable
- el estado de normalizacion se deriva asi:
  - `ASOCIADO` si `productoCanonicoId` existe
  - `PENDIENTE` si `productoCanonicoId` es null

El proveedor no crea producto canonico directamente.

## Endpoints creados

Bajo `/proveedores/**`:

- `GET /proveedores/{proveedorId}/productos`
- `POST /proveedores/{proveedorId}/productos`
- `PUT /proveedores/{proveedorId}/productos/{id}`
- `GET /proveedores/{proveedorId}/productos/{id}/media`
- `POST /proveedores/{proveedorId}/productos/{id}/media`
- `DELETE /proveedores/{proveedorId}/productos/{id}/media/{mediaId}`

## Reglas de seguridad

- No se abrieron `/productos/**`, `/productos-canonicos/**` ni `/media/**` al proveedor.
- Se mantiene el acceso administrativo total para `ADMIN_MD`.
- Se agrega ownership real para proveedor mediante relacion `Proveedor -> Usuario`.
- Un `PROVEEDOR` solo puede operar sobre el `proveedorId` cuyo usuario autenticado coincide con el asociado al proveedor.
- Si intenta acceder a otro proveedor, el recurso se trata como no encontrado.

## Soporte implementado

### Productos del proveedor

- listado de productos/ofertas del proveedor
- alta de producto/oferta del proveedor
- edicion basica de:
  - `sku`
  - `nombre`
  - `descripcion`
  - `precioCosto`
  - `stock`
  - `categoriaPaquete`
  - `activo`
  - `productoCanonicoId` nullable

### Media propia del proveedor

- lectura de media de sus productos
- alta de media sobre sus productos
- eliminacion de media sobre sus productos

Se reutiliza el modelo actual de `producto_media`, incluyendo:

- multiples imagenes
- video opcional si entra en el flujo actual
- `orden`
- `esPrincipal`

La ausencia de media no impide crear el producto.

## DTOs incorporados

- `ProveedorProductoUpsertRequest`
- `ProveedorProductoResponse`

La respuesta proveedor expone datos utiles para gestion:

- `productoCanonicoId`
- `productoCanonicoNombre`
- `estadoNormalizacion`
- `sku`
- `nombre`
- `precioCosto`
- `stockDisponible`
- `activo`
- resumen de media:
  - cantidad
  - imagen principal
  - presencia de video

## Ajustes estructurales

- migracion `V13__proveedor_productos_v1.sql`
- relacion `Proveedor.usuario`
- validacion de ownership en `ProveedorOwnershipService`
- contrato operativo en `ProveedorProductoService`

## Tests cubiertos

- provider owner puede resolver su proveedor
- proveedor no puede operar sobre otro proveedor
- admin puede acceder
- proveedor puede crear y editar sus productos
- media se restringe al producto propio
- seguridad web para endpoints nuevos

## Pendiente para siguientes hilos

- carga por Excel
- flujo de normalizacion canonica administrado por MD
- detalle mas rico del producto/oferta para proveedor
- validaciones de negocio mas avanzadas sobre duplicidad o conciliacion canonica
- media completa con reorder mas sofisticado si hace falta

## Conclusion

Queda implementado el contrato backend minimo para que un proveedor gestione sus productos/ofertas sin romper el modelo canonico existente ni abrir permisos genericos sobre modulos reservados a `ADMIN_MD`.
