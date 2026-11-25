# Acta - Dashboard proveedor base

Fecha: 2026-03-18

## Objetivo

Implementar el dashboard base del modulo proveedor en frontend con una estructura profesional, operativa y preparada para crecer, reutilizando el flujo ya existente de pedidos pendientes sin romperlo.

## Que se hizo

- Se creo una nueva home del modulo proveedor en `/proveedor`.
- Se agregaron tarjetas de resumen derivadas del endpoint real de pedidos pendientes:
  - items pendientes
  - unidades a preparar
  - items con direccion disponible
  - pedido mas antiguo pendiente
- Se agregaron accesos rapidos a:
  - pedidos
  - productos
  - carga por Excel
  - reclamos y garantias
- Se incorporaron placeholders operativos para submodulos aun sin backend suficiente:
  - `/proveedor/productos`
  - `/proveedor/carga-excel`
  - `/proveedor/reclamos`
- Se mejoro visualmente la bandeja actual de `/proveedor/pedidos` para alinearla con el dashboard y el resto del sistema.
- Se actualizo la navegacion del rol proveedor para usar el dashboard como punto de entrada del modulo.

## Que reutiliza del modulo existente

- Hook `usePedidosProveedor`
- Hook `useMarcarListoParaRetiro`
- Endpoint real `GET /proveedores/{proveedorId}/pedidos-pendientes`
- Endpoint real `POST /envios/{pedidoId}/listo-para-retiro`
- Rutas protegidas por rol ya implementadas
- `MainLayout` y patron general del frontend

## Decisiones tomadas

- No se agregaron endpoints nuevos.
- No se duplico logica de negocio del backend.
- Las metricas del dashboard se derivan solo de datos ya visibles en la bandeja real del proveedor.
- Los submodulos sin soporte backend quedan preparados con placeholder operativo en lugar de inventar funcionalidad.
- Se mantuvo la bandeja de pedidos como superficie operativa real del proveedor.

## Estados cubiertos

- loading del dashboard
- error con reintento
- vacio operativo
- falta de `proveedorId`

## Que queda preparado para los siguientes hilos

- gestion de productos del proveedor
- carga masiva por Excel
- reclamos / garantias del proveedor
- refinamiento de metricas cuando existan endpoints especificos adicionales

## Validacion prevista

- build del frontend
- navegacion por rol proveedor
- acceso a `/proveedor` y `/proveedor/pedidos`
- confirmacion de que la accion `Marcar listo para retiro` sigue operativa

## Conclusion

El modulo proveedor queda reestructurado con una base mas profesional y navegable, con dashboard inicial, accesos rapidos y una bandeja de pedidos alineada visualmente al nuevo modulo, sin alterar la logica operativa existente.
