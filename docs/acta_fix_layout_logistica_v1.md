# Acta - fix layout logistica v1

Fecha: 2026-04-07

## Situacion detectada

La UI de logistica se veia desfasada en notebooks y pantallas medianas dentro del `MainLayout` autenticado.

Causa tecnica probable:

- varios grids pasaban a `xl` demasiado temprano
- el breakpoint se calculaba sobre el viewport completo, pero la pantalla real ya venia recortada por la sidebar izquierda
- eso comprimía columnas y subgrids dentro de las tarjetas de retiros, despachos y dashboard

## Ajuste aplicado

Se hicieron mas conservadores los breakpoints del modulo:

- los resumenes y grillas principales de logistica pasan a 4 columnas o layouts amplios recien en `2xl`
- la bandeja operativa principal de `LogisticaEnviosPage` ya no fuerza 3 columnas anchas en `xl`
- las tarjetas de retiro y despacho ya no fuerzan 4 celdas internas en `xl`
- el detalle de envio ahora distribuye su bloque de metadata en 2 columnas solo cuando hay ancho real suficiente

## Archivos tocados

- `frontend/src/pages/logistica/LogisticaEnviosPage.tsx`
- `frontend/src/pages/logistica/LogisticaDashboardPage.tsx`
- `frontend/src/components/logistica/LogisticaRetiroItem.tsx`
- `frontend/src/components/logistica/LogisticaDespachoItem.tsx`

## Resultado esperado

- mejor estabilidad visual en notebooks y anchos medios
- menos compresion horizontal en tarjetas y columnas internas
- lectura mas clara de la bandeja operativa sin romper el flujo ya validado

## Verificacion tecnica

- `npm run build` en frontend: OK

## Estado posterior

- checkout persistido: corregido
- layout de logistica: ajuste tecnico aplicado, listo para validacion visual/manual final
