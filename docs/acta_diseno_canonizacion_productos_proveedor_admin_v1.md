# Acta - Diseno canonizacion productos proveedor admin v1

Fecha: 2026-03-18

## Objetivo

Definir el modulo v1 de canonizacion de productos proveedor bajo `ADMIN_MD`, separado del flujo de importacion proveedor y sin automatizacion de decisiones.

La meta del modulo es permitir que administracion:

- revise productos proveedor pendientes
- compare informacion operativa y metadata externa
- asocie manualmente una oferta proveedor a un `ProductoCanonico` existente
- cree manualmente un `ProductoCanonico` nuevo cuando corresponda

## Principios

- control humano total
- IA no es fuente de verdad
- no se canoniza durante la importacion proveedor
- el proveedor no decide asociaciones canonicas
- la importacion sigue cargando oferta proveedor aunque la canonizacion quede pendiente

## Modelo conceptual

Se mantienen dos capas:

- `Producto`
  - oferta concreta del proveedor
  - contiene `sku`, `nombre`, `descripcion`, `precioCosto`, `stock`, media y metadata externa

- `ProductoCanonico`
  - ficha maestra del catalogo interno
  - es la entidad que estructura el catalogo visible y la consolidacion de ofertas

La canonizacion consiste en definir o corregir la relacion:

- `Producto.productoCanonicoId -> ProductoCanonico.id`

## Alcance funcional v1

### 1. Bandeja de pendientes

Nueva superficie admin para listar productos proveedor con estado de normalizacion pendiente.

Objetivos:

- detectar que ofertas aun no estan vinculadas a un canonico
- priorizar trabajo administrativo
- revisar productos ya asociados si se necesita auditar

### 2. Filtros

Filtros base propuestos:

- estado de normalizacion:
  - `PENDIENTE`
  - `ASOCIADO`
- proveedor
- texto libre:
  - `sku`
  - `nombre`
  - `descripcion`
  - `part number` si estuviera persistido o expuesto
- con stock / sin stock
- con media / sin media
- fecha de actualizacion

Orden inicial sugerido:

- pendientes primero
- mas recientes primero

### 3. Detalle de producto proveedor

La vista de detalle debe mostrar al admin:

- proveedor
- productoId interno
- `sku`
- `nombre`
- `descripcion`
- `precioCosto`
- `stock`
- media principal y galeria si existe
- estado de normalizacion actual
- `productoCanonicoId` actual si existe
- metadata externa disponible del archivo:
  - `tipo`
  - `iva`
  - `grupo origen`
  - `rubro origen`
  - `part number`

Objetivo del detalle:

- dar contexto suficiente para decidir asociacion o alta canonica

### 4. Asociacion a `ProductoCanonico` existente

Accion admin:

- buscar un `ProductoCanonico`
- seleccionar uno
- confirmar asociacion

Resultado:

- se setea `productoCanonicoId` en el `Producto`
- el producto pasa a estado `ASOCIADO`

### 5. Creacion manual de `ProductoCanonico`

Accion admin:

- crear un `ProductoCanonico` nuevo desde el detalle del producto proveedor
- luego asociar el producto actual a ese canonico creado

Campos minimos sugeridos para alta manual canonica:

- `nombrePublico`
- descripcion base opcional
- estado activo

Campos a definir en iteraciones futuras si el modelo los requiere:

- marca
- categoria interna
- atributos tecnicos estructurados

### 6. Estados resultantes

Estados visibles de normalizacion para v1:

- `PENDIENTE`
  - producto proveedor sin `productoCanonicoId`
- `ASOCIADO`
  - producto proveedor vinculado a `ProductoCanonico`

Estado administrativo opcional para evolucion futura:

- `OBSERVADO`
  - no se propone para v1 si no existe persistencia explicita

## Contrato propuesto v1

### Listado admin de productos proveedor

- `GET /admin/productos-proveedor`

Query params sugeridos:

- `estado`
- `proveedorId`
- `q`
- `conStock`
- `conMedia`
- `page`
- `size`
- `sort`

Respuesta sugerida por item:

- `productoId`
- `proveedorId`
- `proveedorNombre`
- `productoCanonicoId`
- `productoCanonicoNombre`
- `estadoNormalizacion`
- `sku`
- `nombre`
- `descripcionCorta`
- `precioCosto`
- `stockDisponible`
- `tieneMedia`
- `imagenPrincipalUrl`
- `updatedAt`

### Detalle admin de producto proveedor

- `GET /admin/productos-proveedor/{productoId}`

Respuesta sugerida:

- todo lo del listado
- `descripcion`
- metadata externa disponible
- media completa
- auditoria minima:
  - `createdAt`
  - `updatedAt`

### Buscar canonicos existentes

- `GET /admin/productos-canonicos`

Query params sugeridos:

- `q`
- `page`
- `size`

Respuesta sugerida:

- `productoCanonicoId`
- `nombrePublico`
- `activo`

### Asociar producto proveedor a canonico existente

- `PUT /admin/productos-proveedor/{productoId}/canonizacion`

Request:

- `productoCanonicoId`

Resultado:

- actualiza la relacion canonica del producto proveedor

### Crear `ProductoCanonico` y asociar

- `POST /admin/productos-proveedor/{productoId}/canonizacion/producto-canonico`

Request minimo sugerido:

- `nombrePublico`
- `descripcion` opcional
- `activo`

Resultado:

- crea `ProductoCanonico`
- asocia el `Producto` proveedor al canonico creado

## UX base sugerida

### Pantalla 1 - Bandeja

Seccion principal:

- tabla o cards de productos proveedor
- filtros superiores
- badges de estado
- acceso a detalle

Estados:

- loading
- error
- vacio
- lista con resultados

### Pantalla 2 - Detalle de canonizacion

Bloques sugeridos:

- resumen del producto proveedor
- imagenes y media
- metadata externa del archivo
- estado actual de normalizacion
- panel de acciones:
  - asociar a canonico existente
  - crear canonico nuevo

### Interacciones clave

- asociacion con confirmacion explicita
- feedback de exito/error
- regreso a bandeja manteniendo filtros si es posible

## No alcance v1

- sugerencias automaticas con decision final automatica
- canonizacion desde el panel proveedor
- alta automatica de canonicos durante importacion
- equivalencias automaticas por `Grupo` o `Rubro`
- motor de matching como fuente de verdad

## IA en etapas futuras

Se permite como asistencia futura:

- ranking de candidatos canonicos
- sugerencias por similitud de nombre
- sugerencias por `part number`
- sugerencias por marca/modelo

Condicion obligatoria:

- la accion final siempre la confirma `ADMIN_MD`

## Conclusion

El modulo admin de canonizacion v1 debe nacer como una bandeja de gobierno de catalogo con control humano total, separada de la importacion proveedor y enfocada en asociar ofertas proveedor existentes al catalogo canonico interno.
