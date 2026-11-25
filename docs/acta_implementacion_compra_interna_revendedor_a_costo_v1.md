# Acta - Implementacion compra interna del revendedor a costo v1

Fecha: 2026-04-02

## Alcance

Se implemento la separacion entre:

- compra publica/comercial del cliente final
- compra interna del revendedor a costo

## Cambios aplicados

- el catalogo interno autenticado del revendedor ahora muestra costo operativo estimado
- el detalle interno del producto ahora muestra costo operativo estimado
- el storefront publico mantiene precio comercial final para cliente final
- el pedido canonico interno del revendedor deja de aplicar markup comercial
- el pedido canonico publico mantiene la logica comercial existente

## Criterio aplicado

- compra interna a costo = sin markup de revendedor
- se mantiene el costo operativo de transaccion en backend
- la separacion se resolvio por contexto de uso:
  - flujo autenticado interno
  - flujo publico/storefront

## Verificacion

- `./mvnw.cmd clean test` en `backend`: ok
- `npm.cmd run build` en `frontend`: ok

## Pendientes relacionados

- resolver ids de dominio sin perfiles demo en frontend
- seguir endureciendo UX y copy para que quede todavia mas clara la diferencia entre compra interna y compra publica
