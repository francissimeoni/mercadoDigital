# Acta: Rotulos de Envio en PDF

## Dependencia usada
- `com.github.librepdf:openpdf:1.3.43`

## Endpoint
- `GET /envios/{pedidoId}/rotulo`
- Respuesta:
  - `Content-Type: application/pdf`
  - PDF generado on-the-fly (sin persistencia en disco/storage)

## Campos incluidos en el rotulo
- `pedidoId`
- `destinatario` (nombre de cliente si existe; fallback a email/id/pedidoId)
- `direccionEnvio`
- `estadoEnvio`
- `transportistaNombre` (si existe)
- `tracking` (si existe)
- `categoriaPaquete`
- `fechaCreacionPedido`
- texto visible para QR: `QR pedidoId (texto): <pedidoId>`

## Validaciones
- Si no existe envio para el pedido:
  - `404 Not Found`
- Si falla la generacion del PDF:
  - `500 Internal Server Error` controlado por `RotuloEnvioException`

## Ejemplo de uso
```http
GET /envios/9f6f5a95-4f36-4e57-8fc9-b85efefe9d72/rotulo
Accept: application/pdf
```
