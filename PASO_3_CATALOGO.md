# Paso 3: cierre de nuevas compras y continuidad del historial

Fecha: 24/11/2026. Evolución funcional autorizada sobre el proyecto reconstruido.

## Estado de la entrega

Implementado en local: cierre de accesos públicos y privados al circuito anterior para tiendas en modo catálogo, recuperación de referencias históricas sin montar checkout, reanudación explícita de pagos existentes con token y host, y simplificación del panel y de la cuenta.

La lista `CATALOGO_PUBLICO_REVENDEDORES` continúa vacía por defecto. No se activaron tiendas reales, desplegaron cambios, reiniciaron servicios existentes ni ejecutaron migraciones. Original y rescate quedan fuera de las escrituras de este trabajo. No se utilizaron servicios o credenciales productivos.

**367 pruebas backend**, **23 pruebas de catálogo**, **23 de transición** y **11 regresiones** correctas. Compilaciones backend/frontend correctas. Chrome con API, sesión y tenant sintéticos verificó navegación e interfaz. Las cifras finales y comprobaciones de conservación se registran en `artifacts/paso-3-catalogo/verificacion-final.json`.

**No acredita un recorrido navegador → Java → MySQL ni preparación para producción.** Persisten los bloqueos de pagos históricos y base real descritos más abajo.

## Reglas conservadas

- Un precio público ARS calculado por `CatalogoPrecioCalculator`: ARS original o USD por cotización propia, más categoría ARS solamente cuando no incluye envío. Sin PSP, comisión, seguro ni tarifa logística anterior.
- Publicación independiente del stock, respetando actividad y configuración completa. Sin cantidades ficticias, precios alternativos ni conversión duplicada en el frontend.
- Selector existente de WhatsApp, responsable destacado, elección expresa y revalidación. Abrirlo no crea pedido, cliente, pago, reserva ni envío. El visitante decide enviar el mensaje.
- Roles, autenticación, Google, contratos de callbacks, estados de pagos y migraciones anteriores conservados. Los mecanismos de autorización siguen en el servidor.

## Matriz de accesos

Las rutas de API son relativas a su base. “Anterior” significa tienda fuera del modo catálogo; los proveedores conservan su recorrido aplicable.

| Acceso / finalidad | Modo catálogo | Modo anterior | Actor / permiso | Evidencia |
| --- | --- | --- | --- | --- |
| Catálogo, ficha y enlaces compartidos | Precio único y selector existentes; sin checkout ni stock público | Experiencia anterior | Visitante, tienda resuelta por host | 23 pruebas de catálogo, Chrome y compilación |
| `/producto/:id/checkout` y `/resumen-compra` sin pedido | Vuelve a ficha con explicación; no monta pantallas ni efectos de compra | Revalida modo e identidad por servidor antes de montar | Visitante; resolución actual por host | Transición DOM y Chrome |
| Mismas URLs con referencia histórica | Va a `/pedido/:token`, que consulta al backend con host | Mantiene checkout anterior | Token de seguimiento y validación del servidor | DOM, Chrome y servicios HTTP reales con repositorios simulados |
| `POST /public/pedido/directo` | 409 antes de buscar/crear cliente | Contrato anterior | Visitante; validaciones del servicio | Bloqueos del paso 2 nuevamente ejecutados |
| `POST /pedidos`, `/pedidos/canonico` y variantes internas | 409 antes de crear pedido o tocar stock | Contratos anteriores | Roles de operación existentes | Pruebas de servicios y suite completa |
| `/catalogo/:id` privado y `/pricing` | Aviso de catálogo y enlace a pedidos anteriores, sin montar formularios antiguos | Formularios anteriores tras verificar modo e ID | Sesión de revendedor y ownership del endpoint de configuración | DOM, Chrome del gate y compilación de AppRouter |
| Escrituras de `pricing-v1`, ajuste masivo de precios anteriores | 409 con orientación a Catálogo | Comportamiento anterior | Ownership y roles existentes | `CatalogoCierreAnteriorTest` y regresiones |
| Importador administrativo anterior | No se ofrece; API rechaza antes de repositorios o transacciones de filas | Importación anterior | Administrador; autorización existente del controlador | Prueba de bloqueo, suite del importador y panel DOM/Chrome |
| `/mi-cuenta` | Pedidos anteriores, perfil, seguimiento y comprobantes; aclara que WhatsApp no registra ventas automáticamente | Textos y experiencia anteriores | Cliente autenticado, filtrado por cuenta y tienda existente | DOM, Chrome y suite de cuenta del backend |
| `/mi-tienda`, productos y configuración | Accesos claros a catálogo, contactos y difusión; ayudas de carga; pedidos/cobros anteriores conservados | Panel anterior | Responsable/administrador autorizado de esa tienda | DOM y Chrome con sesión sintética; backend de ownership conservado |
| `/pago/retorno` | Busca solamente referencias de la tienda resuelta; consulta estado real y conduce al seguimiento | Conserva retorno anterior, con búsqueda acotada a tienda | Token y host | DOM y Chrome |
| `GET /public/pedido/:token` | Sigue disponible con validación de token, origen público y dominio; respuesta `no-store` | Disponible | Portador del token de esa tienda | HTTP/seguridad/servicio reales, repositorios simulados |
| `POST /public/pedido/:token/reanudar-pago` | Nueva entrada explícita para pedido existente, validado por token y host | Compatible, sin cambiar estados | Token de seguimiento, host y validaciones PSP existentes | Nueve casos nuevos de controlador, junto a la suite PSP existente |
| PSP anterior por UUID, transferencia por token, comprobantes, entrega, callbacks y notificaciones | Conservados para operaciones existentes; no agregan creación de pedidos | Conservados | Permisos y contratos anteriores | Regresiones locales; revisión integral pendiente en paso 4 |
| Consultas/preguntas y disponibilidad anteriores | Se conserva su gestión y compatibilidad; no se ofrecen como requisito del contacto por WhatsApp ni condicionan publicación | Conservadas | Contratos anteriores | Inventario y suite de regresión; no se reescribió ese módulo |

## Borradores, referencias y efectos

`CatalogoCompraGate` permite cerrar el circuito antes de montar checkout o resumen. Cuando el contexto dice modo anterior, consulta nuevamente `/public/storefront/resolve`; durante carga, error, identidad incorrecta o respuesta incompleta no monta la compra. La respuesta cacheada no se acepta como verificación inicial. Una pestaña que cambió de modo después de esa consulta sigue cubierta por los bloqueos de escritura del backend y por el detalle público.

No hay borrado global ni migración destructiva de almacenamiento. Se lee la clave existente del producto y de `REVENDEDOR:<id>` o `PROVEEDOR:<id>`. Un token guardado solo permite elegir el destino de navegación: el servidor valida su existencia, origen y tienda. No se utilizan importes, destinatarios ni URLs de pago guardados como autoridad.

- Sin token utilizable: vuelve a la ficha. Si hay ID de pedido sin token, ofrece consultar cuenta o enlace recibido.
- Con token: seguimiento independiente de que el producto siga publicado o tenga stock. Una referencia inválida muestra error; no genera una compra de reemplazo.
- JSON ilegible o almacenamiento no disponible al leer: se trata como ausencia de referencia, sin eliminar otras claves.
- Retorno PSP sin tienda resuelta: no busca “el último pedido” de todas las tiendas.
- En modo catálogo, el retorno conserva la referencia y conduce al seguimiento después de consultar el backend. El `status` de la URL no confirma un pago.
- Un fallo de preparación PSP en el checkout anterior conserva el pedido creado y su enlace de consulta; ya no limpia el ID para volver a ofrecer crear otro. Esto no cambia la cancelación que el servicio PSP anterior pueda realizar ante un fallo; el estado debe consultarse en el backend.

No se restauran automáticamente formularios de nuevas compras ni se prepara un pago al recuperar una referencia histórica en modo catálogo.

## Reanudación histórica y autorización

El nuevo endpoint de reanudación usa `PublicPedidoTrackingService.getPedidoPublicoByTrackingToken(token, host)` y luego el servicio PSP existente con el ID obtenido del servidor. No acepta el ID guardado por el navegador como autorización. Un host ajeno, token inexistente u origen operativo no llega a la preparación PSP.

Se mantienen las condiciones existentes: pedido `CREADO`, pago `PENDIENTE`, método `PSP`. Pedido cancelado o pago confirmado: 409. Si ya hay preferencia se reutiliza; no se genera una al consultar o recargar seguimiento. No se modificaron la integración PSP ni sus reglas de cancelación o estados.

La pantalla requiere pulsar “Retomar pago pendiente”; después ofrece un enlace HTTPS devuelto por el servidor para ese pedido. No navega automáticamente ni utiliza la URL de sessionStorage. Hay bloqueo durante solicitud, error y reintento.

Tracking agrega `transferenciaToken` únicamente para una transferencia pendiente de pedido `CREADO` sin comprobante, y `comprobanteCargado`. Se obtienen después de validar token y tienda. Así se conserva el enlace al comprobante sin confiar en metadatos guardados. No se expone el archivo ni una lista de tokens. La carga mantiene su contrato anterior y requiere revisión integral en el paso 4.

**Límite:** estas pruebas acreditan la validación añadida y la reutilización secuencial de una preferencia existente. No acreditan idempotencia distribuida ni concurrencia real entre pestañas, servidor y PSP.

## Panel y terminación visual

- “Configurar catálogo”, “Contactos y difusión”, “Pedidos anteriores” y “Cobros anteriores” identifican las tareas actuales y las históricas. El acceso antiguo `?tab=precios` abre la configuración nueva; no muestra el ajuste anterior.
- La selección del panel se deriva de la URL, corrigiendo atrás/adelante. Los pedidos y las acciones históricas siguen accesibles. Panel y cuenta esperan la resolución de tienda antes de redirigir o consultar datos; los enlaces guardados no se pierden durante la carga inicial. Hay estados de error y reintento.
- Guardar fichas invalida también la lista administrativa de precios y las consultas públicas: una ficha nueva no queda oculta por la caché al pasar a configurarla.
- Productos explica la secuencia ficha → precio/moneda/envío, con acceso directo a Catálogo. Activar una ficha no promete que esté publicable si aún falta configuración.
- El precio anterior ARS sigue siendo obligatorio por el contrato heredado, identificado expresamente como dato que no se publica. En altas de catálogo empieza vacío y exige entrada explícita; no se escribe un cero por omisión ni se copia USD. Admite centavos. **La creación completamente unificada permanece pendiente.**
- Stock y tipo de paquete logístico anterior no se solicitan en el formulario de catálogo. El campo técnico de paquete se conserva en el contrato heredado y no determina el envío ni el precio público nuevo.
- El importador y ajuste masivo anteriores no se presentan como herramientas de precio del catálogo. No se implementó un ajuste masivo nuevo en este paso.
- Cobros y transferencias explican su vínculo con operaciones anteriores. No se borran cuentas PSP, alias ni snapshots de pedidos al activar catálogo.
- Cuenta identifica historial, conserva perfil y acciones útiles, añade reintentos y aclara que las conversaciones de WhatsApp no aparecerán como ventas automáticas.
- Cabecera con acceso a catálogo; menú móvil hasta 768 px para evitar comprimir enlaces. Footer y texto general de inicio usan el recorrido de contacto. Los textos comerciales personalizados se conservan para revisión por tienda.
- Revisadas capturas de ficha redirigida, seguimiento, cuenta y panel. Se ajustó la tarjeta móvil de pedido para separar total y descripción. El selector existente conserva Escape y retorno de foco.

## Pruebas y reproducción

Evidencias nuevas en `artifacts/paso-3-catalogo/`; no se sobrescribieron informes ni artefactos de pasos anteriores.

| Validación | Resultado y alcance |
| --- | --- |
| Backend `mvn.cmd -B package` | 367 correctas, cero fallos/errores/omitidas; 14 nuevas sobre las 353 anteriores. JAR generado. |
| Nuevas pruebas backend | Token/host, origen público, cancelado/confirmado, preferencia ya existente, comprobante, importación, ajustes y modo de configuración. Seguridad HTTP real en la nueva prueba de controlador; repositorios y proveedor PSP simulados. |
| Frontend `npm.cmd run build` | TypeScript/Vite correctos. Persiste advertencia de tamaño del bundle. |
| Catálogo `npm.cmd run test:catalogo` | 23 correctas; conservación del cálculo/formato, selector, stock y modo anterior. |
| Transición `npm.cmd run test:catalogo-transicion` | 23 correctas; React, router y DOM reales con transporte, tenant y sesión simulados. |
| Regresión recuperada | 11 correctas, precios masivos del modo anterior y tracking. |
| Chrome | API local simulada; borradores, resumen, historial, retorno, reintento explícito, recarga, modo anterior/proveedor, cuenta, atrás/adelante, panel, gate privado y selector. Sin creación de pedidos, pagos externos, mensajes ni excepciones JS. |
| Lint | Línea base 30 errores/19 advertencias; final 30/18. Sin diagnósticos nuevos según comparación por archivo, regla, severidad y primera línea del mensaje. No se declara lint limpio. |
| Conservación | Inventario por SHA-256 respecto de 961 archivos de la línea base; documentos de pasos 1/2, migraciones, POM y lock sin cambios. Dependencias conservadas. |

Comandos desde cada carpeta:

```powershell
# backend
mvn.cmd -B package

# frontend
npm.cmd run build
npm.cmd run test:catalogo
npm.cmd run test:catalogo-transicion
npm.cmd run test:catalogo-transicion-browser

# raíz
node --test recuperacion/frontend-precios.test.mjs recuperacion/frontend-tracking.test.mjs
node artifacts/paso-3-catalogo/verificar-entrega.mjs
```

El navegador usa un perfil temporal exclusivo bajo los artefactos del paso 3, bloquea solicitudes externas y comprueba la ruta antes de eliminar ese perfil. Las sesiones son sintéticas: no acredita inicio real de sesión ni OAuth. La vista de comprobante del arnés es una indicación de ruta; no se ejecutó carga de archivos real. El gate privado se prueba con sesión simulada; la protección de roles de AppRouter se conserva, pero no se presenta como una prueba integral del shell privado.

MockMvc ejecuta los controladores, filtros JWT y servicios Java indicados sin iniciar un servidor HTTP conectado a una base. Esa evidencia se mantiene separada de la del navegador. El navegador final comprobó **17 recorridos**, sin desborde horizontal en seguimiento, cuenta y panel a 390 px; el seguimiento también se comprobó a 1280 px.

`git diff --check` mantiene observaciones heredadas en archivos que este paso no cambió. La comprobación de conservación distingue esas observaciones de los archivos modificados en esta entrega; no se declara limpio todo el árbol de trabajo.

Fallos encontrados y corregidos durante la validación: un paréntesis extra del arnés DOM; una aserción del navegador que esperaba un `form` aunque el primer paso del checkout anterior usa inputs; un escape de selector del arnés Chrome; y una ruta demasiado amplia del arnés anterior que volvía a montar el gate en su propio destino. Se corrigieron los arneses y mantuvieron sus comprobaciones de comportamiento. Las trazas iniciales y finales se conservan. Se ajustó también una dependencia de efecto; no se omitieron pruebas para obtener resultados verdes.

## Archivos afectados

El inventario completo, con tipo de cambio y SHA-256 final de **40 archivos**, está en [archivos-paso-3.json](artifacts/paso-3-catalogo/archivos-paso-3.json). Grupos principales:

| Área | Archivos principales |
| --- | --- |
| Accesos | `CatalogoCompraGate.tsx`, `CatalogoOperacionGate.tsx`, `AppRouter.tsx`, `storefrontCheckoutState.ts` |
| Historial | `StorefrontHistoricalPayment.tsx`, páginas de tracking, retorno, checkout, resultado y cuenta; API/tipos de tracking |
| Backend histórico | `PublicPedidoPagoPspController`, `PublicPedidoTrackingController/Service`, `PublicPedidoTrackingResponse` |
| Restricciones anteriores | `CatalogoModoService`, `RevendedorProductoService`, `RevendedorPricingV1Service`, `AdminRevendedorProductoImportService`, contrato de configuración de pricing |
| Interfaz | Panel propietario, cabecera, footer, layout, inicio, login y aviso de ficha |
| Verificación | Dos clases nuevas de pruebas Java, adaptación de mocks existentes, arnés de transición DOM y navegador, ajuste del arnés público y scripts npm |
| Documentación | Este informe, sección añadida a `INFORME_RECUPERACION.md` y artefactos propios del paso 3 |

## Pendientes y bloqueos para un futuro despliegue

**Implementado con validación parcial:** persistencia y transacciones de la reconstrucción. No se encontraron MySQL/MariaDB, Docker/Podman ni servicios locales disponibles. No se ejecutaron migraciones ni se validaron JPA, bloqueos o concurrencia con base real. No hay migraciones nuevas en este paso; V55/V56 y las anteriores permanecen intactas.

**Paso 4, pagos históricos:**

1. Resolver y probar el defecto heredado de confirmar una transferencia de un pedido cancelado. Su prueba de caracterización reproduce producción; no justifica desplegarlo.
2. Revisar autorización del endpoint PSP legado por UUID, alcance de comprobantes por token y endurecimiento compatible. El endpoint nuevo sí valida tracking y host, pero no reemplaza ni certifica las superficies antiguas.
3. Probar reintentos concurrentes, idempotencia PSP, callbacks/webhooks, cancelaciones, reembolsos y notificaciones con integración local aislada. Revisar la cancelación por error PSP y consistencia de stocks/pagos/envíos.
4. Revisar la clasificación visual del retorno anterior para estados distintos de `PENDIENTE` y `CONFIRMADO`: el código heredado infiere confirmación de “no pendiente”. El modo catálogo conduce al seguimiento y no usa esa inferencia; el modo anterior conserva su implementación hasta esa revisión.

**Paso 5, revisión de tiendas/productos:** preparar contactos, cotizaciones, categorías, precios y condiciones; revisar textos personalizados que prometan checkout, seguro o logística anterior. No inferir moneda ni envío incluido. Decidir la unificación de ficha y precio sin campos ficticios y el alcance de una futura importación/edición masiva del catálogo. No hay ventas por WhatsApp cargadas automáticamente como pedidos.

**Paso 6, validación integral:** MySQL desechable con migraciones y datos sintéticos, navegador con Java y base reales, roles/autenticación reales locales, altas/ediciones/publicación y concurrencia; configuración propia del futuro entorno y revisión operativa. Incluir solicitudes en curso y pestañas abiertas durante un cambio de modo: los ensayos actuales de contexto desactualizado y bloqueo de escritura no reproducen un cambio real entre instancias del servidor. Resolver deuda técnica necesaria para desplegar y verificar reversión por tienda: quitar un UUID no repara datos de fichas creadas sin stock para el checkout anterior.

No se declara equivalencia con producción ni autorización para activar o desplegar.
