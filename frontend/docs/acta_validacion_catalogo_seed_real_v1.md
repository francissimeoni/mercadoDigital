# Acta - Validacion catalogo seed real v1

Fecha: 2026-03-17

## Objetivo

Dejar documentada la puesta a punto del catalogo publico del storefront con datos de prueba alineados al modelo real del marketplace y validar que los productos cargados se vean correctamente en la tienda white-label.

## Contexto

Durante la validacion del catalogo publico se detecto que en la base local solo existia 1 `producto` y 1 `producto_canonico`, por lo que el storefront mostraba un solo item visible.

Se confirmo que el circuito correcto del negocio es:

- proveedor carga productos
- esos productos se vinculan al catalogo canonico
- se registra stock del proveedor
- se registra markup del revendedor
- el storefront publico muestra el precio final resuelto por backend

## Cambios realizados

- Se mantuvieron en frontend las mejoras ya cerradas para:
  - `/`
  - `/catalogo`
  - `/producto/:productoCanonicoId`
- Se agrego descubrimiento basico en `/catalogo` con:
  - busqueda por nombre publico
  - filtro de disponibilidad
  - orden simple
  - persistencia por query params (`q`, `stock`, `sort`)
- Se detecto que el problema de visibilidad no estaba en el frontend sino en la carga real de datos de la base local.
- Se creo el script [seed_catalogo_proveedor_50_articulos.sql](/c:/Users/Usuario/Desktop/MercadoDigital/backend/scripts/seed_catalogo_proveedor_50_articulos.sql) para cargar 50 articulos en el circuito real:
  - `proveedores`
  - `productos_canonicos`
  - `productos`
  - `proveedor_stock`
  - `revendedor_markups`
  - `producto_media`
- Se ajusto la validacion final del script para evitar el error MySQL `Can't reopen table` sobre la tabla temporal de seed.

## Decisiones tomadas

- No se toco checkout publico ni PSP.
- No se agregaron endpoints nuevos.
- No se movio logica de negocio al frontend.
- La visibilidad del catalogo se resolvio respetando el modelo real proveedor -> catalogo -> storefront.
- La busqueda/filtros del catalogo siguen siendo frontend-only en esta etapa porque `GET /public/catalogo` hoy solo soporta `revendedorId`.

## Validacion realizada

- Se ejecuto la carga del script SQL de seed real sobre la base local.
- Se corrigio el bloque final de verificacion del script para compatibilidad con MySQL.
- Se verifico que los productos cargados ahora ingresan correctamente al catalogo publico.
- Se confirmo visualmente que el storefront muestra los productos cargados OK.
- El catalogo publico mantiene operativos:
  - cards con imagen/fallback
  - detalle publico
  - busqueda
  - filtro por stock
  - orden simple

## Resultado

El storefront publico ya muestra correctamente el catalogo poblado con los productos cargados en base a traves del proveedor y su vinculacion canonica, alineado con el circuito real del marketplace.

## Limitaciones vigentes

- La busqueda, filtros y orden aun no se resuelven server-side.
- La carga de catalogo de prueba sigue siendo por script SQL de desarrollo y no por una superficie operativa completa de alta de proveedor.

## Conclusion

Queda validado que el problema era de datos locales y no de render del storefront. Con la carga real de articulos alineada al modelo de negocio, el catalogo publico funciona correctamente y los productos ya se visualizan en la tienda white-label como esperado.
