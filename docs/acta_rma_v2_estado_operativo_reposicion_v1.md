# Acta - RMA v2 Estado Operativo de Reposicion v1

## Objetivo

Formalizar mejor el circuito de reposicion dentro del RMA sin saltar todavia a un modulo logistico independiente.

## Alcance implementado

- backend ahora expone un `estadoReposicion` derivado del circuito real:
  - `NO_APLICA`
  - `PENDIENTE_SOLICITUD_PROVEEDOR`
  - `ESPERANDO_INGRESO_MD`
  - `LISTA_PARA_REENVIO`
  - `EN_CAMINO_AL_CLIENTE`
  - `ENTREGADA_CLIENTE`
- backend ahora devuelve tambien un `resumenReposicion` listo para uso operativo en admin y tracking publico
- `instruccionesSiguientePaso` deja de ser generica en casos `RESUELTO` y pasa a indicar el siguiente hito real de la reposicion
- frontend admin de RMA muestra el estado operativo actual del reemplazo con resumen explicito
- tracking publico del pedido muestra al cliente el estado actual del reemplazo con una explicacion corta y consistente
- `RmaServiceTest` suma cobertura sobre los estados y textos del circuito de reposicion

## Decision funcional

- en esta etapa, la reposicion sigue viviendo dentro del mismo RMA
- no se crea todavia un envio logistico nuevo con entidad propia
- se mejora primero la claridad operativa y de seguimiento antes de separar el circuito

## Beneficio operativo

- admin ve rapidamente en que punto real esta el reemplazo
- el cliente entiende si el reemplazo esta pendiente, ingresado, reenviado o entregado
- la reanudacion futura del trabajo cae sobre un modelo mas explicito y menos dependiente de leer fechas sueltas o inferir estados desde la bitacora

## Siguiente escalon posible

Si hiciera falta mayor profundidad, el siguiente paso natural sigue siendo modelar un envio/logistica real de reposicion como circuito separado.
