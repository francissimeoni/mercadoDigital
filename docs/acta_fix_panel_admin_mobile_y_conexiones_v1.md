# Acta - Primera pasada fuerte del panel admin mobile y conexiones v1

Fecha: 2026-04-18

## Objetivo

Dar una primera pasada fuerte al rol `admin` para:

- ordenar mejor la jerarquia visual con prioridad mobile-first
- reducir ruido y redundancia
- conectar mejor las pantallas reales ya existentes
- corregir accesos frontend que todavia quedaban poco claros o errados

## Superficies tocadas

- `frontend/src/pages/admin/AdminDashboardPage.tsx`
- `frontend/src/pages/admin/AdminRevendedoresPage.tsx`
- `frontend/src/pages/admin/AdminRmaPage.tsx`
- `frontend/src/pages/admin/AdminStorefrontsPage.tsx`
- `frontend/src/pages/NotificacionesPage.tsx`
- `frontend/src/auth/accessControl.ts`
- `frontend/src/routes/AppRouter.tsx`

## Cambios principales

### Dashboard admin

- se reorganizo alrededor de foco actual, orden sugerido y accesos reales
- se priorizo lectura rapida en mobile antes que grillas pesadas de escritorio
- se empezaron a usar enlaces internos con contexto hacia `RMA`, `storefronts` y `notificaciones`

### Revendedores

- se hizo mas directo el paso desde la bandeja al editor real de `storefronts`
- ahora el recorrido principal ya no termina en una lista aislada, sino que abre el revendedor correcto para editarlo
- se agrego persistencia ligera de filtro y busqueda por query string

### Storefronts

- ya acepta `revendedorId` por query para abrir seleccionado desde otras vistas admin
- el retorno de OAuth de Mercado Pago preserva mejor ese contexto

### RMA admin

- la bandeja ahora acepta mejor contexto por query:
  - `estado`
  - `search`
  - `open`
- se reorganizo visualmente para mobile y para entrada mas rapida al detalle del caso

### Notificaciones y navegacion

- `NotificacionesPage` ahora diferencia mejor el rol actual:
  - admin
  - proveedor
  - resto
- se corrigio la navegacion global admin que todavia apuntaba a `/rma` en vez de `/admin/rma`
- se removio un CTA admin que llevaba a una ruta no accesible para ese rol

## Resultado

El panel admin ya se siente bastante mas conectado y menos fragmentado:

- menos saltos muertos
- mas enlaces con contexto
- mejor lectura en mobile
- mejor separacion entre lo real y lo aun preparado

## Verificacion

- `npm run build`

## Siguiente paso sugerido

1. rematar placeholders admin con el mismo criterio sin inventar backend
2. pasar despues al rol `proveedor`
