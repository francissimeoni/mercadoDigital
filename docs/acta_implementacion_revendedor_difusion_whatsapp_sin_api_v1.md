# Acta - implementacion revendedor difusion WhatsApp sin API v1

Fecha: 2026-04-27

## Objetivo

Ejecutar la fase MVP de difusion comercial del revendedor sin API oficial de WhatsApp:

- boton `Compartir` (Web Share cuando exista)
- fallback `wa.me` sin costo
- armado de lista/presupuesto compartible por link

## Alcance implementado

### 1) Red comercial conectable con datos reales

La vista `Red comercial` dejo de ser solo placeholder y ahora consume preview real de pricing:

- productos canonizados visibles para revendedor
- filtro comercial
- precio final backend
- stock disponible
- imagen principal cuando existe

### 2) Compartir producto por WhatsApp sin API paga

Por producto se agrego:

- accion `Compartir`
- mensaje prearmado con nombre + precio + entrega + link de compra
- flujo:
  - primero `navigator.share`
  - si no existe soporte, fallback a `https://wa.me/?text=...`

No se usa API oficial de WhatsApp.

### 3) Lista/presupuesto compartible

Se agrego seleccion de productos y generacion de link de lista:

- agregar/quitar productos a presupuesto
- link compartible de catalogo con `?lista=id1,id2,...`
- soporte de `asesor` opcional por query param
- accion de compartir lista con el mismo esquema Web Share / `wa.me`

### 4) Catalogo publico compatible con lista compartida

`/catalogo` ahora soporta:

- filtro por `lista` (productoCanonicoId)
- banner de contexto de lista compartida
- preservacion de `lista`, `asesor` y `storefrontHost` al limpiar filtros de busqueda

## Decisiones tecnicas

- sin backend nuevo para este MVP
- reutilizacion de `pricing-v1/preview` para evitar duplicar logica
- compartir imagen indirectamente via previsualizacion de link (metadatos de pagina)
- se deja para etapa siguiente:
  - entidad persistida de presupuesto
  - expiracion/versionado de link
  - atribucion fuerte de asesor por id (no solo slug por query)

## Archivos tocados

- `frontend/src/pages/RevendedorRedComercialPage.tsx`
- `frontend/src/storefront/pages/StorefrontCatalogPage.tsx`

## Validacion tecnica ejecutada

- `frontend`: `npm run build` OK

