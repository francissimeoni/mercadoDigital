# Acta: Reserva de Stock y Cancelacion de Pedido

## Estrategia de lock
- Se implemento lock pesimista con `PESSIMISTIC_WRITE` en `ProveedorStockRepository`.
- Metodo agregado:
  - `findByProductoIdForUpdate(UUID productoId)`
- El lock se toma al reservar stock en `POST /pedido/canonico` y al reponer stock en cancelacion.
- La reserva y el descuento de stock ocurren dentro de la misma transaccion de creacion de pedido.

## Endpoints
- `POST /pedido/canonico`
  - Reserva stock por item al confirmar la mejor oferta seleccionada.
  - Si no existe `proveedor_stock`, se trata como stock 0 y responde `409 Conflict`.
  - Registra auditoria `STOCK_RESERVADO`.

- `POST /pedido/{id}/cancelar`
  - Solo permite cancelar en estado `CREADO` (y queda preparado para `PENDIENTE_PAGO` si existe en enum).
  - Repone stock por cada `pedido_item`.
  - Cambia `estado_pedido` a `CANCELADO`.
  - Registra auditoria `PEDIDO_CANCELADO` y `STOCK_REPUESTO`.
  - Si el estado no es cancelable, responde `409 Conflict`.

## Ejemplos
### Crear pedido canonico
```http
POST /pedido/canonico
Content-Type: application/json

{
  "clienteId": "a1b2c3d4-1111-2222-3333-444455556666",
  "revendedorId": "f1e2d3c4-7777-8888-9999-000011112222",
  "items": [
    {
      "productoCanonicoId": "11112222-3333-4444-5555-666677778888",
      "cantidad": 2,
      "tieneSeguro": true
    }
  ]
}
```

### Cancelar pedido
```http
POST /pedido/9f6f5a95-4f36-4e57-8fc9-b85efefe9d72/cancelar
```

## Consideraciones de concurrencia
- Dos pedidos concurrentes para el mismo producto ya no pueden descontar el mismo stock en paralelo:
  - el segundo pedido espera el lock del primero.
  - al continuar, revalida `stock >= cantidad` con el valor actualizado.
- Si durante la creacion no se puede reservar stock en algun item, la transaccion completa revierte y no descuenta nada.
- La reposicion en cancelacion usa el mismo lock para evitar carreras con nuevas reservas simultaneas.
