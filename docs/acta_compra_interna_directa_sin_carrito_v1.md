# Acta - Compra interna directa sin carrito v1

## Fecha

- 2026-04-02

## Decision

Para la etapa actual del modulo interno de `REVENDEDOR` no se usara carrito.

En lugar de eso:

- la compra interna se hace producto por producto
- el revendedor puede elegir cantidad del mismo articulo
- el pedido se crea directo desde el detalle del producto

## Motivo

- el carrito no es prioridad en esta fase
- mezclar varios productos de distintos proveedores agrega complejidad que hoy no aporta al objetivo inmediato
- se prioriza una operacion mas simple, rapida y alineada con el cierre progresivo del sistema

## Alcance implementado

Frontend:

- se quita la navegacion visible a `/carrito`
- se quita la ruta activa de carrito del router principal
- `ProductoDetallePage` deja de agregar al carrito
- `ProductoDetallePage` ahora permite:
  - elegir cantidad
  - validar contra stock disponible
  - crear pedido directo con `POST /pedido/canonico`
- el flujo navega a confirmacion de pedido al crear correctamente

Backend:

- no se cambia el contrato de negocio
- se reutiliza el flujo existente de pedido canonico
- la cantidad ya estaba soportada por backend

## Efecto funcional

Con este cambio, el revendedor puede:

- entrar al detalle de un producto
- elegir cuantas unidades quiere
- crear un pedido directo de ese mismo articulo

Sin:

- carrito intermedio
- mezcla de multiples articulos
- apertura de complejidad innecesaria en esta fase

## Estado

- implementado y validado con `npm run build`

## Nota futura

El carrito puede volver en una segunda fase si se decide soportar:

- multiples productos en una misma compra
- reglas adicionales de combinacion operativa
- una experiencia de compra mas compleja para el revendedor
