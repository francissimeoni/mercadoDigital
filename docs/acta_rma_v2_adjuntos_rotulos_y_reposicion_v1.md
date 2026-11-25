# Acta - RMA v2 adjuntos, rotulos y base de reposicion v1

Fecha: 2026-04-07

## Alcance

Se completa la siguiente capa operativa de postventa/RMA:

- adjuntos de imagen persistidos en base de datos
- carga de imagenes desde tracking publico y desde gestion admin
- visualizacion de adjuntos dentro de la conversacion del caso
- boton operativo admin `Solicitar envio`
- rotulo PDF de devolucion para el cliente
- etiqueta PDF de reposicion para admin cuando el caso queda resuelto

## Backend

- nueva migracion `V23__rma_adjuntos_y_rotulos.sql`
- nueva entidad `RmaAdjunto`
- repositorio `RmaAdjuntoRepository`
- los mensajes de RMA ahora incluyen lista de adjuntos
- `RmaService` ya soporta guardar imagenes en BD ligadas al mensaje correspondiente
- validaciones:
  - solo imagenes `png/jpg/webp/gif`
  - hasta 5 MB por archivo
- nuevo estado operativo expuesto por servicio:
  - `solicitarEnvio()` mueve `APROBADO -> EN_TRANSITO_DEVOLUCION`
- nuevo servicio `RotuloRmaService`
  - `generarRotuloDevolucionPdf`
  - `generarRotuloReposicionPdf`

## API

### Publico

- `POST /public/pedido/{trackingToken}/rma`
  - ahora soporta `multipart/form-data`
- `POST /public/pedido/{trackingToken}/rma/{rmaId}/mensajes`
  - ahora soporta `multipart/form-data`
- `GET /public/pedido/{trackingToken}/rma/{rmaId}/rotulo-devolucion`

### Admin

- `POST /rma/{id}/mensajes`
  - ahora soporta `multipart/form-data`
- `POST /rma/{id}/solicitar-envio`
- `GET /rma/{id}/rotulo-devolucion`
- `GET /rma/{id}/rotulo-reposicion`

## Frontend

### Tracking publico

- se pueden adjuntar imagenes al abrir el caso
- se pueden adjuntar imagenes en mensajes posteriores
- se muestran las imagenes dentro de la conversacion
- si el caso ya tiene devolucion habilitada, aparece el boton para descargar el rotulo

### Admin RMA

- se muestran adjuntos dentro del hilo
- se pueden enviar mensajes con imagenes
- aparece el boton `Solicitar envio`
- aparece `Imprimir rotulo devolucion`
- aparece `Imprimir etiqueta reposicion` cuando el caso lo permite

## Resultado funcional

Con esto el caso de postventa deja de ser solo texto y ya puede operar de manera bastante real:

- cliente reporta con evidencia fotografica
- admin conversa con contexto redundante y adjuntos visibles
- admin habilita devolucion
- cliente descarga rotulo de devolucion
- admin puede emitir etiqueta de reposicion cuando corresponda

## Validacion tecnica

- backend: `./mvnw.cmd -DskipTests compile test-compile`
- backend focalizado: `./mvnw.cmd test "-Dtest=RmaServiceTest,PublicPedidoTrackingServiceTest,PublicPedidoTrackingControllerTest,EnvioServiceTest"`
- frontend: `npm run build`

## Pendiente siguiente

- mails automaticos cuando admin responde o habilita devolucion
- tracking especifico de la devolucion/reposicion
- adjuntos adicionales mas alla de imagenes si negocio los necesita
- pulido estetico del tracking/postventa y del detalle admin
