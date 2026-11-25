# Acta - Regla de compra interna del revendedor a costo v1

Fecha: 2026-04-02

## Alcance

Se deja asentada una aclaración funcional relevante para el desarrollo:

- el revendedor posee una tienda white-label de Mercado Digital
- los clientes finales compran en esa tienda a precio comercial
- el mismo revendedor también puede comprar desde su propia tienda u operación
- cuando compra como revendedor, esa compra debe resolverse a costo y no como venta comercial normal

## Impacto funcional

- compra pública y compra interna no son equivalentes aunque puedan apoyarse en catálogo y pedido canónico
- el sistema debe distinguir el contexto de compra para congelar el precio correcto
- la UI también debería dejar claro si el usuario está comprando como cliente final o como revendedor

## Estado actual observado

- la compra interna simple por producto y cantidad ya existe en frontend autenticado
- el backend de pedido canónico interno sigue usando la misma lógica base de pricing que el flujo comercial
- por lo tanto, la separación explícita de "compra interna a costo" sigue pendiente de implementación

## Siguiente paso recomendado

Definir e implementar un modo de compra interna del revendedor que preserve:

- precio a costo operativo
- congelamiento correcto en pedido
- diferencia clara respecto del flujo público/comercial
