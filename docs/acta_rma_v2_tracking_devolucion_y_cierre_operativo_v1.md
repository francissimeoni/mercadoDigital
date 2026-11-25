# Acta: RMA v2 tracking de devolucion y cierre operativo v1

Fecha: 2026-04-07

## Objetivo

Pulir el circuito de RMA/postventa para que:

- cliente vea con mas claridad la secuencia real del reclamo
- admin tenga mejor lectura del estado operativo
- el cierre quede mejor asentado, incluso con comprobantes adjuntos

## Cambios aplicados

### 1. Hitos reales del caso expuestos por backend

La API de RMA ahora devuelve hitos auditados del caso:

- fecha de aprobacion
- fecha de solicitud de envio
- fecha de descarga del rotulo de devolucion
- fecha de recepcion en deposito
- fecha de cierre
- fecha de generacion del rotulo de reposicion

Tambien se expone:

- `rotuloReposicionGenerado`
- `resolucionFinal`
- `montoCompensacion`
- `referenciaCompensacion`

## 2. Tracking publico mas especifico

Dentro del seguimiento del pedido, el bloque de postventa ya no muestra un unico cierre generico.

Ahora diferencia mejor:

- caso recibido
- garantia aprobada
- devolucion del producto
- recibido en deposito
- cierre por:
  - devolucion de dinero
  - reposicion aprobada
  - reposicion preparada
  - rechazo

Si el caso ya cerro, el cliente ve un resumen final mas claro en el mismo tracking.

## 3. Admin con timeline operativo del caso

El detalle admin de RMA ahora incluye un recorrido del caso separado del resumen, para leer de un vistazo:

- origen del reclamo
- aprobacion
- devolucion
- recepcion en deposito
- cierre final

## 4. Cierre con adjuntos

La resolucion admin ahora acepta adjuntar imagenes del cierre.

Uso previsto:

- comprobante de transferencia
- evidencia de compensacion
- soporte visual del cierre

Esos adjuntos quedan asentados como nota interna del caso junto al resumen de resolucion.

## 5. Trazabilidad de rotulo de reposicion

Cuando admin genera/imprime la etiqueta de reposicion, ahora se registra auditoria especifica:

- `RMA_ROTULO_REPOSICION_GENERADO`

Eso alimenta el tracking mas claro de la rama de reposicion.

## Verificacion

- Backend:
  - `./mvnw.cmd test "-Dtest=RmaServiceTest,NotificacionPostCompraServiceTest,PlantillaEmailPostCompraServiceTest,PublicPedidoTrackingServiceTest,PublicPedidoTrackingControllerTest,EnvioServiceTest"`
  - OK
- Frontend:
  - `npm run build`
  - OK

## Estado resultante

RMA queda bastante mas cerca de cerrado a nivel operativo:

- mejor lectura cliente
- mejor lectura admin
- mejor trazabilidad del caso
- cierre economico mas asentado

Pendiente principal del frente:

- si se quiere profundizar mas, el siguiente escalon seria modelar un tracking logistico real de la reposicion como envio independiente
