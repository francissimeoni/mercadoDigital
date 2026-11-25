# Acta MT6.1 - Branding DOM Sync

## Objetivo

Implementar un ajuste incremental sobre MT6 para sincronizar el branding del tenant actual con el DOM del navegador, sin construir todavia el storefront publico completo ni afectar la app operativa actual.

## Archivos agregados o modificados

Modificados:

- `src/tenant/TenantContext.tsx`
- `index.html`

## Campos de branding usados

La sincronizacion usa estos campos del tenant actual:

- `branding.nombreComercial` -> `document.title`
- `branding.faviconUrl` -> favicon
- `branding.colorPrimario` -> `meta[name="theme-color"]`

## Politica de fallback

Fallbacks aplicados:

- `document.title`
  - si existe `nombreComercial`, se usa ese valor
  - si no, fallback a `Mercado Digital`
- favicon
  - si existe `faviconUrl`, se usa ese valor
  - si no, fallback a `/vite.svg`
- `theme-color`
  - si existe `colorPrimario`, se usa ese valor
  - si no, fallback a `#0f172a`

Adicionalmente:

- si no existe `meta[name="theme-color"]`, se crea
- si no existe `link[rel="icon"]`, se crea

## Comportamiento ante errores y local/dev

- si el tenant se resuelve correctamente, el DOM se sincroniza con el branding recibido
- si el storefront no esta disponible, hay error de red o el contexto queda en `local_unresolved`, se aplican los fallbacks del proyecto
- en local/dev se mantiene la politica de MT6:
  - `localhost`, `127.0.0.1` y `[::1]` no se toman como tenant publico
  - puede usarse `VITE_STOREFRONT_HOST`
  - si no hay override, no se rompe la app y el navegador queda con branding fallback

## Impacto sobre la arquitectura frontend actual

- la sincronizacion vive dentro de la capa `tenant`
- no se mezcla con `AuthContext`
- no se alteran `MainLayout`, `ProtectedRoute` ni rutas operativas
- el efecto es global solo a nivel visual del navegador

## Validacion

Se valido por compilacion:

- `npm run build`

## Pendiente para MT7

- aplicar branding tambien sobre la UI publica real
- definir favicon/titulo mas propios del proyecto en lugar del placeholder tecnico actual
- usar `TenantContext` para el storefront visual completo
- evaluar sincronizacion adicional de metadatos publicos si el storefront lo requiere
