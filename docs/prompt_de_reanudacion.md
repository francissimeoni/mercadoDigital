# Prompt de Reanudacion - Mercado Digital

Usar este documento cuando:

- se abre una sesion nueva
- hubo "compactando contexto"
- se quiere retomar trabajo sin depender de memoria de chat

## Instruccion de reanudacion

Al retomar este proyecto:

1. Leer `docs/especificacion.md` como fuente de verdad del negocio.
2. Leer `docs/contexto_proyecto.md` como marco general del sistema.
3. Leer `docs/Hoja_de_ruta_madre.md` como indice vivo del estado actual y lo pendiente.
4. Leer `docs/contexto_avance.md` como bitacora del ultimo estado de trabajo.
5. Revisar las actas de `docs/` solo si el modulo actual o la decision a retomar lo requiere.

## Prioridad entre documentos

Orden de prioridad:

1. `docs/especificacion.md`
2. `docs/convenciones.md`
3. `docs/Hoja_de_ruta_madre.md`
4. `docs/contexto_avance.md`
5. actas y documentos puntuales

## Reglas al reanudar

- No inventar contexto de negocio fuera de la especificacion.
- No asumir que un modulo esta cerrado solo porque existe UI o documentacion vieja.
- Tomar la hoja de ruta madre como indice operativo principal.
- Tomar `contexto_avance.md` como referencia del ultimo punto exacto de trabajo.
- Si hay conflicto entre documento y codigo, verificar en el repo y explicitar la diferencia.
- Mantener backend como fuente de verdad.
- Respetar `docs/convenciones.md`, incluyendo dejar acta cuando corresponda.

## Como seguir trabajando

Al avanzar en un modulo:

- actualizar `docs/Hoja_de_ruta_madre.md` si cambia el estado real del indice
- actualizar `docs/contexto_avance.md` con el ultimo estado concreto
- agregar acta breve en `docs/` si se cerro una funcionalidad importante o una decision relevante

## Estado actual esperado al reanudar

Foto corta esperada:

- El sistema es una plataforma B2B2C de dropshipping privado white-label.
- El storefront publico, compra publica directa, tracking y retorno PSP ya estan bastante avanzados.
- El foco actual no es abrir mas frentes por abrirlos, sino validar comercialmente, endurecer y cerrar gaps reales.
- La hoja madre ya fue corregida para reflejar mejor el estado real del repo.
- Ya quedo validado localmente un E2E muy fuerte:
  - storefront
  - descuento revendedor
  - preparacion PSP
  - webhook simulado
  - proveedor
  - logistica
  - rotulo PDF
  - `ENTREGADO`
- Ya quedo validado multi-proveedor:
  - el pedido entra solo al proveedor correcto
  - el proveedor incorrecto no lo ve
- El siguiente frente recomendado ya no es PSP local, sino:
  - profundizar RMA/postventa
  - luego rematar hardening operativo fino
- dentro de RMA ya quedo formalizado un subestado operativo de reposicion:
  - pendiente de pedir al proveedor
  - esperando ingreso en MD
  - lista para reenviar
  - en camino al cliente
  - entregada
- ya arrancamos tambien el cierre visual de vistas operativas con prioridad mobile-first:
  - bandeja RMA
  - logistica/envios
  - detalle RMA
  - tracking publico del pedido
  - detalle de producto storefront
  - checkout storefront
  - home storefront
  - catalogo storefront
- ya quedo cerrada tambien una pasada final del rol logistica:
  - dashboard mas claro y menos redundante
  - bandeja de envios mas fluida en mobile
  - cards de retiros/despachos con foco operativo mas directo
  - superficies preparadas mejor separadas del flujo real
- ya arrancamos y cerramos una primera pasada fuerte del rol admin:
  - dashboard admin reorganizado con foco operativo real
  - revendedores y RMA con mejor lectura mobile-first
  - notificaciones ahora entienden mejor el contexto admin/proveedor
  - storefronts ya puede abrir seleccionado por `revendedorId` desde enlaces internos
  - se corrigio navegacion admin que todavia apuntaba a `/rma` en lugar de `/admin/rma`

## Pendientes inmediatos al reanudar

- profundizar el circuito RMA/postventa despues del salto actual:
  - si se necesita mayor profundidad, modelar envio real de reposicion como circuito separado
  - si no, seguir afinando responsabilidades, validacion visual y cierre operativo del reemplazo ya formalizado
- seguir el pulido final por rol con prioridad mobile-first:
  - terminar de rematar superficies admin que siguen como placeholder preparado
  - luego pasar al panel `proveedor`
  - despues `revendedor`
- validar visualmente el ajuste del checkout persistido ya corregido
- validar visualmente en uso real la nueva pasada del panel `logistica`
- evaluar si hace falta una superficie admin real para proveedores

## Prompt sugerido para manana

Usar algo asi:

`Lee docs/prompt_de_reanudacion.md, docs/contexto_avance.md, docs/Hoja_de_ruta_madre.md, docs/acta_rma_v2_postventa_publica_y_gestion_admin_v1.md, docs/acta_rma_v2_adjuntos_rotulos_y_reposicion_v1.md, docs/acta_rma_v2_mails_postventa_sobrios_v1.md, docs/acta_rma_v2_tracking_devolucion_y_cierre_operativo_v1.md, docs/acta_rma_v2_estado_operativo_reposicion_v1.md, docs/acta_fix_vistas_mobile_operativas_v1.md, docs/acta_fix_panel_logistica_mobile_final_v1.md y docs/acta_fix_panel_admin_mobile_y_conexiones_v1.md. Ya dejamos RMA/postventa con tracking mas claro, adjuntos, rotulos, base de reposicion, mails sobrios del caso, cierre con comprobantes, estado operativo explicito del reemplazo, primeras mejoras mobile-first en vistas operativas, una pasada final del panel de logistica y una primera pasada fuerte del panel admin con mejores conexiones internas. Retomemos ahora por el siguiente remate admin o por el siguiente rol que corresponda, manteniendo backend/frontend en verde.` 

## Frase sugerida para reanudar

Al volver, alcanza con pedir:

- "Lee `docs/prompt_de_reanudacion.md` y retomemos desde ahi."

Si hubo compactacion de contexto:

- "Hubo compactacion. Lee `docs/prompt_de_reanudacion.md` y segui."

---

## Actualizacion 2026-04-26

Estado operativo actualizado:

- Canonizacion admin v2 ya esta operativa con backend + frontend reales.
- Ya existe playbook de canonizacion:
  - `docs/playbook_canonizacion_admin_v2.md`
- Ya existe acta tecnica de cierre de canonizacion v2:
  - `docs/acta_canonizacion_admin_v2_operativa.md`
- Storefront quedo en formato produccion (sin copy tipo wizard/dev en vistas publicas).
- Circuito storefront se considera cerrado por ahora.

Pendiente principal para manana:

- mejorar UX en dashboard proveedor para alta de producto con imagenes en el mismo flujo.
- hoy el flujo obliga a:
  - guardar producto
  - buscar producto
  - recien ahi subir imagen
- objetivo manana:
  - permitir subir imagenes durante el alta (hasta 3 imagenes opcionales), sin pasos extra.

## Prompt sugerido para manana (actualizado)

Usar este prompt:

`Lee docs/prompt_de_reanudacion.md, docs/contexto_avance.md, docs/Hoja_de_ruta_madre.md, docs/acta_canonizacion_admin_v2_operativa.md y docs/playbook_canonizacion_admin_v2.md. El storefront ya quedo cerrado por ahora en formato produccion. Retomemos el pendiente del dashboard proveedor: al crear producto, permitir cargar hasta 3 imagenes opcionales en el mismo flujo de alta (sin tener que guardar, buscar y luego adjuntar). Implementa backend+frontend end-to-end, con validaciones, UX clara y build en verde.` 

---

## Actualizacion 2026-04-27

Estado operativo actualizado:

- Quedo cerrado el pendiente de proveedor para alta con imagenes en el mismo flujo.
- Al crear producto, ahora se pueden cargar hasta 3 URLs de imagen opcionales en el mismo formulario.
- Backend valida limite y esquema `http/https`, y guarda la primera imagen como principal automaticamente.
- Acta tecnica:
  - `docs/acta_proveedor_alta_producto_con_imagenes_v1.md`

Siguiente foco recomendado:

- continuar con el siguiente remate por rol (revendedor) o hardening operativo fino.

Prompt sugerido para retomar:

`Lee docs/prompt_de_reanudacion.md, docs/contexto_avance.md, docs/Hoja_de_ruta_madre.md y docs/acta_proveedor_alta_producto_con_imagenes_v1.md. Ya quedo resuelto el alta proveedor con imagenes iniciales en el mismo flujo. Retomemos ahora el siguiente remate del panel revendedor (o el hardening operativo mas urgente) manteniendo backend/frontend en verde.` 

---

## Actualizacion 2026-04-27 (categorias comerciales)

Estado operativo actualizado:

- Quedo operativa una base real de categorias comerciales de producto.
- Admin ya puede crear, activar y desactivar categorias desde `/admin/configuracion`.
- Proveedor ya puede seleccionar categoria comercial al crear/editar producto.
- Si no existe la categoria, proveedor puede sugerirla en el mismo flujo y queda registrada para revision admin.
- Acta tecnica:
  - `docs/acta_categorias_producto_admin_y_proveedor_v1.md`

Siguiente foco recomendado:

- continuar por el remate del panel `revendedor`.

Prompt sugerido para retomar:

`Lee docs/prompt_de_reanudacion.md, docs/contexto_avance.md, docs/Hoja_de_ruta_madre.md, docs/acta_proveedor_alta_producto_con_imagenes_v1.md y docs/acta_categorias_producto_admin_y_proveedor_v1.md. Ya cerramos proveedor con alta de imagenes y categorias comerciales administrables. Continuemos ahora con el siguiente remate funcional del panel revendedor, manteniendo backend/frontend en verde.` 

---

## Actualizacion 2026-04-27 (revendedor difusion WhatsApp sin API)

Estado operativo actualizado:

- Ya existe un MVP de difusion comercial en `Red comercial`.
- La vista ya consume productos reales del preview comercial del revendedor.
- Se implemento compartir sin API oficial de WhatsApp:
  - `navigator.share` cuando el dispositivo/browser lo soporta
  - fallback a `wa.me` cuando no hay Web Share
- Se implemento lista/presupuesto compartible por link:
  - seleccion manual de productos
  - link de catalogo con `?lista=...`
  - soporte de `asesor` opcional por query param
- El catalogo publico ya interpreta `lista` y conserva `lista/asesor/storefrontHost` al limpiar filtros.
- Acta tecnica:
  - `docs/acta_implementacion_revendedor_difusion_whatsapp_sin_api_v1.md`

Siguiente foco recomendado:

- cerrar atribucion de asesor con identificador robusto (no solo slug)
- definir si el presupuesto compartible evoluciona a entidad persistida con vencimiento
- sumar resumen por asesor en `Facturas y movimientos`

Prompt sugerido para retomar:

`Lee docs/prompt_de_reanudacion.md, docs/contexto_avance.md, docs/Hoja_de_ruta_madre.md, docs/acta_diseno_revendedor_comercial_difusion_y_presupuesto_v1.md y docs/acta_implementacion_revendedor_difusion_whatsapp_sin_api_v1.md. Ya quedo implementado el MVP de difusion revendedor sin API oficial de WhatsApp (Web Share + wa.me) y lista compartible por link. Continuemos con el siguiente cierre del panel revendedor: atribucion robusta de asesor y evolucion de presupuesto compartible.` 

---

## Actualizacion 2026-04-27 (asesores + presupuestos + redes)

Estado operativo actualizado:

- `Red comercial` ya no es una sola superficie plana:
  - ahora separa `Presupuestos` y `Redes sociales`
- el revendedor ya puede gestionar asesores desde la misma vista:
  - alta
  - activacion/desactivacion
  - seleccion para campaña
- backend ya expone endpoints reales de asesores por revendedor:
  - `GET /revendedores/{revendedorId}/asesores`
  - `POST /revendedores/{revendedorId}/asesores`
  - `PUT /revendedores/{revendedorId}/asesores/{asesorId}`
- los links compartidos ahora pueden incluir:
  - `asesorId` (atribucion robusta)
  - `asesor` (nombre visible)
- storefront conserva esos params en catalogo/detalle/checkout y checkout ya manda `asesorVentaId` cuando `asesorId` es valido.
- acta tecnica:
  - `docs/acta_implementacion_revendedor_asesores_presupuestos_redes_v1.md`

Siguiente foco recomendado:

- resumen de ventas/comisiones por asesor en `Facturas y movimientos`
- decidir si presupuesto pasa a entidad persistida (con vencimiento/version) o se mantiene como link derivado

Prompt sugerido para retomar:

`Lee docs/prompt_de_reanudacion.md, docs/contexto_avance.md, docs/Hoja_de_ruta_madre.md, docs/acta_diseno_revendedor_comercial_difusion_y_presupuesto_v1.md y docs/acta_implementacion_revendedor_asesores_presupuestos_redes_v1.md. Ya esta operativo el bloque de asesores en red comercial y la separacion en presupuestos/redes con atribucion por asesorId hasta checkout. Continuemos con resumen por asesor en facturas/movimientos y cierre de trazabilidad comercial.` 
