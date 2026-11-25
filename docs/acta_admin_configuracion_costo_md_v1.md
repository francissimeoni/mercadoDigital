# Acta - Admin configuracion costo MD v1

Fecha: 2026-04-06

## Objetivo

Cerrar una primera version real y administrable del costo operativo MD por categoria de paquete, sin depender de `application.yml` ni de redeploy.

## Decision

Se implementa una configuracion persistida por `CategoriaPaquete` con estos criterios:

- editable desde admin en `/admin/configuracion`
- persistida en backend
- consumida por pricing, ofertas y construccion de split logico
- sin hardcodear montos en codigo fuente

## Alcance de la v1

Categorias soportadas:

- `CHICO`
- `MEDIANO`
- `VOLUMINOSO`

Superficies reales agregadas:

- tabla `configuracion_costo_md_categoria`
- endpoint `GET /admin/configuracion/costo-md`
- endpoint `PUT /admin/configuracion/costo-md`
- pantalla admin real para lectura y edicion

## Impacto funcional

El costo MD ya no queda implicito ni fijo.

Ahora:

- se puede ajustar sin deploy
- se integra al costo base comercial
- `SplitPedidoItem.monto_md` deja de quedar en cero para pedidos nuevos
- la preview del revendedor muestra costo operativo compuesto con MD

## Limites de esta v1

Todavia no resuelve:

- overrides por proveedor
- overrides por zona o temporada
- versionado historico fino de tarifas
- auditoria admin dedicada de cambios

## Siguiente paso recomendado

Sobre esta base, conviene seguir con alguno de estos dos frentes:

1. bajar `monto_envio` real al split logico
2. revisar si la formula comercial del revendedor debe aplicar markup sobre costo compuesto exacto incluyendo PSP o mantener el esquema actual
