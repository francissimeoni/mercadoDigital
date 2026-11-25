# Acta - Storefront proveedor base v1

Fecha: 2026-05-07

## Objetivo

Abrir la base técnica para soportar dos tipos de storefront públicos:

- `REVENDEDOR` (flujo actual con canonización y mejor oferta)
- `PROVEEDOR` (catálogo propio, sin selección cruzada entre proveedores)

## Cambios implementados

### 1) Modelo de storefront extendido

Se reutiliza la tabla `revendedor_storefront` como contenedor de storefront público, incorporando:

- `tipo_storefront` (`REVENDEDOR` / `PROVEEDOR`)
- `dominio_publico`
- `proveedor_id` (owner cuando el storefront es de proveedor)

Migración Flyway:

- `backend/src/main/resources/db/migration/V31__storefront_tipo_y_proveedor.sql`

Entidad:

- `RevendedorStorefront` agrega:
  - `tipoStorefront`
  - `dominioPublico`
  - `proveedor`
- `revendedor` pasa a opcional para soportar storefront de proveedor.

### 2) Resolución pública por host

`StorefrontService.resolverPorHost(...)` ahora:

- resuelve por `dominio_publico` del storefront,
- retorna metadatos con `tipoStorefront`,
- preserva compatibilidad legacy (fallback a `revendedores.dominio`).

`StorefrontResponse` ahora incluye:

- `tipoStorefront`
- `proveedorId` (cuando aplica)
- `revendedorId` (cuando aplica)

### 3) Validaciones públicas por tipo

`StorefrontService` incorpora:

- `validarRevendedorPublico(...)`
- `validarProveedorPublico(...)`

con control de:

- entidad activa,
- storefront publicada,
- coherencia con el tipo de storefront.

### 4) Catálogo público proveedor (sin canonización)

Se agregan endpoints públicos para catálogo propio proveedor:

- `GET /public/catalogo/proveedor?proveedorId=...`
- `GET /public/catalogo/proveedor/{productoId}?proveedorId=...`

Implementación:

- `PublicCatalogoService` y `CatalogoService`
- se consume `Producto` del proveedor owner,
- no se ejecuta lógica de “mejor oferta entre proveedores”.

DTOs públicos ahora contemplan `productoId`:

- `PublicCatalogoItemResponse`
- `PublicProductoDetalleResponse`

## Alcance de esta etapa

Esta etapa deja lista la base estructural multi-tipo para storefront y catálogo proveedor.

No cierra todavía:

- checkout público proveedor end-to-end,
- pricing comercial propio configurable por proveedor (markup explícito),
- alta/edición de storefront proveedor desde panel admin.

## Validación

- `backend`: `./mvnw -q -DskipTests compile` en verde.

