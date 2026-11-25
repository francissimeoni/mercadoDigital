# Acta - Checkout publico directo v1

Fecha: 2026-03-11

## Objetivo

Implementar en frontend el checkout publico directo de la tienda white-label, consumiendo el endpoint real `POST /public/pedido/directo` y manteniendo separada la superficie publica del storefront respecto de la app operativa autenticada.

## Archivos agregados o modificados

Agregados:

- `src/storefront/types/storefrontPedido.ts`
- `src/storefront/api/storefrontPedidoApi.ts`
- `src/storefront/hooks/useCrearPedidoDirecto.ts`
- `src/storefront/pages/StorefrontCheckoutPage.tsx`

Modificados:

- `src/storefront/pages/StorefrontProductDetailPage.tsx`
- `src/routes/AppRouter.tsx`

## Endpoint consumido

Se consume el endpoint backend:

- `POST /public/pedido/directo`

## Estructura elegida para el checkout

Se adopto checkout en ruta publica separada:

- `/producto/:productoCanonicoId/checkout`

Motivo:

- evita sobrecargar la ficha publica del producto
- separa mejor el estado del formulario respecto del detalle
- permite manejar loading, error y success sin contaminar la pagina de producto
- mantiene coherencia con la arquitectura actual basada en paginas bajo `src/storefront/pages`

El acceso al checkout parte desde la ficha publica del producto.

## Uso de contexto tenant y producto

El flujo usa:

- `productoCanonicoId` desde la ruta
- `revendedorId` desde `TenantContext`

No se usa query string, localStorage ni variables `VITE_*` como fuente principal del revendedor publico.

## Campos incluidos en formulario

Identificacion:

- nombre y apellido
- email
- telefono / WhatsApp

Envio:

- provincia
- localidad
- codigo postal
- calle
- numero
- piso/depto opcional
- observaciones opcionales

Comercial:

- cantidad
- seguro de transporte
- aceptacion de terminos

## Validaciones del lado frontend

Se agrego validacion basica antes del submit:

- nombre y apellido obligatorio
- email obligatorio y con formato simple valido
- telefono obligatorio
- provincia obligatoria
- localidad obligatoria
- codigo postal obligatorio
- calle obligatoria
- numero obligatorio
- cantidad minima = 1
- aceptacion de terminos obligatoria

No se recalculan precios ni totales en frontend.

## Decisiones cerradas en esta etapa

### 1. Donde vive el checkout

Ruta separada.

### 2. Como se maneja la cantidad

Se implemento un stepper simple con:

- boton `-`
- input numerico
- boton `+`

Siempre se fuerza minimo `1`.

### 3. Como se representa el seguro

Se usa checkbox simple.

La UI solo expresa la decision del cliente final y delega completamente al backend el impacto economico.

### 4. Que se hace con asesor de venta

No se muestra selector en esta etapa.

Motivo:

- hoy no existe una superficie publica backend consistente para listar o elegir asesores sin inventar datos o contratos

La decision queda documentada y el frontend no envia `asesorVentaId`.

### 5. Como se muestra el estado post-creacion

Tras crear el pedido se reemplaza el formulario por un bloque de confirmacion inicial con:

- `pedidoId`
- `estadoPedido`
- `estadoPago`
- `metodoPago`
- `totalProducto`
- `totalEnvio`
- `totalSeguro`
- `totalFinal`
- `demoraEstimadaDias`

Tambien se muestra una nota explicita aclarando que:

- el pedido fue creado
- el pago sigue pendiente
- no se simula PSP
- la integracion concreta de checkout PSP queda para la siguiente etapa

## Manejo de loading/error/success

Loading:

- boton en estado `Creando pedido...`

Error:

- validacion local mostrada con `FeedbackAlert`
- errores HTTP/backend mostrados con `FeedbackAlert`

Success:

- pantalla de confirmacion inicial dentro de la misma ruta
- resumen seguro devuelto por backend

## Como se muestra el resumen del pedido creado

La pantalla de exito renderiza el resumen devuelto por backend sin recalcular ni reinterpretar la logica de negocio.

`totalProducto`, `totalEnvio`, `totalSeguro` y `totalFinal` se muestran exactamente como los devuelve el backend.

## Que queda pendiente para integrar PSP real

- dato concreto de adaptador/inicio de checkout PSP desde backend
- navegacion al checkout PSP real
- retorno o conciliacion visual post-pago
- seguimiento posterior del pedido

En esta etapa no se simula pago ni se muestra el pedido como pagado.

## Impacto sobre la arquitectura frontend actual

- se agrega una nueva capa de pedido publico dentro de `src/storefront`
- `PublicStoreLayout` sigue separado de `MainLayout`
- no se modifica la app operativa autenticada
- el storefront mantiene su estructura por paginas, hooks, api y types

## Validacion

Se valido por compilacion:

- `npm run build`
