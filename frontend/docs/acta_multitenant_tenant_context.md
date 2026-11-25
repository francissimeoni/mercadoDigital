# Acta MT6 - Tenant Context en frontend

## Objetivo

Implementar la etapa MT6 de multitenant v1 incorporando una capa frontend para resolver y exponer el tenant/storefront actual a partir del dominio, sin construir todavia el storefront visual completo ni afectar la app operativa autenticada.

## Archivos agregados o modificados

Agregados:

- `src/tenant/tenantTypes.ts`
- `src/tenant/tenantApi.ts`
- `src/tenant/TenantContext.tsx`
- `src/tenant/useTenant.ts`

Modificado:

- `src/main.tsx`

## Endpoint consumido

La capa consume el endpoint backend ya existente:

- `GET /public/storefront/resolve?host=...`

## Politica de resolucion de host

Fuente principal:

- `window.location.hostname`

Normalizacion aplicada:

- trim
- lowercase
- remocion de protocolo si apareciera por error
- remocion de path
- remocion de puerto
- remocion de punto final

Politica en local/dev:

- si el host real es `localhost`, `127.0.0.1` o `[::1]`, no se usa como tenant publico
- en ese caso se admite fallback opcional con `VITE_STOREFRONT_HOST`
- si no existe ese override, el contexto queda en estado `local_unresolved` y no bloquea la app

Esta decision evita usar query params o localStorage como fuente primaria del tenant publico.

## Shape del contexto tenant

El `TenantContext` expone:

- `status`
- `isLoading`
- `error`
- `storefront`
- `revendedorId`
- `branding`
- `host`
- `source`
- `isAvailable`
- `refresh`

`storefront` conserva el contrato publico resuelto desde backend.

`branding` expone un subset listo para UI:

- `nombreComercial`
- `logoUrl`
- `faviconUrl`
- `colorPrimario`
- `colorSecundario`
- `heroTitulo`
- `heroSubtitulo`
- `heroImagenUrl`
- `emailComercial`
- `telefonoComercial`
- `direccionComercial`
- `footerTexto`

## Manejo de errores

Casos contemplados:

- storefront no disponible (`404`) -> `error.kind = not_available`
- host invalido (`400`) -> `error.kind = invalid_host`
- error de red/backend -> `error.kind = network`
- entorno local sin dominio publico resuelto -> `status = local_unresolved`

La capa no rompe el render de la aplicacion actual: expone estado y error, pero no bloquea children.

## Donde se monta el provider

`TenantProvider` se monta en `src/main.tsx`, por encima de `AuthProvider`.

Motivo:

- queda separado de autenticacion
- queda disponible para futuras rutas publicas sin rehacer bootstrap
- no modifica el shell autenticado actual

## Alcance sobre la app actual

La capa se monta para toda la app, pero en esta etapa funciona como infraestructura reusable.

No se reescribieron todavia:

- `MainLayout`
- `ProtectedRoute`
- rutas operativas autenticadas
- pantallas que hoy usan query/localStorage/env para contexto operativo

Eso queda pendiente para etapas posteriores del storefront publico y del ajuste de contexto de negocio.

## Validacion y tests

No se agregaron tests automatizados frontend en esta etapa porque el proyecto hoy no tiene runner/configuracion de tests montada en `package.json`.

Se valido por compilacion:

- `npm run build`

La cobertura automatizada del contexto tenant queda pendiente hasta incorporar infraestructura de tests frontend consistente con el proyecto.

## Impacto sobre la arquitectura frontend actual

- Se agrega una nueva capa transversal `src/tenant`.
- Se mantiene separada de `src/auth`.
- No se altera la arquitectura operativa ya existente.
- Queda preparada la base para MT7, donde el storefront publico podra consumir `revendedorId` y branding desde contexto en lugar de derivarlo de fuentes manuales.

## Pendiente para la siguiente etapa

- rutas publicas reales del storefront
- UI publica white-label
- uso del `TenantContext` en catalogo publico
- actualizacion dinamica de titulo/favicon/branding visual
- eventual reduccion de dependencias operativas basadas en query/localStorage/env donde corresponda
