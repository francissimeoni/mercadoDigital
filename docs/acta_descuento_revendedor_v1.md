# Acta - descuento revendedor v1

Fecha: 2026-04-06
Estado: implementado v1

## Objetivo

Agregar un codigo de descuento opcional del revendedor para storefront, con una regla dura:

- el descuento solo puede aplicarse contra el markup del revendedor
- nunca puede erosionar costo proveedor, costo MD, PSP, envio ni seguro

## Definicion funcional cerrada

- el revendedor configura un codigo y un monto fijo desde `pricing-v1`
- el cliente final puede ingresar ese codigo en checkout
- backend valida coincidencia exacta del codigo contra el revendedor
- backend topea el descuento por el markup disponible del pedido
- el total final del pedido se reduce por ese descuento
- el split logico descuenta el importe unicamente de `montoRevendedor`

## Alcance tecnico

Backend:

- `Revendedor` ahora persiste `codigoDescuento` y `descuentoFijo`
- `Pedido` ahora persiste `totalDescuento` y `codigoDescuentoAplicado`
- `DescuentoRevendedorService` centraliza normalizacion y tope del descuento
- `PedidoService` distribuye el descuento sobre el split logico del revendedor
- `PublicPedidoDirectoService` y `PublicPedidoTrackingService` exponen `totalDescuento`

Frontend:

- `RevendedorPricingPage` permite configurar codigo y monto de descuento
- `StorefrontCheckoutPage` permite ingresar codigo de descuento
- checkout y tracking publico muestran el descuento separado cuando aplica

## Regla economica vigente

Formula visible:

- `PC = costo proveedor + PSP + comision MD`
- `precio producto publico = PC + markup`
- `total cliente = producto + envio + seguro - descuento`

Restriccion:

- `0 <= descuento aplicado <= markup disponible`

## Verificacion

- `backend`: `./mvnw -q test`
- `frontend`: `npm run build`
