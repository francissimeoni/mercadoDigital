## Acta - Dashboard Admin MD Maqueta Fase A

Fecha:
- 2026-03-19

Estado:
- implementado frontend
- maqueta navegable base

Objetivo:
- iniciar la Fase A del blueprint admin dejando un modulo `ADMIN_MD` tangible, navegable y profesional
- montar layout, home admin, sidebar completa y placeholders consistentes
- usar datos reales donde ya existe contrato suficiente

## Alcance implementado

Se incorporo una primera base visual y funcional del modulo admin:

- nueva home `/admin`
- sidebar admin completa dentro del layout general
- rutas admin dedicadas
- home con KPIs y bloques operativos
- pantallas reales para:
  - revendedores
  - storefronts
- alias admin para:
  - `RMA`
  - notificaciones
- placeholders profesionales para:
  - operacion
  - pedidos
  - pagos
  - logistica
  - catalogo
  - canonizacion
  - proveedores
  - usuarios y roles
  - reportes
  - auditoria
  - configuracion

## Datos reales reutilizados

La home admin y las bandejas conectadas reutilizan:

- `GET /rma`
- `GET /notificaciones/{usuarioId}`
- `GET /admin/revendedores`

Con eso se construyen:

- KPIs de `RMA` abiertos
- conteo de revendedores activos
- cantidad de storefronts publicadas
- cantidad de notificaciones no leidas
- alertas operativas simples y honestas
- actividad reciente derivada de RMA, notificaciones y actualizaciones de revendedores

## Datos derivados localmente

Se derivan en frontend, sin inventar backend:

- `RMA abiertos` como casos no resueltos ni rechazados
- storefronts publicadas / no publicadas
- alertas de atencion inmediata
- actividad reciente consolidada
- mapa operativo basico

## Decisiones de UX

- el admin deja de caer solo en `RMA` y pasa a tener home propia `/admin`
- la sidebar ya refleja la estructura completa del blueprint
- las pantallas sin contrato suficiente no quedan vacias: muestran estado, objetivo del modulo y siguiente paso recomendado
- se prioriza densidad operativa controlada por encima de decoracion

## Lo que no se implemento en esta fase

- endpoints nuevos
- filtros complejos por modulo
- listados admin de pedidos o pagos
- auditoria real consolidada
- catalogo admin completo
- canonizacion operativa
- configuracion avanzada
- reportes reales

## Archivos principales

- `frontend/src/auth/accessControl.ts`
- `frontend/src/routes/AppRouter.tsx`
- `frontend/src/types/admin.ts`
- `frontend/src/api/adminApi.ts`
- `frontend/src/hooks/useAdmin.ts`
- `frontend/src/pages/admin/AdminDashboardPage.tsx`
- `frontend/src/pages/admin/AdminRevendedoresPage.tsx`
- `frontend/src/pages/admin/AdminStorefrontsPage.tsx`
- `frontend/src/pages/admin/AdminModulePlaceholderPage.tsx`

## Validacion

Ejecutado:

- `npm.cmd run build`

Resultado:

- OK

Observacion:

- Vite mantiene advertencia por chunk mayor a `500 kB`
- no bloquea compilacion

## Evolucion posterior - inicio Fase B

Luego de cerrar esta base se profundizaron algunos modulos de mayor valor inmediato:

- `RMA admin` paso a tener bandeja propia bajo `/admin/rma` con:
  - KPIs
  - filtros locales
  - prioridad visual
  - acceso directo al detalle real
- `canonizacion` dejo de ser placeholder generico y paso a una vista de trabajo v1 con estructura explicita de:
  - producto proveedor
  - candidatos canonicos
  - acciones humanas
- `revendedores` y `storefronts` se reforzaron con:
  - KPIs
  - filtros
  - lectura operativa mas clara

La documentacion detallada de esta profundizacion queda asentada en el contexto de avance del proyecto.
