# Acta MT7 - Storefront publico v1

## Objetivo

Implementar la primera superficie publica white-label del frontend, consumiendo `TenantContext` y el catalogo publico real del backend, sin afectar la app operativa autenticada.

## Archivos agregados o modificados

Agregados:

- `src/storefront/types/storefrontCatalogo.ts`
- `src/storefront/api/storefrontCatalogoApi.ts`
- `src/storefront/hooks/useStorefrontCatalogo.ts`
- `src/storefront/components/StorefrontHeader.tsx`
- `src/storefront/components/StorefrontHero.tsx`
- `src/storefront/components/StorefrontProductCard.tsx`
- `src/storefront/components/StorefrontProductGrid.tsx`
- `src/storefront/components/StorefrontFooter.tsx`
- `src/storefront/components/StorefrontEmptyState.tsx`
- `src/storefront/components/StorefrontErrorState.tsx`
- `src/storefront/components/StorefrontCatalogoSkeleton.tsx`
- `src/storefront/layout/PublicStoreLayout.tsx`
- `src/storefront/pages/StorefrontHomePage.tsx`
- `src/storefront/pages/StorefrontCatalogPage.tsx`
- `src/storefront/pages/StorefrontProductDetailPage.tsx`
- `src/routes/HomeRouteGate.tsx`
- `src/routes/CatalogoRouteGate.tsx`
- `src/routes/AppFallbackRoute.tsx`

Modificados:

- `src/routes/AppRouter.tsx`
- `src/components/layout/MainLayout.tsx`

## Rutas publicas incorporadas

- `/`
- `/catalogo`
- `/producto/:productoCanonicoId`

## Convivencia con rutas operativas

Se cerro la convivencia asi:

- `/`
  - sin sesion -> home publica
  - con sesion -> redirect operativo segun rol
- `/catalogo`
  - sin sesion -> catalogo publico
  - con sesion -> catalogo operativo existente
- `/producto/:productoCanonicoId`
  - publico, sin conflicto con rutas operativas actuales

La app autenticada sigue bajo `ProtectedRoute` + `MainLayout`.

## Layout publico creado

Se creo `PublicStoreLayout`.

Responsabilidades:

- leer `TenantContext`
- mostrar `loading`
- mostrar `local_unresolved`
- mostrar `not_available` / error de red
- renderizar header y footer publicos
- mantener separacion total respecto de `MainLayout`

## Componentes base creados

- `StorefrontHeader`
- `StorefrontHero`
- `StorefrontProductCard`
- `StorefrontProductGrid`
- `StorefrontFooter`
- `StorefrontEmptyState`
- `StorefrontErrorState`
- `StorefrontCatalogoSkeleton`

## Integracion con TenantContext

Toda la superficie publica usa:

- `revendedorId` desde `TenantContext`
- `branding` desde `TenantContext`

No se uso query params, localStorage ni variables `VITE_*` como fuente principal del `revendedorId` publico.

## Endpoints publicos consumidos

- `GET /public/catalogo?revendedorId=...`
- `GET /public/catalogo/{productoCanonicoId}?revendedorId=...`

## Fallbacks visuales adoptados

- si falta `nombreComercial`, se usa `Mercado Digital`
- si falta `heroTitulo`/`heroSubtitulo`, se usa copy operativo neutro
- si falta `logoUrl`, se usa avatar textual
- si falta `heroImagenUrl`, se usa bloque visual sobrio con texto
- si falta `colorPrimario`, se usa fallback oscuro del proyecto
- si falta `footerTexto` o contacto comercial, la UI sigue renderizando sin romperse

## Estrategia para home publica

No se invento endpoint de destacados.

La home consulta el catalogo publico real y toma una seleccion corta con `slice(0, 3)` solo para presentacion visual.

## Comportamiento en `local_unresolved` y `not_available`

- `local_unresolved`
  - se muestra estado explicito para entorno local
  - no se rompe la app
- `not_available`
  - se muestra estado publico de tienda no disponible
  - no se exponen detalles internos adicionales

## Impacto sobre la arquitectura frontend actual

- Se agrega una capa `src/storefront` claramente separada.
- `PublicStoreLayout` no reutiliza `MainLayout`.
- La app operativa autenticada se mantiene intacta.
- Queda preparado el terreno para MT8/MT9 sin mezclar aun carrito, pedido ni login cliente final.

## Validacion

Se valido por compilacion:

- `npm run build`

## Pendiente para la siguiente etapa

- carrito publico
- pedido/checkout publico
- login y registro cliente final
- panel cliente
- refinamiento visual del storefront
- filtros o navegacion comercial mas rica cuando backend lo soporte
