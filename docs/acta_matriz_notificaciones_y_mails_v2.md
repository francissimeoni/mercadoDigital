# Acta - matriz de notificaciones y mails v2

Fecha: 2026-04-07
Estado: propuesta operativa integral para cierre funcional

## Objetivo

Dejar una matriz full, consistente con negocio y con lo ya validado en codigo:

- backend como fuente de verdad
- sin exposicion proveedor ↔ revendedor
- auditoria admin en todos los hitos relevantes
- storefront correcto como identidad de cara al cliente final

## Actores

- admin MD
- revendedor
- proveedor
- logistica MD
- cliente final

## Reglas madre

1. proveedor y revendedor no se notifican ni se nombran entre si
2. los mails al cliente final salen con identidad del storefront correcto
3. toda transicion relevante deja auditoria admin
4. si hay reintento tecnico, el disparo debe ser idempotente por evento
5. el tracking publico es la superficie de verdad para el cliente
6. la notificacion interna debe describir la accion sin filtrar datos innecesarios entre actores

## Matriz full recomendada

### 1. Pedido creado

Origen de verdad:

- backend al crear el pedido

Destinatarios y canales:

- admin MD
  - auditoria: `PEDIDO_CREADO`
  - auditoria: `NOTIFICACIONES_PEDIDO_CREADO_ADMIN`
- revendedor
  - in-app: si
  - mail: opcional, no prioritario
- proveedor
  - in-app: si
  - mail: no por ahora
- logistica MD
  - in-app: no
  - mail: no
- cliente final
  - mail: si

Mensaje funcional:

- revendedor: `Nueva venta registrada`
- proveedor: `Nuevo pedido para preparar`
- cliente final: `Recibimos tu pedido`

Observacion:

- todavia no conviene avisar a logistica en este punto si el pedido no fue confirmado por pago

### 2. Pago confirmado

Origen de verdad:

- webhook PSP confirmado por backend

Destinatarios y canales:

- admin MD
  - auditoria: `WEBHOOK_PSP_RECIBIDO`
  - auditoria: `PAGO_CONFIRMADO`
  - auditoria: `NOTIFICACIONES_PAGO_CONFIRMADO_ADMIN`
  - auditoria mail: `EMAIL_PAGO_CONFIRMADO_*`
- revendedor
  - in-app: si
  - mail: opcional, no prioritario
- proveedor
  - in-app: no
  - mail: no
- logistica MD
  - in-app: no
  - mail: no
- cliente final
  - mail: si

Mensaje funcional:

- revendedor: `Pago confirmado`
- cliente final: `Pago confirmado`

Observacion:

- este evento debe seguir siendo estrictamente backend-driven

### 3. Listo para retiro

Origen de verdad:

- proveedor marca `LISTO_PARA_RETIRO`

Destinatarios y canales:

- admin MD
  - auditoria: `ENVIO_LISTO_PARA_RETIRO`
  - auditoria: `NOTIFICACIONES_ENVIO_LISTO_PARA_RETIRO_ADMIN`
- revendedor
  - in-app: si
  - mail: opcional
- proveedor
  - in-app: no adicional
  - mail: no
- logistica MD
  - in-app: si
  - mail: opcional futuro
- cliente final
  - mail: si recomendado

Mensaje funcional:

- revendedor: `Pedido listo para retiro`
- logistica MD: `Retiro pendiente`
- cliente final: `Estamos preparando tu pedido para despacho`

Observacion:

- para cliente final conviene un texto comercial neutro; no hace falta exponer la palabra retiro

### 4. Retirado

Origen de verdad:

- logistica MD marca `RETIRADO`

Destinatarios y canales:

- admin MD
  - auditoria: `ENVIO_RETIRADO`
  - auditoria: `NOTIFICACIONES_ENVIO_RETIRADO_ADMIN`
- revendedor
  - in-app: si
  - mail: opcional
- proveedor
  - in-app: no
  - mail: no
- logistica MD
  - in-app: no adicional
  - mail: no
- cliente final
  - mail: si recomendado

Mensaje funcional:

- revendedor: `Pedido retirado`
- cliente final: `Tu pedido ya fue retirado y sigue avanzando`

### 5. Despachado

Origen de verdad:

- logistica MD marca `DESPACHADO`

Destinatarios y canales:

- admin MD
  - auditoria: `ENVIO_DESPACHADO`
  - auditoria: `NOTIFICACIONES_ENVIO_DESPACHADO_ADMIN`
- revendedor
  - in-app: si
  - mail: opcional
- proveedor
  - in-app: no
  - mail: no
- logistica MD
  - in-app: no adicional
  - mail: no
- cliente final
  - mail: si

Mensaje funcional:

- revendedor: `Pedido despachado`
- cliente final: `Tu pedido fue despachado`

Payload minimo recomendado:

- transportista
- tracking
- pedidoId
- trackingToken

### 6. Entregado

Origen de verdad:

- logistica MD marca `ENTREGADO`

Destinatarios y canales:

- admin MD
  - auditoria: `ENVIO_ENTREGADO`
  - auditoria: `NOTIFICACIONES_ENVIO_ENTREGADO_ADMIN`
- revendedor
  - in-app: si
  - mail: opcional
- proveedor
  - in-app: no
  - mail: no
- logistica MD
  - in-app: no adicional
  - mail: no
- cliente final
  - mail: si

Mensaje funcional:

- revendedor: `Pedido entregado`
- cliente final: `Tu pedido fue entregado`

## Matriz sintetica por actor

### Admin MD

- siempre auditoria, nunca depende de UI
- eventos minimos:
  - `PEDIDO_CREADO`
  - `NOTIFICACIONES_PEDIDO_CREADO_ADMIN`
  - `WEBHOOK_PSP_RECIBIDO`
  - `PAGO_CONFIRMADO`
  - `NOTIFICACIONES_PAGO_CONFIRMADO_ADMIN`
  - `ENVIO_LISTO_PARA_RETIRO`
  - `NOTIFICACIONES_ENVIO_LISTO_PARA_RETIRO_ADMIN`
  - `ENVIO_RETIRADO`
  - `NOTIFICACIONES_ENVIO_RETIRADO_ADMIN`
  - `ENVIO_DESPACHADO`
  - `NOTIFICACIONES_ENVIO_DESPACHADO_ADMIN`
  - `ENVIO_ENTREGADO`
  - `NOTIFICACIONES_ENVIO_ENTREGADO_ADMIN`
  - `EMAIL_*_ENVIADO|ERROR|OMITIDO`

### Revendedor

- in-app:
  - `PEDIDO_CREADO`
  - `PAGO_CONFIRMADO`
  - `LISTO_PARA_RETIRO`
  - `RETIRADO`
  - `DESPACHADO`
  - `ENTREGADO`
- mail:
  - no prioritario
  - dejar como opcional de etapa siguiente

### Proveedor

- in-app:
  - `PEDIDO_CREADO`
- mail:
  - no por ahora

### Logistica MD

- in-app:
  - `LISTO_PARA_RETIRO`
- mail:
  - no por ahora

### Cliente final

- mail:
  - `PEDIDO_CREADO`
  - `PAGO_CONFIRMADO`
  - `LISTO_PARA_RETIRO`
  - `RETIRADO`
  - `DESPACHADO`
  - `ENTREGADO`

## Prioridad de implementacion recomendada

### Ya implementado

- in-app revendedor:
  - `PEDIDO_CREADO`
  - `PAGO_CONFIRMADO`
  - `LISTO_PARA_RETIRO`
  - `RETIRADO`
  - `DESPACHADO`
  - `ENTREGADO`
- in-app proveedor:
  - `PEDIDO_CREADO`
- in-app logistica:
  - `LISTO_PARA_RETIRO`
- mail cliente final:
  - `PEDIDO_CREADO`
  - `PAGO_CONFIRMADO`
- auditoria admin por bloque principal

### Siguiente cierre sugerido

1. mails cliente final para:
   - `LISTO_PARA_RETIRO`
   - `RETIRADO`
   - `DESPACHADO`
   - `ENTREGADO`
2. validacion manual formal de `/notificaciones` por rol
3. endurecer idempotencia de mails por transicion si el flujo se amplía
4. dejar mails a revendedor como decision optativa de etapa siguiente

## Decision recomendada

Para el cierre operativo actual, la version mas sana es:

- mantener notificacion interna a revendedor, proveedor y logistica solo donde agrega accion real
- completar los mails del cliente final en toda la linea logistico-operativa
- no abrir por ahora mails internos a proveedor ni a logistica
- no abrir por ahora mails al revendedor salvo que negocio los pida de forma explicita
