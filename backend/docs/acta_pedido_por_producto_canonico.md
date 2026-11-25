# Acta: Pedido por Producto Canonico

## Endpoint nuevo
- `POST /pedido/canonico`

## Request
```json
{
  "clienteId": "uuid",
  "revendedorId": "uuid",
  "items": [
    {
      "productoCanonicoId": "uuid",
      "cantidad": 2,
      "tieneSeguro": true
    }
  ]
}
```

## Reglas implementadas
- Seleccion automatica de oferta por producto canonico usando:
  - `OfertaProductoCanonicoService.getOfertasConStock(canonicoId, revendedorId)`
- Se toma la primera oferta (mejor) segun orden ya vigente.
- Si no hay ofertas con stock > 0:
  - lanza `SinStockException`
  - respuesta HTTP `409 Conflict`
- Si `cantidad` solicitada > `stockDisponible` de la mejor oferta:
  - `409 Conflict`

## Congelamiento en pedido_items
Por item:
- `producto_id` = oferta seleccionada
- `proveedor_id` = proveedor del producto seleccionado
- `precio_costo` = `proveedor_stock.precio_costo_actual` (fallback `producto.precio_costo`)
- `markup_aplicado` = calculado por `PricingService`
- `monto_seguro` = calculado por `PricingService`
- `precio_item_final` = calculado por `PricingService`
- `tiene_seguro` = request
- `cantidad` = request

## Split logico persistido
Por cada `pedido_item`, se crea `splits_pedido_item`:
- `monto_proveedor = precio_costo * cantidad`
- `monto_revendedor = markup_aplicado * cantidad`
- `monto_md = 0`
- `monto_envio = 0`
- `monto_seguro = monto_seguro * cantidad`

## Totales de pedido
- `total_final` = suma de `precio_item_final * cantidad`
- `total_seguro` acumulado
- `total_envio = 0` (por ahora)

## Ejemplo de respuesta (resumen)
```json
{
  "id": "uuid",
  "clienteId": "uuid",
  "revendedorId": "uuid",
  "estadoPedido": "CREADO",
  "totalFinal": 2600,
  "items": [
    {
      "productoId": "uuid",
      "proveedorId": "uuid",
      "cantidad": 2,
      "precioCosto": 1000,
      "markupAplicado": 200,
      "montoSeguro": 20,
      "precioItemFinal": 1300
    }
  ]
}
```

## Archivos principales tocados
- `dto/request/CrearPedidoCanonicoRequest.java`
- `dto/request/PedidoItemCanonicoRequest.java`
- `controller/PedidoController.java`
- `service/PedidoService.java`
- `exception/SinStockException.java`
- `exception/GlobalExceptionHandler.java`
