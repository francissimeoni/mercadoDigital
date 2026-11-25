# Playbook - Canonizacion admin v2

Fecha: 2026-04-26

## Objetivo

Operar canonizacion de productos proveedor en admin con flujo real y repetible:

1. tomar pendientes
2. asociar a canonico existente o crear canonico nuevo
3. validar que la bandeja se actualiza correctamente

## Precondiciones

- usuario con rol `ADMIN_MD`
- backend y frontend desplegados con version que incluye canonizacion v2
- productos proveedor existentes sin `producto_canonico_id` para probar pendientes

## Entrada al modulo

Opciones:

- menu admin: `Catalogo -> Canonizacion`
- dashboard admin: tarjeta/atajo `Aprobar canonizacion`

Ruta:

- `/admin/canonizacion`

## Flujo operativo estandar

### A) Revisar pendientes

En columna `Pendientes proveedor`:

- usar filtros por texto, proveedor, categoria y stock
- seleccionar un producto pendiente

### B) Asociar a canonico existente

En `Aprobar con canonico existente`:

- buscar candidato por nombre/marca/modelo
- click `Asociar`
- verificar mensaje de exito
- confirmar que el producto desaparece de pendientes (si estaba filtrado como pendiente)

### C) Crear canonico nuevo

En `Crear canonico nuevo`:

- completar `marca`, `modelo`, `nombrePublico`
- opcional: `atributosJson`
- click `Crear y asociar`
- verificar mensaje de exito
- confirmar salida de pendientes

## Contrato API (referencia rapida)

- `GET /admin/canonizacion/pendientes`
- `GET /admin/canonizacion/candidatos?q=&limit=`
- `PUT /admin/canonizacion/productos/{productoId}/asociar/{productoCanonicoId}`
- `POST /admin/canonizacion/productos/{productoId}/crear-canonico`

## Smoke test recomendado (5 minutos)

1. abrir `/admin/canonizacion`
2. filtrar por `sin stock` y luego limpiar filtros
3. tomar un pendiente y asociar a existente
4. tomar otro pendiente y crear canonico nuevo
5. recargar pagina y validar consistencia de bandeja

## Troubleshooting

- no aparecen pendientes:
  - verificar que existan productos activos sin canonico
- no aparecen candidatos:
  - limpiar busqueda y revisar catalogo canonico activo
- error al asociar/crear:
  - revisar respuesta API y logs backend
- UI desactualizada:
  - refrescar pagina y validar que se invaliden queries de React Query

## Criterio de cierre del modulo

El modulo se considera operativo cuando:

- admin puede resolver pendientes sin salir de la pantalla
- filtros permiten trabajo por volumen
- acciones de asociar/crear actualizan bandeja en tiempo real
- build frontend/backend queda en verde

