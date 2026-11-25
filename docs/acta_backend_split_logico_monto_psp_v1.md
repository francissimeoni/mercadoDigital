# Acta - Backend split logico con monto PSP v1

Fecha: 2026-04-06

## Objetivo

Mejorar la base tecnica del split logico para no perder el costo PSP por item mientras se sigue definiendo la semantica completa de:

- costo operativo MD
- envio
- liberacion economica al proveedor

## Cambio realizado

Se agrego `monto_psp` a `splits_pedido_item`.

Archivos principales:

- `backend/src/main/resources/db/migration/V17__split_pedido_item_monto_psp.sql`
- `backend/src/main/java/ar/com/mercadodigital/backend/entity/SplitPedidoItem.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/PedidoService.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/service/PedidoServiceCanonicoTest.java`

## Resultado tecnico

- el backend ya persiste `monto_psp` por item
- `PedidoService` ya no reconstruye el split al final usando solo `PedidoItem`
- ahora el split se arma desde el detalle de pricing calculado para cada item
- esto deja una base mas sana para completar luego:
  - `monto_md`
  - `monto_envio`
  - reglas de liberacion y conciliacion

## Lo que sigue pendiente

Todavia no se implementa en codigo:

- costo operativo MD real por item
- imputacion real de envio
- nueva formula comercial completa si cambia la interpretacion del costo compuesto visible al revendedor

Por ahora:

- `monto_md` sigue en `0`
- `monto_envio` sigue en `0`

## Verificacion

Se valido:

- `./mvnw -q test` en verde

## Conclusion

El split logico del backend ya conserva un componente economico clave que antes se perdia: el costo PSP por item. No cierra aun el waterfall completo, pero reduce deuda tecnica y deja preparado el siguiente paso de implementacion sin refactor grande.
