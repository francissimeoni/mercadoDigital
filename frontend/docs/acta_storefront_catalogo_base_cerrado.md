# Acta - Storefront catalogo base cerrado

Fecha: 2026-03-17

## Objetivo

Cerrar el modulo base del catalogo publico del storefront en `/`, `/catalogo` y `/producto/:productoCanonicoId` con foco operativo y visual base, manteniendo backend como fuente de verdad y sin tocar checkout ni PSP.

## Cambios realizados

- Se pulio la navegacion publica con un `header` mas consistente entre mobile y desktop.
- Se amplio el layout publico para darle mejor continuidad visual entre home, catalogo, detalle y footer.
- Se mejoro la home del storefront con una entrada comercial minima al catalogo y bloques de contexto operativo.
- Se reforzaron las cards del catalogo con:
  - imagen principal con fallback seguro
  - nombre publico
  - precio final estimado
  - demora estimada
  - stock disponible
  - CTA a detalle y CTA a compra
- Se incorporo un componente reutilizable de imagen para fallback ante ausencia o error de carga.
- Se ajusto el skeleton del catalogo para reflejar mejor la estructura real de las cards.
- Se mantuvieron los estados de loading, error con reintento y vacio en home y catalogo.
- Se actualizo el detalle publico del producto para usar el mismo fallback visual de imagen y presentar mejor los datos operativos.

## Pantallas impactadas

- `/`
- `/catalogo`
- `/producto/:productoCanonicoId`

## Decisiones tomadas

- Se priorizo consistencia visual y claridad operativa antes que refinamientos esteticos avanzados.
- Se reutilizo la logica existente de TenantContext, hooks y endpoints publicos.
- No se recalcularon precios, stock ni estados en frontend.
- El CTA de compra reutiliza el flujo ya existente hacia `/producto/:productoCanonicoId/checkout` sin modificar checkout.

## Restricciones respetadas

- No se toco checkout publico ni PSP.
- No se modifico la logica actual de pedido o pago.
- No se agregaron endpoints nuevos.
- No se duplico logica de negocio del backend.
- Los cambios quedaron acotados al frontend publico del storefront.

## Validacion manual realizada

- Se verifico la navegacion soportada por rutas entre `/`, `/catalogo` y `/producto/:productoCanonicoId`.
- Se reviso que home y catalogo mantengan estados de loading, error con reintento y vacio.
- Se dejo fallback de imagen reutilizable para cards y detalle cuando no haya `imagenPrincipal` o falle su carga.
- Se valido compilacion con `npm.cmd run build`.

## Deuda pendiente detectada

- No hay buscador ni filtros de catalogo en esta etapa.
- La home publica muestra destacados fijos por recorte del catalogo cargado; no existe aun criterio editorial configurable desde backend.
- No hay test visual automatizado para responsive; la validacion sigue siendo manual en desarrollo.

## Conclusion

El modulo base del catalogo publico queda cerrado para el seed de desarrollo actual, con navegacion clara, cards mas robustas, estados operativos cubiertos y consistencia razonable entre mobile y desktop, sin afectar el flujo publico de checkout existente.
