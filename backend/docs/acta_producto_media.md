# Acta: Soporte de Media para Productos

## Migracion

Se agrega la migracion Flyway:
- `V8__producto_media.sql`

Tabla nueva: `producto_media`
- `id` (PK)
- `producto_id` (FK -> `productos.id`)
- `tipo_media` (`IMAGEN` | `VIDEO`)
- `url`
- `orden`
- `es_principal`
- `created_at`
- `updated_at`

Indice:
- `idx_producto_media_producto` sobre `producto_id`

## Modelo de dominio

Se incorpora:
- Entidad `ProductoMedia`
- Enum `TipoMedia` (`IMAGEN`, `VIDEO`)
- Repositorio `ProductoMediaRepository`

Metodos principales:
- `findByProducto_IdOrderByOrdenAsc`
- `findByProducto_IdAndEsPrincipalTrue`

## Servicio

`ProductoMediaService` expone:
- `addMedia(productoId, tipoMedia, url, orden, esPrincipal)`
- `getMedia(productoId)`
- `deleteMedia(mediaId)`

Reglas implementadas:
- Si `esPrincipal=true`, desmarca cualquier media principal anterior del producto.
- Si `orden` no se informa, se asigna en base al maximo orden + 1.
- Si producto/media no existe, responde `404` controlado.

## Endpoints

- `POST /productos/{id}/media`
- `GET /productos/{id}/media`
- `DELETE /media/{id}`

## Catalogo

Se agrega `imagenPrincipal` en `CatalogoItemResponse`.

Resolucion de imagen:
1. Buscar `ProductoMedia` principal (`esPrincipal=true`) del producto de la mejor oferta.
2. Si no existe, tomar la primera `IMAGEN` por `orden`.
3. Si no hay imagen, devolver `null`.

## Tests

Se agregan:
- `ProductoMediaServiceTest`
- `CatalogoMediaTest`

Se actualiza `CatalogoServiceTest` para contemplar el nuevo repositorio de media.
