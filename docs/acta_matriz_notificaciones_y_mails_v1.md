# Acta - matriz inicial de notificaciones y mails v1

Fecha: 2026-04-06
Estado: propuesta funcional inicial

## Objetivo

Ordenar el frente de notificaciones in-app y mails transaccionales antes de implementarlo de forma masiva.

## Principios

- backend = fuente de verdad del evento
- una transicion relevante puede disparar:
  - notificacion in-app
  - mail transaccional
- las notificaciones deben ser idempotentes
- proveedor y revendedor no deben exponerse entre si
- los mails al cliente final deben salir con identidad del storefront/revendedor cuando corresponda

## Eventos sugeridos

### 1. Pedido creado

Destinatarios:

- revendedor
- proveedor
- logistica MD
- cliente final

Mensajes sugeridos:

- revendedor: `Vendiste un producto`
- proveedor: `Nuevo pedido para preparar`
- logistica MD: `Ingreso un nuevo pedido`
- cliente final: `Recibimos tu pedido`

Canales:

- in-app: revendedor, proveedor, logistica
- mail: cliente final
- mail opcional: revendedor

### 2. Pago confirmado

Destinatarios:

- revendedor
- proveedor
- logistica MD
- cliente final

Canales:

- in-app: revendedor, proveedor, logistica
- mail: cliente final
- mail opcional: revendedor

### 3. Listo para retiro

Destinatarios:

- logistica MD
- revendedor

Canales:

- in-app: logistica, revendedor
- mail opcional: revendedor

### 4. Retirado

Destinatarios:

- revendedor
- cliente final

Canales:

- in-app: revendedor
- mail: cliente final

### 5. Despachado

Destinatarios:

- revendedor
- cliente final

Canales:

- in-app: revendedor
- mail: cliente final

Payload esperado:

- transportista
- tracking

### 6. Entregado

Destinatarios:

- revendedor
- cliente final

Canales:

- in-app: revendedor
- mail: cliente final

### 7. Excepciones futuras

Eventos a contemplar luego:

- pago rechazado
- retiro fallido
- incidencia logistica
- entrega fallida
- cancelacion
- RMA creado
- RMA aprobado
- RMA resuelto

## Campos minimos por notificacion

- tipo evento
- pedidoId
- trackingToken cuando exista
- actor origen
- timestamp
- estado previo
- estado nuevo
- metadata compacta

## Riesgos a controlar

- duplicacion por reintentos de webhook
- mails duplicados por la misma transicion
- links al storefront equivocado
- notificaciones cruzadas entre tenants

## Orden recomendado de implementacion

1. matriz final por evento y destinatario
2. notificaciones in-app idempotentes
3. mails transaccionales del cliente final
4. mails al revendedor
5. mails internos a proveedor y logistica si siguen teniendo valor operativo
