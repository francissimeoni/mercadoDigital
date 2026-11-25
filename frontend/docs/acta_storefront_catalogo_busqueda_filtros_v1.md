# Acta - Storefront catalogo busqueda y filtros v1

Fecha: 2026-03-17

## Objetivo

Agregar descubrimiento basico al catalogo publico del storefront mediante busqueda, filtro simple y orden basico, sin tocar checkout ni PSP y manteniendo backend como fuente de verdad.

## Cambios realizados

- Se incorporo busqueda simple por `nombrePublico` en `/catalogo`.
- Se agrego filtro `Solo disponibles` usando el `stockDisponible` ya visible en la respuesta del backend.
- Se agrego orden simple por:
  - nombre
  - precio menor
  - precio mayor
  - mayor disponibilidad
- Se persistio el estado basico en query params:
  - `q`
  - `stock`
  - `sort`
- Se agrego accion de limpieza rapida de filtros.
- Se reutilizo el layout y estilo ya cerrados en el sprint anterior para mantener consistencia visual.

## Decision tecnica aplicada

El backend publico actual no expone parametros de busqueda, filtros ni orden en `GET /public/catalogo`; solo recibe `revendedorId`.

Por eso, en esta etapa:

- se reutiliza el listado real ya devuelto por backend
- la busqueda, filtro y orden se aplican solo en frontend sobre los productos ya visibles
- no se agregaron endpoints nuevos
- no se recalculan precios ni stock

## Pantalla impactada

- `/catalogo`

## Restricciones respetadas

- No se toco checkout publico ni PSP.
- No se modifico logica de pedido o pago.
- No se duplico logica de negocio del backend.
- No se abrio refactor grande.
- Los cambios quedaron acotados y compilables.

## Validacion manual realizada

- Se valido compilacion con `npm.cmd run build`.
- Se verifico que `/catalogo` conserve estados de loading, error y vacio.
- Se verifico soporte de estado sin resultados para busqueda/filtros.
- Se dejo persistencia por query params para navegacion y recarga basica.
- Se mantuvo responsive basico dentro del layout publico actual.

## Limitaciones detectadas

- La busqueda y los filtros no son server-side en esta version.
- El orden se aplica solo sobre el listado visible cargado en memoria.
- No hay filtros por categorias o atributos porque no existe aun una superficie publica backend consistente para eso.

## Conclusion

El catalogo publico gana una capa util de descubrimiento para el seed actual sin alterar la operacion existente, con cambios chicos y seguros sobre `/catalogo`, manteniendo backend como fuente de verdad para datos comerciales y stock visible.
