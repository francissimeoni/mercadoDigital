# Acta - Compra publica directa v1 backend

Fecha: 2026-03-11

## Objetivo

Implementar la superficie minima de backend para compra publica directa en tienda white-label, reutilizando el flujo real ya existente de pedido canónico, pricing, seleccion de mejor oferta, reserva de stock, pago PSP y logistica.

La etapa mantiene las decisiones cerradas del proyecto:

- MD no vende productos
- el revendedor vende al cliente final
- el backend sigue siendo la fuente de verdad
- no se expone proveedor
- no se duplica logica de pricing ni de seleccion de oferta
- todo pago entra por PSP

## Endpoint agregado

Se agrego el endpoint publico:

- `POST /public/pedido/directo`

Controller agregado:

- `PublicPedidoDirectoController`

Service agregado:

- `PublicPedidoDirectoService`

## DTOs request/response

### Request

Se agrego `PublicPedidoDirectoRequest`.

Campos:

- `revendedorId`
- `nombreApellido`
- `email`
- `telefonoWhatsapp`
- `provincia`
- `localidad`
- `codigoPostal`
- `calle`
- `numero`
- `pisoDepto` opcional
- `observaciones` opcional
- `productoCanonicoId`
- `cantidad`
- `seguroTransporte`
- `asesorVentaId` opcional
- `aceptacionTerminos`

Validaciones minimas:

- campos principales obligatorios
- `email` con formato valido
- `cantidad >= 1`
- `aceptacionTerminos = true`

### Response

Se agrego `PublicPedidoDirectoResponse`.

Campos expuestos:

- `pedidoId`
- `estadoPedido`
- `estadoPago`
- `metodoPago`
- `totalProducto`
- `totalEnvio`
- `totalSeguro`
- `totalFinal`
- `demoraEstimadaDias`

El contrato es apto para continuar con la siguiente integracion PSP sin exponer datos internos de abastecimiento.

## Regla de ingreso del revendedor

Se adopto `revendedorId` explicito en request.

Motivo:

- alinea el backend con el `TenantContext` actual del frontend
- evita acoplar esta etapa a resolucion por host dentro del endpoint de compra
- mantiene la implementacion chica y consistente con la superficie publica existente

Antes de crear pedido, el backend valida disponibilidad publica del revendedor usando la misma politica de storefront ya cerrada.

## Reglas de creacion y reutilizacion de cliente

La compra publica no exige cuenta ni login.

Flujo implementado:

1. buscar `Usuario` por `email` normalizado
2. si no existe, crear `Usuario` nuevo con `tipoUsuario = CLIENTE`
3. asegurar relacion `ClienteRevendedor`
4. si la relacion no existe, crearla con `origenAlta = COMPRA_PUBLICA_DIRECTA_V1`
5. si la relacion existe pero esta inactiva, reactivarla

No se duplica la relacion `ClienteRevendedor`.

## Como se asegura ClienteRevendedor

La etapa reutiliza el modelo incorporado en MT1.

La verificacion se hace por:

- `usuario_id`
- `revendedor_id`

Comportamiento:

- si no existe, se inserta la relacion
- si ya existe, se reutiliza
- si existe inactiva, se reactiva

Esto deja trazada la vinculacion comercial cliente ↔ revendedor sin alterar el modelo de `Pedido`.

## Politica de validacion de revendedor y producto

Se mantiene una politica publica conservadora.

Se responde `404 Not Found` cuando:

- el revendedor no existe
- el revendedor esta inactivo
- no tiene storefront
- el storefront no esta publicado
- el producto canónico no existe
- el producto no tiene oferta visible para ese revendedor
- el `asesorVentaId` no existe, no esta activo o no pertenece a ese revendedor

Se responde `409 Conflict` cuando el flujo real de pedido devuelve conflicto operativo, por ejemplo:

- falta de stock
- stock insuficiente

Se responde `400 Bad Request` cuando el request es invalido.

## Compatibilidad con el flujo actual

La compra publica directa no crea un circuito paralelo.

Reutiliza:

- `PublicCatalogoService` para validar visibilidad publica del producto
- `PedidoService` para crear el pedido canónico real
- logica real de mejor oferta
- lock de stock existente
- persistencia real de `Pedido`, `PedidoItem`, `Pago`, `Envio` y `SplitPedidoItem`

Se agrego una variante interna en `PedidoService` para permitir direccion real de envio sin romper `POST /pedido/canonico`.

## Estado del pedido resultante

La compra publica crea el pedido en:

- `estadoPedido = CREADO`
- `estadoPago = PENDIENTE`
- `metodoPago = PSP`

Esta decision mantiene compatibilidad exacta con el flujo actual antes del webhook PSP.

## Que parte del flujo PSP queda preparada en esta etapa

Queda preparado:

- pedido persistido
- pago pendiente con metodo `PSP`
- totales congelados
- stock reservado
- envio persistido con direccion del comprador

Queda pendiente para la siguiente etapa:

- generar el dato concreto que consuma el frontend para iniciar checkout PSP si se define un endpoint o adaptador dedicado

En esta etapa no se introdujo un cambio grande en PSP porque no era necesario para compilar ni para integrar la compra publica al flujo real actual.

## Validacion de asesorVentaId

Si `asesorVentaId` llega informado:

- debe existir
- debe estar activo
- debe pertenecer al mismo revendedor de la compra

Si no cumple, se responde `404 Not Found`.

En v1 la referencia se valida pero no se persiste en `Pedido`, porque el modelo actual de pedido no tiene ese campo y no se hizo un refactor de dominio en esta etapa.

## Representacion de direccion de envio

El modelo actual de `Envio` solo persiste `direccionEnvio` como texto plano.

Para mantener cambios chicos y consistentes, la direccion se serializa a un string con:

- calle y numero
- piso/depto si existe
- localidad
- provincia
- codigo postal
- observaciones si existen

No se introdujo un modelo estructurado nuevo de direccion en esta etapa.

## Datos explicitamente no expuestos

La respuesta publica no expone:

- proveedor
- `proveedorId`
- costo
- precio costo
- split
- markup interno
- ids internos de abastecimiento
- ids de stock
- estructura interna de pricing

## Notas sobre totales

`totalEnvio` se mantiene como linea separada.

`totalSeguro` se devuelve separado.

`totalProducto` se expone como el remanente comercial:

- `totalFinal - totalEnvio - totalSeguro`

Esta decision evita exponer detalle interno de costo/split y se mantiene consistente con el modelo persistido actual.

## Tests agregados

Se agregaron o ajustaron tests minimos para:

- compra directa exitosa
- creacion de usuario cliente nuevo
- reutilizacion de usuario existente
- creacion/aseguramiento de `ClienteRevendedor`
- revendedor no disponible
- producto no visible
- request invalido
- acceso publico sin token

Archivos:

- `backend/src/test/java/ar/com/mercadodigital/backend/service/PublicPedidoDirectoServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/PublicPedidoDirectoControllerTest.java`
- ajuste en `backend/src/test/java/ar/com/mercadodigital/backend/controller/SecurityAccessTest.java`

## Pendiente para la siguiente etapa

- frontend de checkout publico
- inicio concreto de checkout PSP desde storefront
- carrito publico multi-item
- login/registro cliente final
- historial y panel de cliente
- seguimiento publico de pedido por token

Nota de backlog v2:

- seguimiento/garantia por troquel fisico + QR/token y canal digital complementario para postventa

## Resultado

El backend ya soporta compra publica directa minima por producto canónico para tienda white-label, asociando la operacion al revendedor correcto, creando o reutilizando cliente final sin login, asegurando `ClienteRevendedor` y manteniendo intacto el flujo operativo real ya existente.
