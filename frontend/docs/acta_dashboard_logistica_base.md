## Acta - Dashboard Logistica Base

Fecha:
- 2026-03-19

Estado:
- implementado frontend

Objetivo:
- convertir al rol `LOGISTICA_MD` en un modulo con home propia, navegacion coherente y bandejas operativas mas profesionales
- dejar visible que partes ya estan conectadas, cuales quedan parciales y cuales siguen pendientes

## Rutas incorporadas o ajustadas

Nuevas rutas:

- `/logistica`
- `/logistica/seguimiento`
- `/logistica/incidencias`
- `/logistica/historial`

Rutas existentes mantenidas:

- `/logistica/envios`

Cambio funcional de home por rol:

- `LOGISTICA_MD` ahora entra naturalmente por `/logistica`

## Partes conectadas a backend real

Se reutilizan contratos ya existentes para:

- `GET /logistica/retiros-pendientes`
- `GET /logistica/despachos-pendientes`
- `POST /envios/{pedidoId}/retirado`
- `POST /envios/{pedidoId}/despachar`
- `POST /envios/{pedidoId}/entregar`
- `GET /envios/{pedidoId}`
- `GET /envios/{pedidoId}/rotulo`
- `GET /logistica/transportistas`
- `POST /logistica/transportistas`

Superficies reales resultantes:

- dashboard `/logistica` con resumen operativo usando retiros y despachos reales
- bandeja `/logistica/envios` reorganizada para:
  - retiros pendientes
  - despachos pendientes
  - operacion por pedido
  - selector de transportista
  - alta inline de transportista
  - tracking
  - despacho
  - entrega
  - descarga de rotulo

## Partes parciales

- tarjeta `En transito` del dashboard:
  - visible
  - honesta
  - sin numero inventado
- pantalla `/logistica/seguimiento`:
  - estructura final prevista
  - pendiente de backend para listado consolidado de envios despachados

## Placeholders preparados

Se dejaron visibles y navegables:

- `/logistica/incidencias`
- `/logistica/historial`

Decision de UX:

- no se simulan casos persistidos
- no se inventan metricas
- se muestra el diseño final esperado del modulo con copy honesto

## Componentes principales incorporados o reforzados

- `frontend/src/pages/logistica/LogisticaDashboardPage.tsx`
- `frontend/src/pages/logistica/LogisticaEnviosPage.tsx`
- `frontend/src/pages/logistica/LogisticaPreparedModulePage.tsx`
- `frontend/src/components/logistica/LogisticaRetiroItem.tsx`
- `frontend/src/components/logistica/LogisticaDespachoItem.tsx`

## Semaforo visual incorporado

Se usa lenguaje sobrio para indicar:

- `Disponible`
- `Parcial`
- `Pendiente`

Aplicado en:

- dashboard principal
- modulo operativo de envios
- modulos preparados

## Lo que queda pendiente

- listado real de envios en transito
- modulo real de incidencias logisticas
- historial real de acciones logisticas
- posibles ampliaciones del detalle de envio si backend expone mas datos
- eventual timeline de estados si backend lo publica

## Validacion

Ejecutado:

- `npm.cmd run build`

Resultado:

- OK

Observacion:

- Vite mantiene advertencia por chunk mayor a `500 kB`
- no bloquea compilacion

