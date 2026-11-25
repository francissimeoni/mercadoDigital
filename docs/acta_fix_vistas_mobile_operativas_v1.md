# Acta - Fix Vistas Mobile Operativas v1

## Objetivo

Empezar el cierre visual del producto sobre superficies ya operativas, respetando la convencion de priorizar dispositivos moviles.

## Alcance implementado

- mejora mobile-first de la bandeja `RMA`
- mejora mobile-first del detalle `RMA`
- mejora mobile-first de la bandeja de `logistica/envios`
- mejora mobile-first del tracking publico del pedido y su bloque de postventa
- mejora mobile-first del detalle publico de producto
- mejora mobile-first del checkout publico
- mejora mobile-first de la home publica storefront
- mejora mobile-first del catalogo publico storefront
- tarjetas de retiros y despachos mas orientadas a:
  - estado visible
  - siguiente accion
  - seleccion del pedido activo
- resumenes compactos arriba de la vista para leer volumen y foco operativo sin depender de desktop
- detalle del pedido logistico con snapshot rapido del caso antes de bajar al bloque de acciones
- detalle RMA con snapshot rapido del caso antes de entrar a secciones y acciones
- tracking publico con snapshot del pedido y del caso de postventa antes de bajar a la conversacion

## Criterio aplicado

- primero una sola columna util en pantallas chicas
- acciones primarias con targets mas claros
- detalle progresivo y no todo mezclado en el primer bloque
- el diseño sigue funcionando en desktop, pero nace desde mobile

## Pantallas tocadas

- `frontend/src/pages/rma/RmaPage.tsx`
- `frontend/src/pages/logistica/LogisticaEnviosPage.tsx`
- `frontend/src/pages/rma/RmaDetallePage.tsx`
- `frontend/src/storefront/pages/StorefrontPedidoTrackingPage.tsx`
- `frontend/src/storefront/pages/StorefrontProductDetailPage.tsx`
- `frontend/src/storefront/pages/StorefrontCheckoutPage.tsx`
- `frontend/src/storefront/pages/StorefrontHomePage.tsx`
- `frontend/src/storefront/pages/StorefrontCatalogPage.tsx`
- `frontend/src/components/logistica/LogisticaRetiroItem.tsx`
- `frontend/src/components/logistica/LogisticaDespachoItem.tsx`

## Verificacion

- `npm run build` en frontend OK

## Siguiente paso sugerido

- seguir con las vistas operativas que mas se usan de punta a punta
- validar manualmente en ancho chico:
  - bandeja RMA
  - logistica envios
  - detalle RMA
  - tracking publico
  - detalle de producto
  - checkout publico
  - home storefront
  - catalogo storefront
