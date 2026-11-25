# Playbook - Validacion manual RMA con reposicion trazada v1

Fecha: 2026-04-07

## Objetivo

Validar manualmente el circuito de reemplazo dentro del mismo RMA:

1. admin resuelve el caso con reposicion
2. admin registra los hitos manuales de la reposicion
3. cliente ve la bitacora y el tracking real del reemplazo

## Precondiciones

- backend levantado
- frontend levantado
- existe un pedido entregado o despachado con tracking publico disponible
- existe un caso RMA ya abierto y recibido en deposito, o bien uno listo para llevar hasta ese punto

## Credenciales utiles

- `admin.demo@md.local / Admin123!`
- `cliente.pruebas@md.local / ClientePruebas123!`

## Recorrido recomendado

### 1. Abrir o ubicar un caso apto

- entrar a `/admin/rma`
- abrir un RMA que ya este en `RECIBIDO_EN_DEPOSITO`

Resultado esperado:

- el detalle muestra acciones de cierre
- todavia no aparece la bitacora de reposicion

### 2. Resolver con reposicion

- en el bloque `Resolver`
- elegir `RESUELTO`
- completar observacion si hace falta
- ejecutar `Resolver RMA`

Resultado esperado:

- el caso queda en `RESUELTO`
- aparece la gestion manual de reposicion
- aparece la bitacora de reposicion vacia o iniciada segun eventos

### 3. Registrar solicitud al proveedor

- cargar una observacion breve
- ejecutar `Solicitar reposicion a proveedor`

Resultado esperado en admin:

- aparece el primer hito en `Bitacora de reposicion`
- se ve fecha y observacion
- se habilita `Recibir reposicion en MD`

Resultado esperado en tracking publico:

- aparece el bloque `Bitacora del reemplazo`
- se ve el paso equivalente registrado

### 4. Registrar recepcion en MD

- cargar observacion si hace falta
- ejecutar `Recibir reposicion en MD`

Resultado esperado:

- aparece segundo hito en la bitacora
- se habilita el reenvio al cliente

### 5. Registrar reenvio al cliente

- elegir transportista
- cargar tracking
- opcionalmente observacion
- ejecutar `Reenviar al cliente`

Resultado esperado en admin:

- aparece tercer hito en bitacora
- se ve tracking
- se ve transportista
- se habilita `Marcar entregada`

Resultado esperado en tracking publico:

- el cliente ve el tracking real del reemplazo
- el paso queda visible dentro de la bitacora

### 6. Registrar entrega final

- cargar observacion si hace falta
- ejecutar `Marcar entregada`

Resultado esperado:

- aparece cuarto hito en bitacora
- el reemplazo queda marcado como entregado
- ya no quedan acciones pendientes de reposicion

## Evidencia minima a guardar

- captura del detalle admin con la bitacora completa
- captura del tracking publico mostrando la bitacora del reemplazo
- evidencia del tracking del reenvio

## Casos borde a revisar

- intentar reenviar sin transportista o tracking
  - debe fallar
- intentar usar reposicion sobre un caso `REEMBOLSADO`
  - debe fallar

## Validacion tecnica asociada

- backend:
  - `./mvnw.cmd test "-Dtest=RmaServiceTest,PublicPedidoTrackingServiceTest"`
- frontend:
  - `npm run build`
