# Hoja de Ruta Maestra - Mercado Digital

## 1. Filosofia de desarrollo

Principios vigentes del proyecto:

- Backend = fuente de verdad
- Frontend = operativo minimo, sin recalcular negocio
- Un hilo = un modulo
- Cada modulo deja acta
- No avanzar fuerte sin cerrar lo anterior
- Cambios chicos, compilables y testeables
- Operacion real > estetica

Esto sigue alineado con el repositorio actual.

## 2. Estado global real del proyecto

### Backend

Estado general: muy avanzado, pero no todavia "cerrado limpio".

Implementado en codigo:

- catalogo canonico
- multi-oferta
- pricing engine
- pedido canonico
- stock locking / reserva
- webhook PSP
- logistica operativa base
- RMA
- notificaciones
- multitenant
- storefront publico
- compra publica directa
- inicio PSP publico
- tracking publico
- retorno publico post pago
- notificaciones post compra
- productos proveedor
- importador proveedor
- administracion base de revendedores/storefront

Observacion importante:

- El backend compila y la suite `./mvnw test` esta en verde.
- Sigue sin implicar produccion inmediata: aun falta validacion E2E real, endurecimiento de casos borde y limpieza de dependencias demo.
- Conclusion: backend muy avanzado y ahora bastante mas sano para seguir cerrando modulos.

### Frontend

Estado general: muy avanzado, con mezcla de superficies reales y superficies preparadas.

Implementado en codigo:

- login real con JWT
- rutas protegidas por rol
- catalogo interno
- detalle de producto interno
- pedidos internos
- notificaciones
- proveedor:
  - dashboard
  - pedidos
  - productos
  - carga Excel
- logistica:
  - dashboard
  - bandeja de envios
- RMA
- multitenant / tenant context
- storefront publico
- catalogo publico
- detalle publico
- checkout publico
- compra directa
- inicio PSP
- retorno PSP
- tracking publico
- dashboard admin base

Observacion importante:

- El frontend builda bien con `npm run build`.
- No todo lo visible en frontend esta conectado a backend real.
- Hay modulos admin, logistica y proveedor que todavia son placeholders honestos o workspaces sin contrato backend.
- La compra interna del revendedor ya funciona sin carrito para un producto y cantidad, y ahora quedo separada del pricing comercial a cliente final.
- El login ya devuelve contexto operativo real (`revendedorId`, `proveedorId`, `clienteId`) y el frontend dejo de resolver esas entidades con un mapa demo por email.
- Queda asentada tambien la separacion conceptual entre:
  - portal madre MD (`www.mercado-digital.com.ar`)
  - storefronts publicos de revendedores (`www.ventasnavarro.com`, etc.)
- En local, `VITE_STOREFRONT_HOST` sigue sirviendo para una simulacion fija de storefront, pero el admin ya puede abrir storefronts por dominio usando override local en URL sin tocar el `env`.

## 3. Modulos cerrados, parciales y pendientes

### Modulo 1 - Backend Core

Estado: casi cerrado

Incluye:

- pricing
- catalogo
- pedidos
- stock
- webhook PSP
- logistica base
- RMA

Diagnostico:

- La base de dominio esta implementada y es consistente.
- Hay cobertura automatizada amplia.
- No lo marco como "cerrado total" porque todavia faltan validacion comercial real y hardening operativo, aunque la suite ya quedo en verde.

### Modulo 2 - Frontend Operativo Interno

Estado: parcial avanzado

Implementado:

- login
- roles
- catalogo
- detalle de producto
- compra directa simple por producto y cantidad
- pedidos
- notificaciones
- proveedor
- logistica
- RMA

No cerrado del todo por:

- la estrategia actual descarta carrito en esta fase y prioriza compra directa simple por articulo
- algunas superficies proveedor siguen siendo parciales
- aun faltan validaciones manuales E2E en algunos recorridos internos y de admin

Conclusion:

- usable para trabajo interno y demos
- no lo considero modulo completamente cerrado

### Modulo 3 - Multitenant

Estado: cerrado base

Incluye:

- modelo multitenant base
- storefront por dominio
- branding
- tenant context
- admin de revendedores/storefronts base

Diagnostico:

- la base tecnica esta bien asentada
- el flujo principal existe de punta a punta

### Modulo 4 - Storefront Publico

Estado: cerrado y extendido

Incluye:

- catalogo publico
- detalle de producto
- checkout publico

Ademas hoy ya suma:

- tracking publico
- retorno post pago

Diagnostico:

- este modulo esta mas avanzado que lo que reflejaba la hoja anterior

### Modulo 5 - Compra Publica Directa

Estado: cerrado

Incluye:

- crear pedido publico
- crear cliente automatico
- vincular cliente con revendedor

Diagnostico:

- consistente con el flujo conversado
- implementado en backend y consumido por frontend

### Modulo 6 - Inicio PSP

Estado: cerrado

Incluye:

- `POST /public/pedido/{id}/pago`
- preparacion real del checkout PSP
- `checkoutUrl` real

Diagnostico:

- ya no es solo "sandbox con referencias"
- el frontend ya redirige al checkout cuando recibe URL valida

### Modulo 7 - PSP End-to-End

Estado: parcial muy avanzado

Lo que la hoja anterior marcaba como faltante ya existe en gran parte:

- retorno PSP
- webhook publico
- confirmacion automatica por webhook
- pantalla post pago
- tracking publico post compra

Lo realmente pendiente ahora es:

- validacion comercial real con URL publica de webhook
- pruebas E2E estables fuera de localhost
- endurecimiento de casos borde
- confirmacion final de topologia publica backend/frontend para cerrar `MERCADOPAGO_REDIRECT_URI`, `MERCADOPAGO_WEBHOOK_URL` y `VITE_API_BASE_URL`
- validar escenarios multi-storefront antes de dar por cerrado negocio operativo

Conclusion:

- este modulo ya no esta "reci en curso"
- esta en etapa final tecnica + validacion real

## 4. Modulo actual real

Punto actual del proyecto:

### Modulo 8 - Validacion comercial real

Estado: en curso / siguiente prioridad real

Objetivo:

- probar flujo real completo:
  - cliente compra
  - PSP cobra
  - webhook confirma
  - proveedor prepara
  - logistica retira/despacha
  - cliente sigue tracking

Por que este es el modulo actual:

- el repo ya tiene gran parte del flujo tecnico
- lo que falta es validacion operativa real y ajuste fino

Definicion funcional ya asentada:

- cliente final compra en la tienda del revendedor a precio comercial
- el revendedor tambien puede comprar desde su propia tienda, pero a costo
- esa separacion de pricing ya quedo implementada en catalogo interno, detalle interno y pedido interno canonico

### Modulo 8.1 - Markup comercial revendedor v1

Estado: implementado base / parcial avanzado

Objetivo:

- permitir que el revendedor defina su margen comercial
- permitir que el revendedor defina comision para asesores de venta
- devolver precio final estimado, ganancia neta revendedor y ganancia asesor sin calculo local en frontend

Alcance v1 acordado:

- markup global del revendedor
- comision global de asesor
- override manual por producto
- grilla en dashboard del revendedor con preview de:
  - costo operativo
  - markup bruto
  - comision asesor
  - ganancia neta revendedor
  - precio final estimado

Implementado hoy:

- persistencia de configuracion global en backend
- override por producto en backend
- preview operativa por producto en backend
- pantalla de dashboard revendedor para:
  - editar regla global
  - revisar preview comercial
  - editar o eliminar override puntual
- endpoints protegidos bajo rol `REVENDEDOR`
- suite backend nuevamente en verde luego del cambio
- frontend compilando con la nueva superficie

Decisiones funcionales ya tomadas:

- la comision del asesor sale del markup del revendedor, no del costo proveedor ni del total de venta
- el backend debe seguir siendo la fuente de verdad del pricing comercial
- la v1 resuelve el corazon del modulo; reglas masivas por categoria/marca/proveedor quedan fuera por ahora

Pendiente para considerarlo "cerrado":

- validacion manual funcional de punta a punta con datos demo mas expresivos
- validar manualmente el nuevo contexto auth real en login revendedor/cliente/proveedor/admin
- decidir si mas adelante hace falta edicion masiva o si la v1 alcanza operativamente

### Modulo 8.2 - Waterfall economico y split real

Estado: definicion funcional iniciada

Diagnostico:

- la especificacion ya exigia split logico persistido y prohibia una cuenta recaudadora propia de MD
- el repo ya persiste `SplitPedidoItem`, pero hoy `monto_md` y `monto_envio` siguen en cero
- ya quedo asentada una recomendacion funcional para evolucionar a:
  - cobro por cuenta del revendedor
  - split logico persistido como fuente de verdad
  - split automatico real como etapa posterior

Pendiente real:

- llevar esa definicion al backend con semantica economica completa
- decidir modelado explicito del fee PSP
- definir cancelaciones, RMA y conciliacion con mas detalle operativo

Avance real reciente:

- backend ya persiste `monto_psp` por item
- backend ya tiene base de liquidacion proveedor:
  - `PENDIENTE`
  - `LIBERADO_AUTOMATICO`
  - `LIBERADO_MANUAL`
  - liberacion automatica en `RETIRADO`
- backend y frontend ahora ya exponen configuracion real del costo MD por categoria de paquete
- pedidos nuevos ya completan `monto_md` en split logico
- pedidos nuevos ya completan tambien `monto_envio` y `totalEnvio` usando tarifa logistica real

## 5. Modulos siguientes

### Modulo 9 - UX operativa minima

Estado: parcialmente avanzado

Ya hay trabajo hecho en:

- loading
- mensajes de error
- feedback visible
- alertas reutilizables

Sigue pendiente:

- pulido transversal
- consistencia de copy
- menos dependencia de estados demo
- claridad visual sobre cuando una compra es publica/comercial y cuando es interna/a costo

### Modulo 10 - Hardening

Estado: iniciado, no cerrado

Incluye:

- edge cases
- seguridad
- validaciones
- endurecimiento de tests

Hallazgos concretos hoy:

- suite backend en verde tras ajuste de tests de seguridad/proveedor/importacion
- el principal gap de ids demo en login ya fue eliminado con contexto auth real desde backend
- superficies visibles sin contrato real en admin/logistica/proveedor
- la compra interna del revendedor ya opera con costo operativo y sin markup comercial en el flujo autenticado

### Modulo 11 - Proveedores escalables

Estado: avanzado pero parcial

Ya implementado:

- productos proveedor
- media proveedor
- importador CSV/XLSX
- carga Excel en frontend
- dashboard proveedor bastante operativo
- alta manual de producto con hasta 3 imagenes opcionales en el mismo flujo (sin paso extra de buscar y adjuntar luego)
- categorias comerciales de producto administrables:
  - admin puede crear/activar/desactivar categorias
  - proveedor puede seleccionar categoria al cargar producto
  - proveedor puede sugerir categoria nueva si no existe

Pendiente real:

- canonizacion admin con backend real
- cierre de reclamos/garantias
- mas robustez sobre importacion y sus tests
- evaluar siguiente remate de UX en panel proveedor y luego avanzar al panel revendedor

### Modulo 12 - Admin MD operativo completo

Estado: parcial

Ya implementado:

- home admin
- RMA admin
- revendedores
- storefronts
- gestion real de alta/edicion de revendedor
- gestion real de configuracion/persistencia de storefront
- base navegable del modulo

Pendiente real:

- pedidos admin reales
- pagos admin reales
- auditoria real
- configuracion real mas amplia
- usuarios/roles reales
- reportes reales
- canonizacion con contrato backend

Diagnostico actualizado:

- el backend ya exponia contratos reales para revendedores/storefront
- ahora el frontend admin tambien permite operar altas y ediciones reales sin depender de seed manual
- el admin ya puede abrir la simulacion local del storefront configurado para validar dominio y branding sin cambiar Vite manualmente
- el admin ya puede subir assets de storefront (`logo`, `hero`, `favicon`) al backend y reutilizarlos como URL publica local
- el admin ya puede editar costo operativo MD por categoria de paquete y ese dato impacta en pricing real
- todavia no es un admin completo, pero este frente ya dejo de ser solo lectura

### Modulo 13 - Logistica ampliada

Estado: parcial

Ya implementado:

- dashboard logistica
- retiros pendientes
- despachos pendientes
- transportistas
- detalle de envio
- rotulo PDF

Pendiente real:

- seguimiento consolidado de envios en transito
- incidencias persistidas
- historial logistico persistido
- timeline de estados

### Modulo 14 - Automatizacion comercial

Estado: futuro

Objetivo:

- asesores
- white-label mas completo
- automatizaciones comerciales
- branding extendido

Referencia funcional ya formalizada para este frente:

- `docs/acta_diseno_revendedor_comercial_difusion_y_presupuesto_v1.md`
- `docs/acta_implementacion_revendedor_difusion_whatsapp_sin_api_v1.md`
- `docs/acta_implementacion_revendedor_asesores_presupuestos_redes_v1.md`
- alcance priorizado:
  - compartir producto (incluyendo WhatsApp)
  - presupuesto/lista compartible por link
  - atribucion por asesor sobre ventas del revendedor
  - cierre obligatorio por checkout PSP (sin pago manual)

## 6. Riesgos y observaciones madre

### A. Riesgo de sobreestimar "cerrado"

Hay modulos que visualmente ya existen, pero siguen siendo:

- placeholders honestos
- workspaces sin contrato real
- flujos apoyados en datos demo

### B. Riesgo de desalineacion entre documentacion y repo

La hoja anterior habia quedado atrasada en storefront/PSP publico y demasiado optimista en cierre interno/backend.

### C. Riesgo tecnico inmediato

Antes de declarar produccion tecnica cercana, conviene cerrar:

- validacion E2E real PSP
- limpieza de dependencias demo en frontend
- consolidacion final del hardening mas alla de la cobertura automatizada

## 7. Resumen ejecutivo actual

Foto corta y honesta del proyecto:

- El proyecto esta mucho mas avanzado de lo que parecia en storefront y PSP publico.
- El backend tiene una base muy fuerte y ahora con suite en verde, pero todavia no esta en estado de hardening final.
- El pricing comercial ya soporta descuento v1 del revendedor, limitado a su propio markup.
- El ownership multi-proveedor ya tuvo una validacion local real satisfactoria.
- La base local multi-storefront ya tiene segundo tenant demo y retorno post-pago aislado por `revendedorId`.
- El frente multi-storefront ya tuvo validacion manual real satisfactoria:
  - pedido, tracking, mail y operacion quedan bajo el tenant correcto
  - el tracking publico ya no responde en otro storefront gracias a validacion host-tenant en backend
- El motor comercial y operativo ya tuvo una validacion local integral mas fuerte:
  - compra sobre producto con 2 proveedores
  - seleccion correcta de la mejor oferta
  - visibilidad correcta del pedido para el proveedor correcto
  - storefront correcto reteniendo pedido, tracking y mails
  - reclamo de garantia ejecutado de punta a punta
  - resolucion RMA validada hasta cierre final del reemplazo
- El bug de checkout persistido ya tuvo correccion tecnica:
  - el retorno post-pago limpia el estado local asociado al tracking conciliado
  - no se reutilizan snapshots viejos de pedidos ya pagados al reabrir checkout
- El layout de logistica ya tuvo un primer ajuste tecnico:
  - grillas y cards pasan a breakpoints mas conservadores para no comprimirse dentro del layout con sidebar
  - queda pendiente la validacion visual/manual final sobre la pantalla ajustada
- El panel de logistica ya tuvo tambien una pasada visual final mas coherente con convenciones:
  - dashboard menos redundante
  - bandeja mas fluida y mas clara en mobile
  - cards operativas mas compactas
  - superficies preparadas mejor separadas del flujo real
- El panel admin ya tuvo tambien una primera pasada fuerte de cierre:
  - dashboard mas orientado a foco operativo real
  - conexiones internas mejores entre revendedores, storefronts, RMA y notificaciones
  - correccion de navegacion admin que aun apuntaba a rutas equivocadas o poco claras
- Las notificaciones internas ya dieron un paso real de negocio:
  - sin exposicion proveedor ↔ revendedor
  - con auditoria admin por cada bloque de disparo
- La matriz full de notificaciones/mails ya quedo redefinida para el cierre operativo:
  - foco en in-app interno por actor correcto
  - foco en mails transaccionales del cliente final a lo largo del ciclo logistico
  - mails internos opcionales diferidos para una etapa siguiente
- Los mails del cliente final ya avanzaron sobre el mismo tracking canonico:
  - `PEDIDO_CREADO`
  - `PAGO_CONFIRMADO`
  - `ENVIO_DESPACHADO`
  - `ENVIO_ENTREGADO`
- El tracking publico tambien ya tiene una direccion UX mas sana:
  - estado actual resumido
  - recorrido didactico del pedido
  - base para reutilizar el mismo enlace en postventa/garantia
- RMA ya no queda solo como modulo interno aislado:
  - existe base real de postventa publica desde tracking
  - existe gestion admin enriquecida con conversacion del caso
  - ya soporta adjuntos de imagen persistidos en BD
  - ya tiene boton admin `Solicitar envio`
  - ya tiene rotulo PDF de devolucion y etiqueta PDF de reposicion
  - ya tiene politica sobria de mails del caso:
    - `caso recibido`
    - `ya puedes enviar el producto`
    - `tu caso fue resuelto`
  - ahora suma tambien un mail puntual cuando el reemplazo ya fue reenviado al cliente, con tracking real de la reposicion
  - ahora suma tambien un mail puntual cuando la entrega final del reemplazo queda confirmada
  - ya diferencia mejor devolucion de dinero vs reposicion dentro del seguimiento
  - admin ya puede adjuntar comprobantes al cierre
  - la impresion del rotulo de reposicion ya deja auditoria especifica
  - la bandeja y el detalle admin ya quedaron mas consistentes con el estado real del caso, evitando recargas duras y reduciendo acciones invalidas visibles
  - la reposicion v1 queda definida como circuito manual trazado dentro del mismo RMA, con tracking real cargado por admin para el reenvio al cliente
  - backend ahora resume ese circuito con un `estadoReposicion` explicito y un `resumenReposicion` listo para admin/storefront
  - los casos `RESUELTO` ya indican el siguiente paso real del reemplazo en lugar de un mensaje generico
  - el frente de transportistas ya no bloquea la operacion si la lista no aparece:
    - logistica y RMA aceptan nombre manual
    - backend crea o reutiliza el transportista al operar
  - si se quisiera profundizar mas, el siguiente escalon seria modelar un envio real de reposicion como circuito logistico independiente
- El frontend interno es ampliamente operativo, aunque no completamente cerrado.
- Ya empezo tambien un frente de cierre visual mobile-first sobre vistas operativas reales:
  - `RMA`
  - `logistica/envios`
  - `detalle RMA`
  - `tracking publico`
  - `detalle de producto storefront`
  - `checkout storefront`
  - `home storefront`
  - `catalogo storefront`
- Ese frente ya paso de vistas sueltas a un cierre por rol:
  - `logistica` ya tiene una pasada final mas consistente
  - `admin` ya tiene una primera pasada fuerte y todavia admite un remate de placeholders
  - siguiente recorrido recomendado despues de ese remate: `proveedor`, despues `revendedor`
- El modulo proveedor crecio mucho y ya es relevante.
- El admin existe como base real, pero todavia no como centro operativo completo.
- El siguiente foco sano no es "inventar mas superficies", sino cerrar gaps reales:
  - validacion publica real del webhook/PSP fuera de localhost
  - hardening tecnico y cierre de apoyos manuales
  - validacion visual/manual del nuevo cierre del panel de logistica
  - validacion visual/manual de la nueva pasada del panel admin y sus conexiones internas
  - pulido UX/estetico transversal

## 8. Prioridad recomendada

Orden recomendado de trabajo desde esta hoja madre:

1. Profundizar el circuito RMA/postventa:
   si hiciera falta, modelar envio/logistica real de reposicion como circuito independiente
2. Validacion publica real de PSP/webhook
3. Hardening backend y suite de tests
4. Cierre de gaps internos reales:
   carrito, ids de dominio, proveedor/importacion
5. Admin MD con contratos reales
6. Logistica ampliada
7. Canonizacion admin real
