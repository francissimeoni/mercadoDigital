# Acta - Notificaciones post compra v1

Fecha: 2026-03-17

## Objetivo

Implementar el modulo inicial de notificaciones post-compra por email transaccional para Mercado Digital, respetando:

- backend como unica fuente de verdad
- webhook como unica confirmacion valida de pago
- branding white-label por revendedor
- desacople de proveedor de email
- tolerancia a fallo del email sin romper pedido ni pago

## Alcance implementado

Se implemento:

1. email de confirmacion de pedido creado
2. email de pago confirmado disparado solo desde webhook validado
3. inclusion de `trackingToken` y `trackingUrl`
4. branding simple white-label por revendedor/storefront
5. servicio de email desacoplado con implementacion segura para local/test
6. auditoria del intento de envio

No se implemento:

- proveedor real de email
- WhatsApp
- campañas
- panel cliente
- colas distribuidas

## Diseño aplicado

### Eventos de dominio

Se definieron eventos internos:

- `EventoPedidoCreado`
- `EventoPagoConfirmado`

Se publican desde backend y se consumen con listener `AFTER_COMMIT` para no acoplar el envio de email a la transaccion principal.

### Servicio desacoplado

Se agrego la fachada:

- `NotificacionPostCompraService`

Responsabilidades:

- resolver branding
- renderizar plantilla
- delegar envio al canal de email
- registrar auditoria de enviado, omitido o error
- nunca propagar fallo de email al flujo principal

### Canal de email

Se definio la interfaz:

- `EmailSender`

Implementacion inicial:

- `LoggingEmailSender`

Comportamiento:

- para local/test no envia correo real
- deja evidencia por log
- mantiene el modulo desacoplado de proveedor especifico

### Branding y tracking

Se agrego:

- `BrandingPostCompraResolver`

Resuelve:

- `nombreComercial`
- `colorPrimario`
- `logoUrl`
- `baseUrlPublica`
- `emailComercial`
- remitente visible

Fallbacks seguros:

- `nombreComercial` -> storefront, luego revendedor, luego nombre remitente por defecto
- `colorPrimario` -> `#0f172a`
- `baseUrlPublica` -> dominio del revendedor si existe, sino `app.post-compra-email.base-public-url`

El `trackingUrl` se construye como:

- `{baseUrlPublica}/pedido/{trackingToken}`

### Plantillas

Se agrego:

- `PlantillaEmailPostCompraService`

Plantillas implementadas:

- pedido creado
- pago confirmado

Contenido incluido:

- nombre comercial del revendedor
- pedido
- estado
- monto
- tracking token
- tracking URL

No se exponen:

- proveedor
- costos internos
- split
- datos sensibles del PSP

## Integracion con flujo actual

### Pedido creado

Punto de integracion:

- `PublicPedidoDirectoService`

Comportamiento:

- al crear el pedido publico directo se publica `EventoPedidoCreado`
- el email se resuelve y envia solo despues del commit

### Pago confirmado

Punto de integracion:

- `WebhookPspService`

Comportamiento:

- el email de pago confirmado solo se publica cuando el webhook produce la transicion real a `PAGADO` / `CONFIRMADO`
- el webhook repetido no vuelve a publicar el evento

Esto mantiene la idempotencia requerida.

## Configuracion agregada

Se agregaron propiedades:

- `app.post-compra-email.from-email`
- `app.post-compra-email.from-name`
- `app.post-compra-email.base-public-url`

Mapeables por entorno via:

- `POST_COMPRA_EMAIL_FROM_EMAIL`
- `POST_COMPRA_EMAIL_FROM_NAME`
- `POST_COMPRA_EMAIL_BASE_PUBLIC_URL`

Defaults seguros:

- `no-reply@mercadodigital.local`
- `Mercado Digital`
- `http://localhost:5173`

## Auditoria y robustez

Se registra auditoria en `auditoria_eventos` para:

- `EMAIL_PEDIDO_CREADO_ENVIADO`
- `EMAIL_PEDIDO_CREADO_OMITIDO`
- `EMAIL_PEDIDO_CREADO_ERROR`
- `EMAIL_PAGO_CONFIRMADO_ENVIADO`
- `EMAIL_PAGO_CONFIRMADO_OMITIDO`
- `EMAIL_PAGO_CONFIRMADO_ERROR`

Regla aplicada:

- si falla el email, no se rompe la transaccion principal
- si faltan email o tracking, se omite y queda auditado

## Archivos principales agregados o modificados

Configuracion:

- `backend/src/main/resources/application.yml`
- `backend/src/main/java/ar/com/mercadodigital/backend/config/PostCompraEmailProperties.java`

Post-compra:

- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/EventoPedidoCreado.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/EventoPagoConfirmado.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/MensajeEmail.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/ContenidoEmail.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/EmailSender.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/LoggingEmailSender.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/BrandingPostCompra.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/BrandingPostCompraResolver.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/PlantillaEmailPostCompraService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/NotificacionPostCompraService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/postcompra/NotificacionPostCompraListener.java`

Integraciones:

- `backend/src/main/java/ar/com/mercadodigital/backend/service/PublicPedidoDirectoService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/WebhookPspService.java`

Tests:

- `backend/src/test/java/ar/com/mercadodigital/backend/service/PublicPedidoDirectoServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/service/WebhookPspServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/service/postcompra/BrandingPostCompraResolverTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/service/postcompra/PlantillaEmailPostCompraServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/service/postcompra/NotificacionPostCompraServiceTest.java`

## Validacion realizada

Se validaron estos tests:

- `PublicPedidoDirectoServiceTest`
- `WebhookPspServiceTest`
- `BrandingPostCompraResolverTest`
- `PlantillaEmailPostCompraServiceTest`
- `NotificacionPostCompraServiceTest`
- `WebhookPspControllerTest`
- `SecurityAccessTest`

Resultado:

- verdes

## Resultado funcional

El backend ahora puede:

- disparar email post-compra cuando se crea un pedido publico
- disparar email post-compra cuando el webhook confirma el pago
- incluir tracking publico por token
- aplicar branding white-label del revendedor
- registrar auditoria del intento de envio
- evitar duplicacion del email de pago cuando el webhook es idempotente

## Pendiente para una etapa posterior

- implementar proveedor real de email
- agregar reintentos o outbox si se requiere mayor robustez operativa
- sumar metricas o panel de entregabilidad

## Conclusion

Quedo implementado el modulo v1 de notificaciones post-compra con cambios chicos, compilables y coherentes con el modelo actual:

- sin mover verdad de negocio al frontend
- sin confirmar pagos fuera del webhook
- sin acoplarse a un proveedor de email especifico
- sin romper el flujo actual de compra publica y webhook PSP
