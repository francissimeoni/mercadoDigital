# Mercado Digital - Contexto de Avance

## Uso de este documento

Este archivo funciona como bitacora breve de reanudacion.

No reemplaza:

- `docs/especificacion.md`
- `docs/contexto_proyecto.md`
- `docs/Hoja_de_ruta_madre.md`
- actas en `docs/`

Su objetivo es responder rapido:

- donde quedo el trabajo
- que estado real tiene el proyecto
- que conviene hacer a continuacion

## Estado actual resumido

### Negocio

Mercado Digital es una plataforma B2B2C orientada a dropshipping privado white-label.

Reglas madre:

- MD no vende productos
- el revendedor vende al cliente final
- el proveedor no debe quedar expuesto
- todo pago entra por PSP
- backend = fuente de verdad

Aclaracion funcional vigente:

- el revendedor es dueno de una tienda white-label de Mercado Digital
- los clientes compran en esa tienda a precio comercial
- el mismo revendedor tambien puede comprar desde su propia operacion, pero a costo
- esa diferencia de pricing ya quedo implementada en catalogo interno, detalle interno y pedido interno autenticado

Aclaracion vigente de superficies:

- `www.mercado-digital.com.ar` debe leerse como portal madre administrativo/operativo
- cada revendedor tiene su propio storefront publico en su dominio
- `VITE_STOREFRONT_HOST` sirve en local para simulacion fija de storefront, no para entrar al admin madre
- en frontend ya existe acceso explicito al portal madre via `/portal` y `/portal/login`
- desde admin ya se puede abrir una simulacion local de storefront por dominio usando `?storefrontHost=...`, sin editar el `env` para cada prueba

Referencia principal:

- `docs/especificacion.md`

### Estado tecnico general

Backend:

- muy avanzado
- base de dominio fuerte
- storefront publico, compra publica, PSP, tracking, logistica, RMA y proveedor ya existen
- suite backend en verde
- pendiente de hardening real y validacion comercial E2E
- el PSP publico fue migrado de Mobbex a Mercado Pago Checkout Pro
- backend ahora tambien deja diagnostico de arranque para Mercado Pago:
  - variables faltantes
  - webhook en localhost
  - return URL local solo apta para prueba local

Frontend:

- muy avanzado
- operativo interno amplio
- storefront publico ya bastante completo
- admin/logistica/proveedor combinan superficies reales con placeholders honestos
- compra interna del revendedor ahora va directo por producto y cantidad, sin carrito en esta fase

### Hallazgos vigentes

- `docs/Hoja_de_ruta_madre.md` fue corregida para reflejar mejor el estado real del repo
- el flujo PSP publico esta mas avanzado de lo que decia la hoja vieja
- el frontend interno no debe considerarse completamente cerrado
- el backend ya no tiene la barrera de tests rotos, pero todavia no debe considerarse "cerca de produccion tecnica" sin validacion E2E real
- quedo implementada la base real de la v1 del modulo de markup/comision del revendedor
- el gap principal de ids demo en login ya quedo cerrado con contexto auth real desde backend
- el onboarding local de tienda white-label ya quedo funcional desde admin: alta, edicion, publicacion, simulacion local y upload de assets
- la integracion trabada de Mobbex fue reemplazada por Mercado Pago Checkout Pro manteniendo `checkoutUrl` y webhook como fuente de verdad

## Punto actual de trabajo

Foco recomendado actual:

1. validacion comercial real E2E
2. hardening backend
3. cierre de gaps internos reales
4. consolidacion funcional del markup revendedor v1
5. consolidacion del admin real de revendedores/storefront
6. validacion manual del nuevo contexto auth real

Modulo actual de referencia:

- validacion comercial real del flujo publico + PSP + webhook + operacion posterior

## Gaps concretos hoy

- modulos admin/logistica/proveedor todavia parciales en algunas rutas
- modulo markup/comision del revendedor ya implementado en base, pendiente de validacion funcional manual mas expresiva
- ya existe pantalla admin real para alta/edicion de revendedor y storefront, y ahora tambien acceso directo a la simulacion local del storefront por dominio
- el admin ya permite subir assets de storefront al backend para completar logo, hero y favicon sin URLs externas
- el siguiente frente importante recomendado ya vuelve a ser negocio duro: validacion comercial real del flujo publico y PSP
- al mismo tiempo, ya no debe leerse como si el motor base siguiera "por demostrar":
  - compra
  - webhook simulado
  - logistica
  - tracking
  - mails
  - garantia / RMA
  - ya tuvieron validacion local integral satisfactoria
- la validacion PSP real sigue bloqueada si no hay:
  - `MERCADOPAGO_ACCESS_TOKEN`
  - `MERCADOPAGO_WEBHOOK_URL` publico accesible
  - tunel HTTPS activo o backend publicado
- ya quedo implementada la base admin para Mercado Pago por revendedor/storefront con persistencia propia y fallback global
- el flujo publico y el webhook ya pueden resolver token por revendedor cuando exista `mercadoPagoUserId`
- ya quedo implementado el flujo OAuth base de Mercado Pago:
  - generacion de link desde admin
  - callback backend
  - guardado automatico de tokens del revendedor
- el frontend ya puede apuntar a backend publicado con `VITE_API_BASE_URL`, sin depender de `http://localhost:8080` hardcodeado
- ya quedo documentada una recomendacion funcional v1 para el waterfall economico:
  - cobro por cuenta del revendedor
  - split logico persistido como fuente de verdad
  - evolucion posterior a split automatico real
- ya quedo aclarado tambien:
  - el revendedor ve costo compuesto sin desglose interno
  - el fee PSP queda trasladado al cliente final via precio publico
  - el envio lo cobra MD y luego puede conciliarlo con proveedor si despacha directo
  - la liberacion a proveedor se recomienda en estado `RETIRADO` con override manual auditable
- backend ahora ya persiste `monto_psp` por item dentro del split logico, aunque `monto_md` y `monto_envio` todavia siguen pendientes de implementacion real
- backend ya tiene base real para liquidacion proveedor:
  - estado persistido en split
  - liberacion automatica al marcar `RETIRADO`
  - liberacion manual excepcional con motivo
- ya quedo implementada la configuracion admin real del costo MD por categoria:
  - persistencia propia
  - pantalla admin operativa
  - integracion con pricing, ofertas y split
- `monto_md` ya se completa para pedidos nuevos usando la categoria de paquete
- backend ahora ya baja tambien `monto_envio` real:
  - `pedido.totalEnvio`
  - `envio.costoEnvio`
  - `split.montoEnvio`
- tambien quedo corregido el gap entre preview/oferta y pedido real respecto del costo logistico
- la formula vigente queda ahora asi:
  - `PC = costo proveedor + PSP + comision MD`
  - `markup` sobre `PC`
  - `envio` separado
  - `seguro` separado
- ya quedo implementado tambien un descuento v1 del revendedor:
  - codigo configurable desde `pricing-v1`
  - descuento fijo opcional en storefront
  - aplicado solo contra el markup del revendedor
  - persistido en pedido como `totalDescuento` y `codigoDescuentoAplicado`
- ya quedo validado localmente un E2E operativo muy fuerte:
  - compra publica
  - Checkout Pro sandbox hasta apertura real
  - webhook simulado
  - tracking publico
  - proveedor
  - logistica
  - rotulo PDF
  - cierre en `ENTREGADO`
- esa validacion local ya escalo tambien a una corrida integral mas fuerte:
  - compra sobre producto con 2 proveedores
  - eleccion correcta del proveedor por mejor oferta
  - aislamiento correcto por actor y por storefront
  - mails correctos al cliente
  - reclamo de garantia posterior
  - gestion completa de RMA hasta resolucion y reemplazo
  - cierre final del reemplazo validado
- ya quedo validado tambien multi-proveedor:
  - se forzo una segunda oferta sobre el mismo producto canonico
  - el storefront paso a resolver precio/seguro/imagen segun la nueva mejor oferta
  - el pedido nuevo entro solo en la bandeja del proveedor correcto
  - el proveedor anterior no vio ese pedido
- ya quedo preparada tambien la base local para multi-storefront:
  - segundo revendedor/storefront demo publicado
  - dominio demo alternativo: `www.vidrieraurbana.com`
  - usuario demo alternativo: `revendedor.tenant2@md.local`
  - cobertura backend para relacion `ClienteRevendedor` en tenant correcto
  - retorno post-pago frontend acotado al `revendedorId` activo para no mezclar checkout persistido entre tiendas
- el frente multi-storefront/tenant ya tuvo validacion manual real satisfactoria:
  - segundo storefront operando
  - cliente creado en el tenant correcto
  - pedido visible solo en el storefront correcto
  - proveedor correcto ve el pedido y el incorrecto no
  - tracking publico endurecido por `storefrontHost` para evitar cruce entre tenants
  - mails reales saliendo por SMTP del storefront correcto
  - operacion completa validada entre revendedor, proveedor y logistica
- la matriz de notificaciones internas ya quedo parcialmente implementada y reencuadrada segun negocio:
  - sin exponer proveedor ↔ revendedor
  - `PEDIDO_CREADO`: proveedor + revendedor
  - `PAGO_CONFIRMADO`: revendedor
  - `ENVIO_LISTO_PARA_RETIRO`: revendedor + logistica
  - `RETIRADO`, `DESPACHADO`, `ENTREGADO`: revendedor
  - cada bloque deja auditoria admin explicita
- tambien quedo definida una matriz full v2 para el cierre operativo:
  - mantiene la separacion proveedor ↔ revendedor
  - consolida auditoria admin en todos los hitos
  - propone completar mails del cliente final para `LISTO_PARA_RETIRO`, `RETIRADO`, `DESPACHADO` y `ENTREGADO`
  - deja mails a revendedor/proveedor/logistica como optativos de etapa siguiente
- tambien quedo lista una base real de mails transaccionales por storefront:
  - transporte `log` o `smtp`
  - `emailComercial` del storefront reutilizable como remitente preferido/reply-to
  - fallback seguro a configuracion global si el storefront no define email
- el circuito de mails cliente final tambien dio un cierre operativo mas sano:
  - se mantienen `PEDIDO_CREADO` y `PAGO_CONFIRMADO`
  - se agregan `ENVIO_DESPACHADO` y `ENVIO_ENTREGADO`
  - todos reutilizan el mismo tracking publico como enlace canonico
- el tracking publico tambien ya dio un paso de UX orientado a cliente final:
  - menos ficha tecnica y mas lectura guiada del estado actual
  - timeline simple y didactico del pedido
  - reutilizacion del mismo enlace para postventa/garantia cuando el pedido ya fue entregado
- RMA/postventa tambien dio un salto estructural:
  - el tracking publico ya permite abrir y seguir casos de postventa
  - admin recibe detalle enriquecido de compra y conversacion del caso
  - ya existe base real para mensajes entre cliente y admin, mas notas internas
  - ahora tambien soporta adjuntos de imagen persistidos en base de datos
  - ahora ya existe boton admin `Solicitar envio`
  - ahora ya existen rotulo PDF de devolucion y etiqueta PDF de reposicion
  - ahora ya existe una politica sobria de 3 mails del caso:
    - `caso recibido`
    - `ya puedes enviar el producto`
    - `tu caso fue resuelto`
  - sigue pendiente el remate de tracking especifico de devolucion/reposicion
- el onboarding admin ahora ya puede dejar una tienda mucho mas operativa:
  - usuario/login real del revendedor desde admin
  - SMTP propio por storefront
  - preservacion de passwords al editar sin reingresarlas
- sigue pendiente seguir afinando la formula comercial final del costo compuesto y decidir si el descuento evolucionara a multiples cupones o reglas mas complejas
- pendientes funcionales inmediatos detectados:
  - revisar carga/admin real de proveedores para evitar pruebas manuales por base
  - completar validacion manual de la bandeja `/notificaciones` por rol con evidencia mas formal si se quiere dejar acta final
  - decidir si mails transaccionales posteriores a `pago confirmado` para cliente final se implementan ahora o en etapa siguiente
- tambien quedo corregido el bug de checkout persistido detectado en storefront publico:
  - el retorno post-pago ya limpia el snapshot local del checkout asociado al `trackingToken` conciliado
  - no se rehidratan pedidos ya pagados al volver a entrar al checkout del mismo producto dentro del mismo tenant
  - el tracking publico queda como superficie correcta para continuar viendo el pedido confirmado
- tambien se aplico un ajuste tecnico al layout de logistica:
  - las grillas principales ya no saltan a configuraciones anchas demasiado pronto dentro del layout con sidebar
  - retiros, despachos y dashboard pasan a breakpoints mas conservadores para evitar compresion y columnas desfasadas en notebooks
  - queda pendiente la validacion visual/manual final sobre el modulo ya ajustado
- RMA/postventa dio otro salto operativo:
  - backend ahora expone hitos auditados del caso para cliente/admin
  - el tracking publico diferencia mejor devolucion de dinero vs reposicion
  - admin ya puede adjuntar comprobantes o evidencias al resolver el caso
  - la impresion del rotulo de reposicion deja auditoria propia
  - la UI admin de RMA ya endurece mejor las acciones visibles segun estado del caso y evita recargas completas innecesarias al navegar desde la bandeja
  - queda formalizado como v1 el circuito de reposicion manual trazada dentro del mismo RMA:
    - solicitud a proveedor
    - recepcion en MD
    - reenvio al cliente con tracking real
    - entrega final
  - backend ahora expone una mini bitacora explicita de reposicion con fecha, observacion, tracking y transportista por hito
  - frontend admin y tracking publico ya muestran esa bitacora de reemplazo como bloque propio
  - se reconstruyo `RmaServiceTest` con cobertura real del circuito de reposicion manual
  - ahora tambien queda formalizado un subestado operativo de reposicion para admin y storefront:
    - `PENDIENTE_SOLICITUD_PROVEEDOR`
    - `ESPERANDO_INGRESO_MD`
    - `LISTA_PARA_REENVIO`
    - `EN_CAMINO_AL_CLIENTE`
    - `ENTREGADA_CLIENTE`
  - backend ahora expone tambien un resumen textual del reemplazo y un siguiente paso mas preciso en casos resueltos con reposicion
  - frontend admin y tracking publico ya consumen ese estado resumido para evitar leer solo fechas sueltas o inferencias desde la bitacora
  - ahora tambien salen mails transaccionales del reemplazo en los hitos de mayor valor:
    - cuando el reemplazo se reenvia al cliente
    - cuando la entrega final del reemplazo queda confirmada
    - ambos usan branding del storefront correcto
    - ambos reutilizan tracking canonico del pedido y exponen tracking real del reemplazo cuando aplica
  - el frente de transportistas en runtime quedo mas robusto:
    - si la lista no carga o viene vacia, logistica y RMA pueden seguir operando con nombre manual
    - backend resuelve o crea el transportista automaticamente
  - si se quisiera profundizar mas, el siguiente paso seria modelar un envio real de reposicion como circuito logistico separado
- tambien arrancamos el cierre visual de vistas sobre superficies reales, respetando la convencion mobile-first:
  - bandeja `RMA` ahora resume mejor volumen, estados y siguiente paso
  - `logistica/envios` ahora prioriza mejor lectura en una columna, pedido activo y accion siguiente
  - tarjetas de retiros y despachos ya muestran foco operativo mas claro sin depender de desktop
  - `RmaDetalle` ahora abre con snapshot rapido del caso antes de bajar al acordeon operativo
  - el tracking publico ahora resume mejor estado actual, proximo paso y caso de postventa seleccionado en mobile
  - el detalle publico de producto ahora abre mejor con snapshot comercial rapido en mobile
  - el checkout publico ahora resume mejor producto y total estimado antes de bajar al formulario y al cierre
  - la home storefront ahora deja mas claro desde mobile que la tienda sirve para comprar y seguir pedidos
  - el catalogo storefront ahora resume mejor resultados y recorrido comercial antes de entrar al grid
- ya quedo tambien una pasada final de cierre visual sobre el rol `logistica`:
  - dashboard mas claro, con foco actual explicito y menos redundancia entre bloques
  - bandeja de envios mas fluida para mobile, con flujo corto y mejor resumen del pedido activo
  - cards de retiros y despachos mas sobrias, con menos repeticion
  - modulos preparados mejor alineados al resto del panel sin confundirse con operacion real
- tambien quedo una primera pasada fuerte sobre el rol `admin`:
  - dashboard reorganizado alrededor de foco actual, prioridades y accesos reales
  - `revendedores` ahora deriva mejor al editor real de `storefronts`
  - `storefronts` ya puede abrir con `revendedorId` preseleccionado desde enlaces internos
  - `RMA` admin ahora soporta mejor filtros/contexto por query y abre mas limpio en mobile
  - `notificaciones` ya diferencia mejor cuando el usuario actual es admin o proveedor
  - se corrigio el acceso de navegacion admin a `RMA`, que seguia apuntando a `/rma`
- modulo proveedor tambien cerro un pendiente concreto de UX operativa:
  - alta manual de producto ahora admite hasta 3 URLs de imagen opcionales en el mismo flujo de creacion
  - backend valida limite y esquema `http/https`
  - la primera imagen se marca principal automaticamente
- tambien quedo cerrada una base real de categorias comerciales para producto:
  - admin ya puede crear, activar y desactivar categorias de producto desde configuracion
  - proveedor ya puede seleccionar categoria comercial al crear/editar producto
  - si no existe, proveedor puede sugerirla en el mismo flujo y queda pendiente de revision admin
- ya arranco el primer MVP funcional del remate `revendedor` sobre difusion comercial:
  - `Red comercial` ahora consume productos reales del preview comercial (`pricing-v1`)
  - se agrego compartir por WhatsApp sin API paga:
    - primero `navigator.share`
    - fallback `wa.me` cuando no hay soporte
  - se agrego lista/presupuesto compartible por link:
    - seleccion de productos
    - enlace de catalogo con `?lista=...`
    - soporte de `asesor` opcional por query param
  - el catalogo publico ahora reconoce `lista` y preserva `lista/asesor/storefrontHost` al limpiar filtros
  - acta tecnica: `docs/acta_implementacion_revendedor_difusion_whatsapp_sin_api_v1.md`
- el remate `revendedor` ya sumo una capa operativa adicional fuerte:
  - backend ahora expone gestion de asesores por revendedor:
    - `GET /revendedores/{revendedorId}/asesores`
    - `POST /revendedores/{revendedorId}/asesores`
    - `PUT /revendedores/{revendedorId}/asesores/{asesorId}`
  - `Red comercial` ahora quedo separada en dos secciones:
    - `Presupuestos`
    - `Redes sociales`
  - el revendedor ya puede crear/activar/desactivar asesores desde la misma vista
  - los links comerciales ahora llevan `asesorId` (ademas de nombre visible) cuando hay asesor seleccionado
  - storefront ya conserva `asesorId` en el recorrido catalogo -> detalle -> checkout
  - checkout publico ya envia `asesorVentaId` cuando llega un `asesorId` valido en query
  - acta tecnica: `docs/acta_implementacion_revendedor_asesores_presupuestos_redes_v1.md`

## Documentos que leer al retomar

Orden recomendado:

1. `docs/prompt_de_reanudacion.md`
2. `docs/especificacion.md`
3. `docs/contexto_proyecto.md`
4. `docs/Hoja_de_ruta_madre.md`
5. `docs/contexto_avance.md`

## Proximo paso sugerido

Cuando se retome el trabajo, conviene:

- confirmar en que modulo exacto se quiere avanzar
- revisar si corresponde acta nueva o solo actualizacion de hoja madre
- si el trabajo toca backend critico, mantener la suite en verde mientras se avanza
- si se sigue con markup v1, priorizar validacion funcional real del dashboard revendedor y decidir si hace falta o no una etapa de edicion masiva
- si se sigue con admin/storefront, validar manualmente alta, edicion, publicacion y reflejo real en tenant/storefront publico
- si se sigue con auth/hardening, validar manualmente login de admin, revendedor, proveedor y cliente revisando que cada uno reciba su contexto operativo real
- si se sigue con PSP real, usar `docs/playbook_validacion_comercial_publica_psp.md` como checklist corto operativo
- para la decision de migracion de proveedor PSP, ver `docs/acta_migracion_psp_mercadopago_checkout_pro_v1.md`
- para la base admin por revendedor, ver `docs/acta_mercadopago_revendedor_admin_v1.md`
- para el cierre de URLs/variables de servidor, ver `docs/acta_cierre_mercadopago_servidor_urls_y_variables_v1.md`
- para ejecutar el despliegue real de Mercado Pago en servidor, ver `docs/playbook_despliegue_mercadopago_servidor.md`
- para la definicion funcional del waterfall economico, ver `docs/acta_diseno_waterfall_economico_mercadopago_v1.md`
- para la mejora tecnica del split logico con PSP por item, ver `docs/acta_backend_split_logico_monto_psp_v1.md`
- para la base backend de liquidacion proveedor, ver `docs/acta_backend_liquidacion_proveedor_v1.md`
- para la configuracion admin del costo operativo MD, ver `docs/acta_admin_configuracion_costo_md_v1.md`
- para la persistencia real del envio en split logico, ver `docs/acta_backend_split_logico_envio_v1.md`
- para el descuento storefront del revendedor, ver `docs/acta_descuento_revendedor_v1.md`
- para la validacion local integral del flujo comercial/operativo, ver `docs/acta_validacion_local_e2e_checkout_pro_y_operacion_v1.md`
- para la validacion local integral ya incluyendo multi-proveedor, ownership por actor y garantia/RMA, ver `docs/acta_validacion_local_e2e_integral_comercial_operativo_y_rma_v1.md`
- para la matriz inicial de notificaciones y mails, ver `docs/acta_matriz_notificaciones_y_mails_v1.md`
- para la matriz full consolidada de notificaciones y mails, ver `docs/acta_matriz_notificaciones_y_mails_v2.md`
- para los mails cliente final conectados al tracking en hitos logisticos, ver `docs/acta_mails_cliente_tracking_hitos_logisticos_v1.md`
- para la redefinicion UX del tracking publico orientado a cliente final, ver `docs/acta_tracking_publico_experiencia_cliente_v1.md`
- para el salto de RMA hacia postventa publica y gestion admin, ver `docs/acta_rma_v2_postventa_publica_y_gestion_admin_v1.md`
- para el cierre operativo de adjuntos + rotulos + base de reposicion en RMA, ver `docs/acta_rma_v2_adjuntos_rotulos_y_reposicion_v1.md`
- para el cierre de mails sobrios de postventa/RMA, ver `docs/acta_rma_v2_mails_postventa_sobrios_v1.md`
- para el aviso automatico cuando el reemplazo ya fue reenviado, ver `docs/acta_rma_v2_mail_reemplazo_reenviado_v1.md`
- para el aviso automatico cuando el reemplazo ya fue entregado, ver `docs/acta_rma_v2_mail_reemplazo_entregado_v1.md`
- para el tracking mas claro de devolucion/reposicion y cierre operativo de RMA, ver `docs/acta_rma_v2_tracking_devolucion_y_cierre_operativo_v1.md`
- para el subestado operativo resumido del reemplazo dentro de RMA, ver `docs/acta_rma_v2_estado_operativo_reposicion_v1.md`
- para el cierre visual mobile-first de bandejas operativas, ver `docs/acta_fix_vistas_mobile_operativas_v1.md`
- para la pasada final del rol logistica, ver `docs/acta_fix_panel_logistica_mobile_final_v1.md`
- para la primera pasada fuerte del rol admin, ver `docs/acta_fix_panel_admin_mobile_y_conexiones_v1.md`
- para el fallback operativo de transportistas cuando la lista no aparece en runtime, ver `docs/acta_logistica_transportista_manual_runtime_v1.md`
- para el cierre v1 del alta proveedor con imagenes en el mismo flujo, ver `docs/acta_proveedor_alta_producto_con_imagenes_v1.md`
- para la base v1 de categorias comerciales admin/proveedor, ver `docs/acta_categorias_producto_admin_y_proveedor_v1.md`
- para el MVP de difusion comercial revendedor sin API oficial de WhatsApp, ver `docs/acta_implementacion_revendedor_difusion_whatsapp_sin_api_v1.md`
- para el cierre de asesores + secciones `presupuestos/redes` + atribucion por `asesorId`, ver `docs/acta_implementacion_revendedor_asesores_presupuestos_redes_v1.md`
- para ejecutar una prueba manual concreta del reemplazo en garantia, ver `docs/playbook_validacion_rma_reposicion_manual_v1.md`
- para la base local del frente multi-storefront/tenant, ver `docs/acta_base_local_multistorefront_tenant_v1.md`
- para la base de mails SMTP reales por storefront, ver `docs/acta_storefront_mails_smtp_real_v1.md`
- para el onboarding real de usuario revendedor + SMTP propio, ver `docs/acta_onboarding_storefront_usuario_y_smtp_v1.md`
- para el hardening del tracking por host correcto, ver `docs/acta_hardening_tracking_multistorefront_host_v1.md`
- para el ajuste de notificaciones internas sin exponer proveedor ↔ revendedor, ver `docs/acta_ajuste_notificaciones_internas_sin_exposicion_revendedor_proveedor_v1.md`
- para el fix del checkout persistido post pago, ver `docs/acta_fix_checkout_persistido_post_pago_v1.md`
- para el ajuste tecnico del layout de logistica, ver `docs/acta_fix_layout_logistica_v1.md`
- antes de cerrar split productivo, definir waterfall real de fondos y responsabilidades economicas

## Regla de mantenimiento

Actualizar este archivo cuando cambie alguna de estas cosas:

- modulo actual
- principal bloqueo
- prioridad inmediata
- estado general real del proyecto

Mantenerlo corto.
