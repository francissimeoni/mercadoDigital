# Acta: Bandejas Operativas

## Objetivo
Implementar bandejas operativas para proveedor, logistica MD y revendedor con filtros por participacion y estado.

## Endpoints

### Bandeja proveedor
- `GET /proveedores/{proveedorId}/pedidos-pendientes`
- Incluye items de pedidos donde el proveedor participa y el envio esta en:
  - `PENDIENTE_PREPARACION`
  - `LISTO_PARA_RETIRO`
- Campos:
  - `pedidoId`
  - `itemId`
  - `productoId`
  - `nombreProducto`
  - `cantidad`
  - `direccionEnvio`
  - `estadoEnvio`
  - `fechaCreacionPedido`

### Bandeja logistica MD
- `GET /logistica/retiros-pendientes`
  - Items con envio en `LISTO_PARA_RETIRO`
- `GET /logistica/despachos-pendientes`
  - Items con envio en `RETIRADO`
- Campos:
  - `pedidoId`
  - `productoId`
  - `nombreProducto`
  - `proveedorId`
  - `nombreProveedor`
  - `cantidad`
  - `estadoEnvio`
  - `tracking`
  - `fechaCreacionPedido`

### Bandeja revendedor
- `GET /revendedores/{revendedorId}/pedidos`
- Campos:
  - `pedidoId`
  - `estadoPedido`
  - `estadoEnvio`
  - `totalFinal`
  - `tracking`
  - `fechaCreacionPedido`

## DTOs agregados
- `PedidoProveedorResponse`
- `PedidoLogisticaResponse`
- `PedidoRevendedorResponse`

## Implementacion tecnica
- Servicio central: `BandejasOperativasService`
- Repositorios con queries de soporte:
  - `PedidoItemRepository` por proveedor + estado de envio
  - `PedidoItemRepository` por estado de envio para logistica
  - `PedidoRepository` por revendedor

## Ejemplo breve
```json
[
  {
    "pedidoId": "9f6f5a95-4f36-4e57-8fc9-b85efefe9d72",
    "estadoPedido": "PAGADO",
    "estadoEnvio": "RETIRADO",
    "totalFinal": 15000.00,
    "tracking": null,
    "fechaCreacionPedido": "2026-03-06T10:15:30"
  }
]
```
