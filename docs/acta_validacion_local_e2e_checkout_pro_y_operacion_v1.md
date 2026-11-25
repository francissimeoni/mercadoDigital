# Acta - validacion local E2E Checkout Pro y operacion v1

Fecha: 2026-04-06
Estado: validado localmente

## Objetivo

Dejar asentada la validacion local integral del flujo comercial y operativo, aun sin webhook publico real de Mercado Pago.

## Alcance validado

Se valido localmente:

1. compra publica desde storefront
2. aplicacion de descuento del revendedor
3. creacion de pedido publico
4. preparacion de pago PSP con Checkout Pro sandbox
5. apertura real del checkout sandbox de Mercado Pago
6. simulacion manual del webhook `PAGO_CONFIRMADO`
7. tracking publico actualizado
8. bandeja proveedor
9. bandeja logistica
10. rotulo PDF
11. cierre operativo hasta `ENTREGADO`
12. reflejo final correcto en dashboard revendedor

## Evidencia funcional observada

- el storefront creo pedido con:
  - producto
  - envio
  - seguro
  - descuento separado
- `Preparar pago PSP` genero contrato real y redireccion al checkout sandbox
- el checkout sandbox abrio correctamente, aunque el ultimo click de pago no quedo confirmado desde la UI sandbox
- se simulo localmente el webhook `PAGO_CONFIRMADO`
- backend disparo el flujo post-compra:
  - mail transaccional de pago confirmado preparado
- el tracking publico reflejo:
  - `estadoPedido = PAGADO`
  - `estadoPago = CONFIRMADO`
  - `estadoEnvio = PENDIENTE_PREPARACION`
- proveedor vio el pedido y pudo marcar `LISTO_PARA_RETIRO`
- logistica tomo el pedido, marco:
  - `RETIRADO`
  - `DESPACHADO`
  - `ENTREGADO`
- rotulo PDF se genero correctamente
- dashboard del revendedor termino reflejando:
  - `estadoPedido = PAGADO`
  - `estadoPago = CONFIRMADO`
  - `estadoEnvio = ENTREGADO`

## Conclusion

Para entorno local, el flujo E2E ya quedo validado de punta a punta en negocio y operacion.

Lo pendiente ya no es validar si el mecanismo base funciona, sino cerrar:

- webhook publico real de Mercado Pago
- OAuth real del revendedor
- prueba sandbox/publica sin simulacion manual

## Hallazgos detectados

- el checkout sandbox de Mercado Pago abrio bien, pero la UI sandbox no permitio completar el click final de pago
- el dashboard revendedor tuvo una inconsistencia de navegacion al abrir detalle de pedido
- la UI de logistica se ve desfasada en layout y columnas

## Siguientes validaciones recomendadas

1. cargar un segundo proveedor y confirmar que el pedido se asigne al proveedor correcto
2. cargar un segundo storefront y confirmar:
   - aislamiento correcto de tenant
   - alta de cliente en storefront correcto
3. cerrar matriz de notificaciones y mails
4. luego pasar a webhook publico real y OAuth real
