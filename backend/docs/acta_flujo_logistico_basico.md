# Acta: Flujo Logistico Basico Post-Pago

## Estados de envio
- `PENDIENTE_PREPARACION`
- `LISTO_PARA_RETIRO`
- `RETIRADO`
- `DESPACHADO`
- `ENTREGADO`

## Endpoints
- `GET /envios/{pedidoId}`
  - Consulta estado logistico por pedido.

- `POST /envios/{pedidoId}/listo-para-retiro`
  - Requiere pedido en estado `PAGADO`.
  - Transicion: `PENDIENTE_PREPARACION -> LISTO_PARA_RETIRO`.
  - Auditoria: `ENVIO_LISTO_PARA_RETIRO`.

- `POST /envios/{pedidoId}/retirado`
  - Transicion: `LISTO_PARA_RETIRO -> RETIRADO`.
  - Auditoria: `ENVIO_RETIRADO`.

- `POST /envios/{pedidoId}/despachar`
  - Request:
    - `transportistaId`
    - `tracking` (obligatorio)
  - Transicion: `RETIRADO -> DESPACHADO`.
  - Setea transportista y tracking.
  - Auditoria: `ENVIO_DESPACHADO`.

- `POST /envios/{pedidoId}/entregar`
  - Transicion: `DESPACHADO -> ENTREGADO`.
  - Auditoria: `ENVIO_ENTREGADO`.

## Reglas de transicion (secuencia estricta)
- No se permiten saltos de estado.
- Si la transicion es invalida, retorna `409 Conflict`.
- Si no existe envio para el pedido, retorna `404 Not Found`.
- Para `listo-para-retiro`, si el pedido no esta `PAGADO`, retorna `409 Conflict`.

## Integracion con webhook PSP
- En confirmacion de pago (`PAGO_CONFIRMADO` + `estado=CONFIRMADO`):
  - Si el pedido no tiene `envio`, se crea automaticamente:
    - `estado_envio = PENDIENTE_PREPARACION`
    - `categoria_paquete` inferida del primer `pedido_item` (o `null`)
    - `costo_envio = pedido.total_envio`
    - `tracking = null`

## Ejemplo JSON de consulta
```json
{
  "pedidoId": "9f6f5a95-4f36-4e57-8fc9-b85efefe9d72",
  "estadoEnvio": "DESPACHADO",
  "tracking": "AND-AR-123456",
  "transportistaNombre": "Andreani",
  "costoEnvio": 1200.00
}
```
