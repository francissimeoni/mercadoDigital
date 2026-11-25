# Acta - RMA v2 postventa publica y gestion admin v1

Fecha: 2026-04-07

## Objetivo

Reencuadrar el RMA para que deje de verse solo como flujo tecnico interno y pase a operar como:

- solicitud publica de postventa desde el tracking del pedido
- caso administrable por MD con ficha completa de compra
- conversacion simple entre cliente y admin dentro de plataforma

## Criterio adoptado

El cliente final no abre un flujo "tecnico" complejo.

Hace esto:

- entra al tracking del pedido
- abre un reclamo simple
- describe el problema
- sigue el estado y la conversacion desde el mismo enlace

Internamente, admin recibe un caso con mucho contexto y decide como seguir.

## Implementado en esta etapa

### 1. Postventa publica desde tracking

Se agrego una capa publica para:

- listar casos de postventa del pedido
- solicitar un nuevo caso
- responder dentro del mismo caso

Todo queda scopeado por:

- `trackingToken`
- `storefrontHost`

sin romper el hardening multi-tenant ya aplicado.

### 2. Conversacion del caso

Se agrego persistencia de mensajes para RMA/postventa:

- autor
- texto
- marca interna/publica
- timestamps

Esto permite:

- mensajes del cliente
- respuestas admin
- notas internas admin

### 3. Detalle admin enriquecido

El detalle de RMA ahora expone mucho mas contexto operativo:

- ids de caso, pedido y item
- cliente
- storefront/revendedor
- proveedor y email
- producto
- precio final y costo
- seguro y monto seguro
- estado pedido/envio
- tracking logistico
- transportista
- direccion de envio
- conversacion del caso

### 4. UI publica en tracking

La pagina de tracking ahora permite:

- abrir un reclamo simple
- ver casos del pedido
- seguir la conversacion
- responder desde el mismo enlace

### 5. UI admin en detalle RMA

El detalle admin ahora permite:

- revisar ficha redundante de compra
- ver el hilo de mensajes
- agregar respuesta publica
- agregar nota interna
- mantener acciones ya existentes de transicion

## Archivos principales

Backend:

- `backend/src/main/java/ar/com/mercadodigital/backend/service/RmaService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/PublicRmaService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/controller/PublicRmaController.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/entity/RmaMensaje.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/repository/RmaMensajeRepository.java`
- `backend/src/main/resources/db/migration/V22__rma_postventa_chat.sql`

Frontend:

- `frontend/src/storefront/pages/StorefrontPedidoTrackingPage.tsx`
- `frontend/src/storefront/api/storefrontRmaApi.ts`
- `frontend/src/storefront/hooks/useStorefrontRma.ts`
- `frontend/src/pages/rma/RmaDetallePage.tsx`

## Verificacion tecnica

- backend: `./mvnw.cmd -DskipTests compile test-compile` OK
- backend focalizado: `./mvnw.cmd test "-Dtest=RmaServiceTest,PublicPedidoTrackingServiceTest,PublicPedidoTrackingControllerTest,EnvioServiceTest"` OK
- frontend: `npm run build` OK

## Lo que queda como siguiente profundidad

Todavia no se implemento en esta etapa:

- boton real `Solicitar envio` con emision de rotulo de devolucion al cliente
- boton real de `Imprimir etiqueta de reposicion`
- timeline logistico especifico de devolucion
- estados cliente/internos separados visualmente con mas riqueza
- adjuntos/fotos del reclamo
- mail automatico al cliente cuando admin responde
- decision economica formal de responsabilidad y solucion dentro del caso

## Resultado

RMA deja de ser solo una bandeja tecnica y pasa a tener una base real de:

- postventa publica
- gestion admin contextual
- conversacion auditable

sin exponer proveedor al cliente final ni romper el tracking/storefront correcto.
