## Acta - Dashboard Admin MD Blueprint v1

Fecha:
- 2026-03-19

Estado:
- diseno funcional
- sin implementacion de codigo en esta etapa

Objetivo:
- definir la maqueta funcional completa del dashboard `ADMIN_MD`
- visualizar de forma tangible lo existente, lo parcial y lo faltante
- ordenar la construccion futura con criterio operativo primero

## Decision de producto

Se define que el dashboard admin no debe empezar como una portada generica ni como un panel de BI estetico. Su rol es funcionar como centro de operaciones de Mercado Digital, con foco en supervision, arbitraje, control y administracion transversal.

El criterio rector queda asi:

- prioridad operativa por encima de decoracion
- reutilizacion de contratos y modulos reales ya existentes
- placeholders honestos cuando no exista backend suficiente
- semaforo claro entre:
  - ya disponible
  - parcial / conectable
  - pendiente grande

## Rol funcional del Admin MD

El rol `ADMIN_MD` debe poder:

- supervisar la operacion integral
- arbitrar incidencias
- administrar revendedores y storefronts
- revisar productos proveedor y canonizacion
- monitorear pagos, pedidos, envios y RMA
- controlar calidad operativa
- acceder a auditoria y reportes

## Blueprint general del modulo

Sidebar propuesto:

- Resumen
- Operacion
- Pedidos
- Pagos
- Logistica
- RMA
- Catalogo
- Canonizacion
- Proveedores
- Revendedores
- Storefronts
- Usuarios y roles
- Notificaciones
- Reportes
- Auditoria
- Configuracion

Header superior propuesto:

- buscador global
- switch rapido de modulo
- alertas criticas
- notificaciones admin
- perfil / sesion

Layout de contenido:

- fila superior de KPIs
- bloque principal con bandejas y alertas
- bloques secundarios con metricas, actividad y accesos rapidos

## Home admin `/admin`

La home admin debe concentrar la foto operativa del sistema y responder rapidamente:

- que esta pasando hoy
- que necesita atencion inmediata
- donde hay riesgo o demora
- que modulo conviene abrir primero

Bloques definidos:

1. KPIs principales
- pedidos hoy
- pedidos pendientes de pago
- pedidos pagados sin despacho
- envios en preparacion
- envios despachados hoy
- RMA abiertos
- productos proveedor pendientes de canonizacion
- revendedores activos
- proveedores activos
- incidentes criticos

2. Atencion inmediata
- pagos con inconsistencia
- envios demorados
- RMA vencidos o sin movimiento
- productos sin stock critico pero muy vendidos
- storefronts publicados con problemas

3. Actividad reciente
- ultimos pedidos
- ultimos pagos confirmados
- ultimos cambios de envio
- ultimos RMA
- ultimas altas de productos proveedor

4. Mapa operativo
- pedidos por estado
- envios por estado
- RMA por estado
- volumen por proveedor
- volumen por revendedor

5. Accesos rapidos
- crear revendedor
- editar storefront
- revisar canonizacion
- abrir bandeja RMA
- revisar auditoria
- ver reportes

## Modulos del admin

### Operacion

Vista tipo command center con:

- timeline de eventos criticos
- alertas agrupadas por severidad
- cola de tareas pendientes
- indicadores de salud operativa

Paneles sugeridos:

- salud de pagos
- salud logistica
- salud catalogo
- salud storefront
- salud RMA

### Pedidos

Debe incluir:

- listado con filtros por estado, fecha, revendedor, proveedor, origen, pago, envio y busqueda libre
- detalle admin con pedido, cliente, revendedor, proveedor, items, split logico, pago PSP, envio, auditoria y acciones

Acciones esperadas:

- ver detalle
- abrir pago
- abrir envio
- abrir RMA relacionado
- exportar
- copiar referencia

### Pagos

Debe contemplar:

- bandeja por estado
- KPIs diarios y mensuales
- detalle con referencia PSP, monto, pedido, timestamps, estado interno, webhook y auditoria

Linea futura documentada:

- conciliacion
- split real
- reintentos
- validacion de firma

### Logistica

Debe mostrar:

- retiros pendientes
- despachos pendientes
- entregas del dia
- envios demorados
- tracking incompleto

Detalle esperado:

- pedido asociado
- proveedor origen
- transportista
- tracking
- estado
- timestamps
- rotulo PDF
- evidencia futura

### RMA

Debe incluir:

- KPIs por estado
- bandeja con filtros operativos
- detalle con pedido, cliente, producto, motivo, seguro, historial, decision, evidencia y resolucion

Acciones base:

- aprobar
- rechazar
- registrar recepcion
- resolver
- dejar observacion

### Catalogo admin

Debe servir para:

- ver catalogo canonico
- buscar producto canonico
- revisar ofertas proveedor asociadas
- inspeccionar media
- crear / editar canonico
- revisar calidad de ficha

### Canonizacion

Modulo separado del flujo proveedor. Su objetivo es resolver pendientes y asociarlos manualmente a `ProductoCanonico`.

Piezas definidas:

- bandeja principal:
  - pendientes
  - asociados recientemente
  - posibles duplicados
  - sin datos suficientes
- filtros:
  - proveedor
  - texto libre
  - SKU
  - estado
  - rango de fecha
- vista de trabajo de dos columnas:
  - izquierda: producto proveedor y metadata
  - derecha: candidatos canonicos y acciones

Acciones permitidas:

- asociar a canonico existente
- crear nuevo canonico
- dejar pendiente
- marcar para revision posterior

Restricciones mantenidas:

- no usar IA como fuente de verdad
- no automatizar canonizacion en v1
- mantener control humano total

### Proveedores

Debe incluir listado y detalle con tabs:

- resumen
- productos
- importaciones
- pedidos
- media
- incidencias
- auditoria

### Revendedores

Debe incluir:

- listado operativo
- detalle por tabs
- acciones de alta, edicion, activacion y publicacion

### Storefronts

Debe permitir:

- revisar dominio
- branding
- estado de publicacion
- salud de catalogo
- preview y validacion

### Usuarios y roles

Debe mostrar:

- listado
- detalle
- roles
- entidad asociada
- permisos efectivos
- auditoria futura

### Notificaciones admin

Debe concentrar:

- historial
- filtros por tipo
- deteccion de eventos no leidos
- revision de notificaciones criticas

### Reportes

Reportes iniciales:

- ventas por periodo
- pedidos por estado
- RMA por periodo
- envios por transportista
- productos mas vendidos
- proveedores con mayor volumen
- revendedores con mayor volumen

Linea futura:

- comisiones
- rentabilidad MD
- seguros
- desempeno asesor
- cohortes

### Auditoria

Vista central para:

- reconstruccion operativa
- soporte
- disputas
- debugging funcional

Campos minimos:

- fecha/hora
- tipo de evento
- entidad
- actor
- resultado
- referencia

### Configuracion

Secciones previstas:

- parametros generales
- branding MD
- transportistas
- reglas logisticas
- parametros de notificacion
- seguros
- PSP futuro
- dominios y entorno

## Estados visuales obligatorios

Todas las pantallas admin deben contemplar:

- loading
- error con reintento
- vacio
- exito de accion
- warning
- deshabilitado por permisos o estado

## Semaforo de implementacion

### Ya existe o esta cerca

Superficies y contratos con base real disponible:

- pedidos
- logistica
- RMA
- notificaciones
- revendedores / storefronts backend
- auditoria base por eventos

### Parcial o por conectar

- dashboard admin visual
- catalogo admin
- proveedores admin
- pagos admin
- reportes

### Pendiente grande

- canonizacion admin operativa completa
- metricas avanzadas
- configuracion avanzada
- conciliacion / split real

## Rutas sugeridas

- `/admin`
- `/admin/operacion`
- `/admin/pedidos`
- `/admin/pedidos/:id`
- `/admin/pagos`
- `/admin/pagos/:id`
- `/admin/logistica`
- `/admin/logistica/envios/:pedidoId`
- `/admin/rma`
- `/admin/rma/:id`
- `/admin/catalogo`
- `/admin/catalogo/:productoCanonicoId`
- `/admin/canonizacion`
- `/admin/canonizacion/:productoId`
- `/admin/proveedores`
- `/admin/proveedores/:id`
- `/admin/revendedores`
- `/admin/revendedores/:id`
- `/admin/storefronts`
- `/admin/storefronts/:id`
- `/admin/usuarios`
- `/admin/notificaciones`
- `/admin/reportes`
- `/admin/auditoria`
- `/admin/configuracion`

## Criterio visual

Lenguaje visual recomendado:

- serio
- limpio
- operativo
- empresarial
- alta densidad de informacion controlada
- colores por estado
- cards sobrias
- tablas claras
- filtros visibles
- jerarquia fuerte de alertas

No buscar todavia un look startup. El dashboard admin debe sentirse como centro de operaciones confiable.

## Prioridad de construccion sugerida

### Fase A - base completa de maqueta

- layout admin
- home dashboard
- sidebar definitivo
- cards KPI
- tablas base
- filtros base
- placeholders consistentes

### Fase B - modulos de mayor valor inmediato

- pedidos
- RMA
- canonizacion
- revendedores / storefronts
- auditoria

### Fase C - completitud profesional

- pagos
- reportes
- configuracion
- usuarios / roles
- metricas refinadas

## Primer sprint recomendado

1. layout admin definitivo
2. home admin con KPIs, alertas y actividad
3. sidebar con todos los modulos visibles
4. pantalla de pedidos admin
5. pantalla de RMA admin
6. pantalla de canonizacion admin
7. pantalla de revendedores / storefronts admin
8. pantalla de auditoria admin

## Alcance de esta acta

Esta acta:

- si define blueprint funcional completo del modulo admin
- si ordena el roadmap visual y operativo
- si clasifica que partes ya tienen base real y cuales no

Esta acta no implica:

- implementacion de codigo
- creacion de endpoints nuevos
- cierre funcional de pagos, catalogo o canonizacion
- automatizacion por IA

