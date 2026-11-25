# Acta - dashboard proveedor completo v2

Fecha: 2026-03-19

## Objetivo

Llevar `/proveedor` desde su dashboard base inicial a un centro de operacion diaria mas completo, accionable y profesional, reutilizando datos reales ya conectados sin inventar backend nuevo.

## Bloques nuevos o ampliados

Se incorporaron o reforzaron estos bloques:

- bloque superior del modulo proveedor con identidad y CTA principal
- KPIs de pedidos
- KPIs de productos
- alertas operativas
- accesos rapidos accionables
- bandeja prioritaria embebida
- resumen de catalogo propio
- bloque de carga Excel preparado y conectado al flujo actual
- bloque de reclamos / garantias preparado de forma honesta
- actividad reciente basada en notificaciones visibles
- seguimiento de catalogo con listas cortas de items a revisar

## Datos reales utilizados

### Pedidos

Fuente real:

- `GET /proveedores/{proveedorId}/pedidos-pendientes`

Datos usados:

- `pedidoId`
- `itemId`
- `productoId`
- `nombreProducto`
- `cantidad`
- `direccionEnvio`
- `estadoEnvio`
- `fechaCreacionPedido`

### Productos

Fuente real:

- `GET /proveedores/{proveedorId}/productos`

Datos usados:

- `id`
- `productoCanonicoId`
- `productoCanonicoNombre`
- `estadoNormalizacion`
- `sku`
- `nombre`
- `precioCosto`
- `stockDisponible`
- `activo`
- `cantidadMedia`
- `imagenPrincipalUrl`
- `tieneVideo`

### Notificaciones

Fuente real:

- `GET /notificaciones/{usuarioId}`

Datos usados:

- `tipo`
- `titulo`
- `mensaje`
- `leida`
- `createdAt`

## Datos derivados localmente

Se derivan localmente, sin inventar negocio de backend:

- `pedidos pendientes`
- `unidades a preparar`
- `pedidos con direccion`
- `pedido mas antiguo`
- prioridad visual de pedidos por antiguedad y falta de direccion
- `productos activos`
- `productos inactivos`
- `productos sin stock`
- `productos pendientes de normalizacion`
- `productos sin media`
- `productos sin imagen principal`
- `con stock / sin stock`
- `con media / sin media`
- alertas operativas priorizadas
- resumen visual de catalogo propio
- actividad reciente a partir de notificaciones

## Alertas operativas mostradas

El dashboard muestra alertas solo si los datos reales las permiten derivar:

- pedido pendiente con mayor antiguedad
- productos sin stock disponible
- productos pendientes de normalizacion
- productos sin media

Todas se presentan con CTA hacia modulos ya existentes.

## Carga Excel

El dashboard no inventa historial ni auditoria que hoy no existen en backend.

Se deja un bloque profesional que comunica:

- que el modulo esta operativo
- que soporta `CSV/XLSX`
- que el flujo real vive en `/proveedor/carga-excel`
- que no hay aun historial resumido embebido en dashboard

## Reclamos y garantias

No se implemento gestion completa por falta de contrato backend suficiente.

Se deja un bloque preparado que comunica:

- estado del modulo
- CTA
- objetivo futuro del espacio
- ausencia actual de metricas reales

## Actividad reciente

Se reutilizan notificaciones visibles del usuario como mejor fuente real disponible para una linea de actividad.

No se inventaron:

- fechas de edicion de productos no expuestas
- historial de importaciones no disponible
- auditoria de media no disponible

## Que no se implemento por falta de backend o contrato real

- historial real de importaciones Excel en dashboard
- metricas reales de reclamos y garantias
- actividad reciente de productos con timestamps propios
- actividad reciente de media agregada con auditoria especifica
- estados de negocio adicionales no expuestos por backend
- canonizacion o sugerencias admin dentro del dashboard proveedor

## Decisiones de UX

- priorizar operacion diaria sobre decoracion
- separar modulos operativos de modulos preparados
- dejar los accesos rapidos con estado, utilidad y CTA claros
- mostrar resumen de catalogo solo con informacion realmente disponible
- mantener placeholders honestos donde todavia no hay contrato suficiente

## Validacion

- `npm run build` OK

## Conclusion

`/proveedor` deja de ser una portada base y pasa a ser un dashboard proveedor v2 enfocado en operacion diaria, integrando datos reales de pedidos, productos y notificaciones, con alertas accionables y una lectura mucho mas util del estado del proveedor sin inventar backend nuevo.
