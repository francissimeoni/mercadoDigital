# Acta - Bandeja pedidos proveedor cierre

Fecha: 2026-03-18

## Objetivo

Cerrar la bandeja de pedidos del modulo proveedor en frontend como una superficie operativa mas clara, profesional y util para el trabajo diario, sin alterar la logica real ya implementada.

## Mejoras realizadas en experiencia operativa

- Se mejoro la lectura de cada item de pedido con una jerarquia mas clara:
  - identificacion del pedido
  - producto
  - cantidad
  - direccion o falta de direccion
  - antiguedad
  - estado operativo
- Se agrego busqueda local por datos visibles:
  - `pedidoId`
  - `nombreProducto`
  - `direccionEnvio`
- Se agregaron filtros locales seguros:
  - todos
  - pendientes
  - con direccion
  - sin direccion
- Se agrego orden local por antiguedad:
  - mas antiguos primero
  - mas recientes primero
- Se incorporo un panel expandible por item con detalle simple usando solo informacion ya disponible en la respuesta actual.
- Se mejoro el feedback de accion `Marcar listo para retiro`:
  - loading por item
  - bloqueo de acciones repetidas mientras hay una transicion en curso
  - mensaje de exito
  - mensaje de error claro
- Se mejoraron los estados de loading, error y vacio para que queden mas alineados con el dashboard proveedor.

## Que se mantuvo igual en logica real

- Se sigue usando el endpoint real:
  - `GET /proveedores/{proveedorId}/pedidos-pendientes`
- Se sigue usando la transicion real:
  - `POST /envios/{pedidoId}/listo-para-retiro`
- No se modificaron reglas de negocio.
- No se agregaron endpoints nuevos.
- No se recalcularon estados operativos en frontend.

## Datos reales vs presentacionales

Datos reales provenientes del backend actual:

- `pedidoId`
- `itemId`
- `productoId`
- `nombreProducto`
- `cantidad`
- `direccionEnvio`
- `estadoEnvio`
- `fechaCreacionPedido`

Datos solo presentacionales o derivados localmente:

- antiguedad mostrada en horas o dias a partir de `fechaCreacionPedido`
- filtros locales sobre el listado ya cargado
- busqueda local sobre campos visibles
- orden local por fecha del pedido
- expansion visual de detalle por item

## Pendiente para hilos futuros

- filtros server-side si el backend los expone en el futuro
- vista de detalle proveedor mas completa si aparece nueva superficie backend
- acciones adicionales sobre pedidos proveedor segun evolucione el flujo operativo
- metricas especificas del proveedor basadas en endpoints dedicados

## Conclusion

La bandeja de pedidos del proveedor queda cerrada como una superficie mas util para operacion diaria, mejorando navegabilidad, lectura, filtro y feedback de acciones, manteniendo intacta la logica real ya implementada en backend.
