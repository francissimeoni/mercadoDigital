# Acta: Webhooks PSP con Idempotencia

## Endpoint
- `POST /webhooks/psp`

## Payload ejemplo
```json
{
  "idEventoPsp": "evt_12345",
  "tipoEvento": "PAGO_CONFIRMADO",
  "referenciaPedido": "9f6f5a95-4f36-4e57-8fc9-b85efefe9d72",
  "referenciaPago": "pay_abc_001",
  "monto": 15000.00,
  "estado": "CONFIRMADO"
}
```

## Reglas de idempotencia
- Se persiste en `psp_webhooks` usando `id_evento_psp` como clave unica.
- Si llega un webhook con `idEventoPsp` ya existente:
  - responde `200 OK`
  - no reprocesa la logica de negocio.
- Se registra siempre `WEBHOOK_PSP_RECIBIDO` para webhook nuevo.

## Transicion de estados
- Condicion de confirmacion:
  - `tipoEvento = PAGO_CONFIRMADO`
  - `estado = CONFIRMADO`
- Si cumple:
  - `pago.estado_pago = CONFIRMADO`
  - `pago.psp_referencia = referenciaPago`
  - `pedido.estado_pedido = PAGADO`
  - auditoria: `PAGO_CONFIRMADO`
- Si `pedido` ya esta `PAGADO`:
  - responde `200`
  - no reprocesa confirmacion.
- Si `estado != CONFIRMADO`:
  - no confirma pedido ni pago.

## Errores posibles
- `404 Not Found`:
  - `referenciaPedido` no encontrada.
- `409 Conflict`:
  - monto de webhook distinto a `pedido.total_final`.
  - `referenciaPedido` invalida (formato UUID invalido).

## Consideraciones de implementacion
- No se valida firma PSP por ahora.
- No se integra PSP real por ahora.
- No se implementa split real por ahora.
