# Acta - Backend split logico envio v1

Fecha: 2026-04-06

## Objetivo

Dejar de persistir el envio en cero cuando ya existia una tarifa logistica usada por la preview y el storefront.

## Problema detectado

Antes de este cambio:

- catalogo y preview comercial ya usaban `TarifaTransportista`
- el pedido real seguia congelando `totalEnvio = 0`
- `Envio.costoEnvio` quedaba en `0`
- `SplitPedidoItem.montoEnvio` quedaba en `0`

Eso podia desalinear lo que el usuario ve con lo que el backend congela al crear el pedido.

## Decision tecnica

Se agrega una base comun de costo logistico con `CostoEnvioService` y se integra en:

- calculo de pedido
- entidad `Envio`
- split logico
- ofertas internas y publicas

Ademas, se corrige la formula para que:

- el precio del producto no incluya envio
- el envio se persista y se muestre por separado
- el seguro se mantenga por separado

## Resultado

Ahora, para pedidos nuevos:

- `pedido.totalEnvio` se calcula y persiste
- `envio.costoEnvio` se calcula y persiste
- `split_pedido_item.monto_envio` se calcula y persiste
- el calculo del pedido canonico real queda alineado con la preview comercial
- el `PC` del producto deja de mezclar envio dentro del precio del item

## Nota funcional

En esta v1 el envio sigue resolviendose por tarifa de categoria de paquete.

No se cierra todavia:

- diferenciacion por zona real
- override por proveedor que despacha directo
- conciliacion logistico-financiera posterior

## Siguiente paso recomendado

El proximo frente sano es revisar si la formula final visible al revendedor debe separar mejor:

- costo compuesto base
- envio
- fee PSP

sin romper la regla comercial ya acordada.
