# Recuperación de MercadoDigital

Fecha: 20 de noviembre de 2026, America/Buenos_Aires.

## Evolución posterior: catálogo público y WhatsApp (24/11/2026)

Se implementó el paso 2 del catálogo independiente, con precio público ARS por tienda, publicación sin stock, selector de contactos, revalidación antes de WhatsApp y bloqueos de nuevas compras para tiendas habilitadas. El modo queda desactivado por defecto y no se activó ninguna tienda real. Es una evolución funcional autorizada, distinta de recuperar producción. Evidencias, migración V56, validaciones y límites en [PASO_2_CATALOGO.md](PASO_2_CATALOGO.md). MySQL e integración completa siguen pendientes; no habilita un despliegue. Se conserva debajo la entrega del paso 1 y el informe histórico.

## Evolución posterior: preparación del catálogo (24/11/2026)

Se implementó el paso 1 autorizado del catálogo independiente: configuración privada de precios ARS/USD, cotización por tienda, categorías comerciales de envío, contactos WhatsApp y formularios administrativos. Es una evolución funcional posterior a la reconstrucción, no una recuperación de comportamiento productivo. El nuevo recorrido público sigue sin activarse. Evidencia, pruebas, migración V55, límites y pendientes del paso 2 en [PASO_1_CATALOGO.md](PASO_1_CATALOGO.md). Se conservan a continuación el informe de reconstrucción y sus pendientes históricos.

## Revisión posterior a Sol: estado actual

**Reconstrucción ampliada y revisada; todavía NO lista para un futuro despliegue.** Esta sección reemplaza el estado y los pendientes de la entrega inicial conservada más abajo como historial. Se trabajó exclusivamente en `C:\Sistemas\MercadoDigital-Reconstruido`, sin conectarse al VPS, desplegar, reiniciar servicios ni ejecutar migraciones productivas. Se conservaron los cambios de la copia de Sol.

### Recuperado y corregido en esta revisión

| Área | Resultado y evidencia productiva |
| --- | --- |
| Precios masivos | Interfaz conectada: búsqueda por nombre/SKU, categorías, selección visible que conserva los seleccionados ocultos, porcentajes entre -99,99 y 1000 distintos de cero, redondeo, importes manuales, totales, confirmación y limpieza tras éxito. `PA-832434.js` y `XR`; implementación en `StorefrontBulkPrices.tsx`. Se preservó la edición individual. |
| Pedidos del propietario | Filtro inicial de activos, filtro de cancelados, cancelación únicamente con pedido CREADO y pago PENDIENTE/RECHAZADO, confirmación explícita y refresco. Etiqueta solo con pago confirmado; reparación de preparación para transferencia confirmada con envío PENDIENTE_PAGO. Método, importe, referencia e imágenes. `MA-856881.js`. |
| Enlaces desde notificaciones | Apertura de `?tab=pedidos&pedido=...`, expansión y desplazamiento al pedido; recarga de pedidos cada 30 segundos, parámetros de pestaña. `AA-814803.js` y `MA`. |
| Checkout | Conservada y revisada la fórmula productiva de recargo por unidad y cantidad. Recuperadas las pantallas posteriores de transferencia y PSP: aclaran que el pedido está pendiente de pago, instrucciones, comprobante, seguimiento, contacto, aviso WhatsApp con enlace al pedido del propietario, errores y enlace para abrir Mercado Pago. `bR-728948.js`; `StorefrontCheckoutResult.tsx`. |
| Ficha de producto | Precio de contado/transferencia y precio de tarjeta con recargo solo cuando Mercado Pago está habilitado. Fórmula de `qR-773334.js`. La banda gráfica de marcas de tarjetas no se reprodujo. |
| Tracking | Reemplazado el flujo RMA antiguo de esa página por productos con enlace a publicación, transportista, seguimiento de cuatro etapas, confirmación de entrega desde QR, WhatsApp y correo de ayuda directa. `OR`, `UR`, `_R`, `tc`. Copia de la página previa en `recuperacion/Tracking-antes-revision.tsx`; el backend RMA y sus otros consumidores no fueron eliminados. |
| Google y cuenta | Revisados callback, canje, validadores, perfil e historial contra `KA`, `FA`, `VA`, `GA`, `QA` y clases recuperadas. Sin sustituir la política de autenticación de producción. Pruebas nuevas de identidad firmada, límites de roles, renovación y aislamiento de tienda. |
| Constructores generados | La revisión con `javap -public -s` detectó orden incorrecto de parámetros en `StorefrontService` y `NotificacionInternaEmailService`. Se reordenaron sus campos finales para que Lombok reproduzca los constructores originales. Tras recompilar, las firmas públicas de **485 clases**, incluidas clases internas/builders, no presentan diferencias. Esto no demuestra equivalencia de implementación. |

### Validaciones de esta revisión

- **Backend:** `mvn.cmd -B package` correcto, **273 pruebas, 0 fallos, 0 errores, 0 omitidas**. Evidencia: `recuperacion/logs/revision-backend-final.log`. Son 20 casos adicionales sobre los 253 anteriores.
- **Google:** siete casos usan RSA, un servidor HTTP y JWKS reales sobre `127.0.0.1` con puerto efímero y credenciales ficticias. Callback válido, PKCE transmitido y rechazo del segundo callback; rechazo de emisor, audiencia, nonce, vencimiento, firma y correo no verificado. No se conectaron a Google. Persistencia y creación de usuario están simuladas.
- **Autenticación y cuenta:** cuenta operativa que entra por Google recibe exclusivamente CLIENTE y mantiene ese límite al renovar; MVC con filtro JWT real rechaza anónimos y revendedores en historial de clientes y rechaza clientes en precios/cancelación del propietario; cuenta sin relación activa no expone perfil/historial; consulta de pedidos usa usuario y tienda y filtra origen público. No equivale a una prueba JPA real.
- **Precios y pagos:** duplicados rechazados antes de escribir, consulta bloqueante por producto y tienda, redondeo HALF_UP sin alterar stock; comprobante PDF enviado como octet-stream se guarda en directorio temporal, emite evento y mantiene pago pendiente; confirmación de transferencia repara preparación; cuatro estados PSP se traducen a RECHAZADO/REEMBOLSADO.
- **Frontend:** TypeScript y Vite correctos (`revision-frontend-final.log`), con la advertencia de tamaño del bundle. **11 pruebas correctas** (`revision-frontend-tests.log`): cinco trazas de selección/filtros/importes comparadas directamente contra la función productiva PA y seis casos de tracking contra UR, incluido el requisito de QR y confirmación manual. El arnés simula hooks y renderizado: **no es una prueba en navegador ni contra una API HTTP real**.
- **Lint:** 30 errores y 19 advertencias; comparación con los fuentes antiguos y la misma instalación de ESLint sin diagnósticos nuevos (`revision-lint-comparado.log`, `comparacion-lint.json`). No se declara lint limpio.
- **Migraciones:** V54 se cotejó con la entidad: UUID CHAR(36), hashes CHAR(64), longitudes de nonce/verificador/host/retorno, fechas y restricciones únicas de state/ticket. El repositorio declara PESSIMISTIC_WRITE para ambas búsquedas y el servicio usa transacciones. Los 45 recursos siguen verificados por SHA-256. **No se ejecutó Flyway ni se probó concurrencia de base de datos:** no se encontraron ejecutables/servicios MySQL, MariaDB, Docker o Podman disponibles.
- **Originales:** los SHA-256 del JAR rescatado y del JAR antiguo siguen siendo `f597e376489d1733472f45bc14fe2a08bcc8cd438e2250499be5f1ff35ca27e5` y `230af9122a4cd689bb03ccabee7362be850f96d5b459de7d59f260ddbd112933`.

La primera suite ampliada falló en cuatro casos nuevos de webhook porque la prueba enviaba tipo PAGO_CONFIRMADO con un estado rechazado/reembolsado. La evidencia de `MercadoPagoWebhookAdapter` indica PAGO_ACTUALIZADO; se corrigió el fixture, no el servicio ni las expectativas de negocio. El fallo está conservado en `revision-backend-primer-intento.log`. El primer arnés de frontend tuvo una recursión accidental en su recorrido de nodos; se corrigió el arnés y luego pasaron los 11 casos. El primer comando de inventario de firmas excedió la longitud de comandos de Windows; se dividió en lotes de 25 clases.

### Revisión de las cinco diferencias de descompilación

`revision-diferencias-metodos.txt` muestra las líneas concretas diferentes. En `NotificacionInternaEmailService`, `StorefrontAuthService.loginClienteExistente` y `TransferenciaService.validarArchivo`, las expresiones y condiciones son iguales y cambia la separación de asignaciones encadenadas en temporales. En `RotuloEnvioService.generarQr` cambian variables temporales de retorno; se conserva la creación de la imagen y el cierre del stream. No se identificó un cambio de comportamiento en esos cuatro casos, sin afirmar una demostración formal.

Para `WebhookPspService.procesarEstadoNoAprobado` se inspeccionó además bytecode: las ramas REJECTED/CANCELLED cargan RECHAZADO; REFUNDED/CHARGED_BACK cargan REEMBOLSADO; luego todas guardan en la misma variable local (offset 177). Confirma la corrección realizada por Sol frente a una representación defectuosa de CFR. Los cuatro casos ahora tienen prueba. Evidencia en `revision-bytecode-produccion.txt` y `revision-bytecode-reconstruido.txt`.

### Problema heredado confirmado: bloqueo para desplegar

**Una transferencia con comprobante de un pedido CANCELADO puede confirmarse mediante `TransferenciaService.confirmar`.** El servicio verifica tienda, método y comprobante, pero no rechaza el estado cancelado. Cambia el pedido a PAGADO y el pago a CONFIRMADO, mientras el envío permanece CANCELADO. La cancelación ya puede haber devuelto el stock. La interfaz productiva también conserva la acción de confirmar cuando hay comprobante y el pago no está confirmado.

Esto existe en el compilado productivo y se reprodujo mediante `TransferenciaRecoveryTest.caracterizacionProduccionPermiteConfirmarTransferenciaDePedidoCancelado`. La prueba es una **caracterización del defecto, no una garantía deseada**. Se conservó la conducta para no inventar una política de recuperación. Antes de desplegar se debe decidir e implementar explícitamente la prohibición o el flujo de reactivación y stock, incluyendo carreras con confirmaciones/webhooks. La reparación de preparación de un pago ya confirmado también vuelve a emitir EventoPagoConfirmado; revisar duplicación de notificaciones según la política acordada.

### Aproximaciones y pendientes reales

1. **Resolver el defecto de transferencias canceladas** anterior y verificar las transacciones con stock y eventos. Sería una corrección de producto, distinta de reconstruir fielmente el binario.
2. **MySQL descartable:** ejecutar las 42 migraciones, validar JPA y actualización desde un esquema previo a V54; probar dos canjes concurrentes y rollback de un ajuste masivo cuando un producto no pertenece a la tienda. Los mocks no comprueban bloqueos ni atomicidad real. El rescate no contiene el esquema/datos efectivos de producción.
3. **Integración de extremo a extremo:** navegador con backend y base locales; login/callback/refresh y expiración, navegación entre roles/tiendas, compra/transferencia/comprobante/confirmación/cancelación, importes UI/API y retorno de PSP. Los contratos están revisados y las pruebas locales pasan, pero esos recorridos completos no se ejecutaron.
4. **Notificaciones y pagos externos:** probar entrega asíncrona después de commit con SMTP simulado y ciclos PSP simulados, reintentos/idempotencia y fallos. Esta revisión prueba emisión de eventos y ramas de servicio, junto con las pruebas existentes de correo; no valida un diálogo SMTP ni cobros/retornos reales.
5. **Fidelidad visual y frontend restante:** precios, bandeja, resultado de checkout y tracking recuperan las funciones descritas, con JSX legible y composición visual aproximada. No hay comparación pixel a pixel. Quedan diferencias de presentación del propietario (diseño, ampliación de imágenes y otras pestañas antiguas), marcas de tarjetas, y un inventario exhaustivo de funciones fuera de los flujos priorizados. No se afirma que todo el bundle sea equivalente.
6. **Calidad y operación:** resolver lint heredado en un cambio separado; completar configuración local/secretos propios de un futuro entorno, uploads y datos. No copiar credenciales productivas. Verificar que los requisitos operativos del futuro despliegue estén definidos antes de prepararlo.

Los artefactos de esta revisión son el JAR en `backend/target/` y `frontend/dist/`, ambos nuevos y distintos del rescate. Scripts de comparación reutilizables: `revision-bytecode.mjs`, `revisar-diferencias.mjs`, `frontend-precios.test.mjs`, `frontend-tracking.test.mjs`. `revision-interfaces.mjs` es un registro de edición de una sola ejecución: **no volver a ejecutarlo** sobre el proyecto ya revisado.

---

## Entrega inicial de Sol (historial; estado y pendientes sustituidos por la revisión anterior)

Se creó un proyecto editable en `C:\Sistemas\MercadoDigital-Reconstruido`, con el backend reconstruido y parte del frontend recuperada. **No se declara equivalencia funcional completa con producción ni se considera listo para desplegar.** La interfaz aún mezcla elementos locales anteriores con funciones recuperadas; los pendientes concretos están al final.

El backend compila, genera el JAR y pasa **253 pruebas, sin fallos, errores ni pruebas omitidas**, incluidas nueve pruebas nuevas de recuperación. El frontend pasa TypeScript y Vite. El lint general conserva problemas anteriores: 30 errores y 19 advertencias. La comparación con los mismos fuentes antiguos usando la misma instalación de ESLint no encontró diagnósticos nuevos.

No se desplegó, reinició ni migró producción. Tampoco se ejecutó el JAR recuperado contra servicios reales. Las pruebas usaron mocks y contextos MVC de prueba, sin levantar una base de datos ni hacer intercambios OAuth, cobros o envíos SMTP reales.

## Origen y conservación

- Proyecto base: `C:\Sistemas\MercadoDigital`.
- Compilados originales rescatados: `C:\Sistemas\MercadoDigital-Rescate-VPS-20261120`.
- JAR productivo SHA-256: `f597e376489d1733472f45bc14fe2a08bcc8cd438e2250499be5f1ff35ca27e5`.
- Frontend activo original: `assets/index-Blt9f1I_.js`.
- Los archivos compilados productivos tienen fecha remota del 25/9/2026. Los locales anteriores eran del 9/9/2026.

La copia se hizo desde el árbol de trabajo, incluyendo cambios sin confirmar, archivos sin seguimiento y `.git`. No se hizo checkout ni se restauraron archivos desde un commit. Se omitieron `node_modules` y directorios `target`; las dependencias y los compilados se regeneraron. El proyecto base y el rescate se conservaron sin modificaciones. La situación inicial de Git de la copia quedó en `recuperacion/estado-inicial-git.txt`.

## Evidencia y método

Se utilizó [CFR 0.152](https://www.benf.org/other/cfr/) descargado desde el sitio del autor. SHA-256 de la herramienta: `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`.

Se extrajeron las clases y dependencias de ambos JAR y se descompilaron sus clases propias. Se compararon primero las versiones descompiladas entre sí, para no confundir todo el ruido del descompilador con cambios de negocio. En los archivos existentes se trasladaron los miembros diferentes y los imports necesarios, preservando los demás miembros del fuente. Las clases nuevas se reconstruyeron desde el descompilado, usando Lombok para getters, setters y builders. Se retiraron métodos antiguos que ya no figuraban en producción, incluidos el antiguo constructor de transferencias y la sobrecarga anterior de creación de pedidos propios.

El `pom.xml` se recuperó directamente de `META-INF/maven`, y los recursos desde `BOOT-INF/classes`. **Los 45 recursos del backend —incluidos los YAML y las 42 migraciones SQL— coinciden por SHA-256 con los del JAR de producción.** No se ejecutaron las migraciones.

La recompilación también se descompiló para comparar los miembros de las 364 clases propias contra la producción. El diagnóstico final registra diferencias textuales en cinco clases. Esa comparación ignora el orden de miembros y los miembros Lombok marcados como generados; no sustituye una prueba de equivalencia ni una revisión de las APIs generadas. Sus resultados se conservan para revisión.

Archivos de evidencia:

| Archivo/directorio | Contenido |
| --- | --- |
| `recuperacion/plan-miembros.json` | Miembros nuevos y modificados identificados antes de editar |
| `recuperacion/miembros-recuperados.json` | Registro de las operaciones iniciales por clase |
| `recuperacion/miembros-obsoletos.json` | Inventario histórico de miembros retirados o candidatos a retirar |
| `recuperacion/archivos-reconstruidos.json` | Diferencias en `backend/src`, `frontend/src`, POM y `.gitignore`, con hashes |
| `recuperacion/recursos-verificados.json` | Comprobación de los 45 recursos productivos |
| `recuperacion/comparacion-recompilada.json` | Diferencias textuales tras recompilar y descompilar |
| `recuperacion/descompilado-local/` | Descompilación de la versión local antigua |
| `recuperacion/descompilado-produccion/` | Descompilación productiva original |
| `recuperacion/descompilado-final/` | Descompilación del backend reconstruido final |
| `recuperacion/frontend/` | Inventario y fragmentos del bundle activo, separados por función |
| `recuperacion/comparacion-lint.json` | Comparación de los diagnósticos contra la base antigua |
| `recuperacion/logs/` | Compilaciones, pruebas y errores encontrados durante el trabajo |

Los scripts de transferencia de miembros reflejan el procedimiento usado. **No son una automatización idempotente:** no ejecutar de nuevo `recuperar-miembros.mjs` ni `ajustar-pruebas.mjs` sobre la entrega. Los correctivos posteriores y la revisión están incorporados en los fuentes finales. Los directorios binarios y descompilados se excluyeron del seguimiento de Git, pero permanecen disponibles en disco.

## Backend recuperado con evidencia

- OAuth de Google: configuración, controladores de inicio/callback/canje, validación JWT, PKCE, nonce, expiración, tickets y repositorio con bloqueo pesimista. Incorporación de la migración V54 y la dependencia `spring-security-oauth2-jose`.
- Sesiones de cliente con roles limitados, refresh con roles explícitos y rechazo del registro que antes podía apropiarse de cuentas de invitados sin contraseña. Se conservó la conducta observada en el JAR; aún requiere la revisión de seguridad integral indicada abajo.
- Cuenta de cliente: lectura y edición de perfil e historial de hasta 100 pedidos, filtrado por cliente, revendedor y origen público directo. Protección de `/cliente/storefront/**` con rol `CLIENTE`.
- Precios masivos: contratos de entrada, validación de IDs repetidos, actualización con bloqueo y redondeo a dos decimales.
- Cancelación de pedidos impagos: reposición de stock y estados cancelados de pedido, pago y envío.
- Medios de pago y recargo PSP para productos propios, datos adicionales en la respuesta pública del storefront y retorno de Mercado Pago basado en el pedido.
- Estados rechazados y reembolsados del webhook y notificaciones asociadas, evitando procesar pagos sobre pedidos cancelados.
- Confirmación de entrega por tracking y host de tienda, equivalencia entre dominios con/sin `www`, notificaciones al vendedor y etiquetas PDF con QR. Dependencia ZXing `core:3.5.3` recuperada del POM productivo.
- Correos de postcompra asíncronos, nuevos eventos de transferencia y comprobante, datos comerciales de la tienda, imágenes, firma, SMTP propio y tiempos de espera.
- Preservación de la configuración de correo de una tienda existente al editarla desde administración; imágenes y metadatos adicionales de pedidos y tracking.

Se corrigieron defectos de representación de CFR sin introducir comportamientos nuevos: inferencia de genéricos, variables de asignaciones/ternarios y switch, cierre de `ByteArrayOutputStream` y duplicación de anotaciones de validación entre declaración y tipo. Las pruebas nuevas comprueban que las restricciones no quedaron duplicadas.

## Frontend recuperado e interpretaciones

El análisis utilizó el parser de TypeScript para extraer funciones y cadenas del bundle activo. No se incorporó al contexto el bundle completo. Los archivos JS extraídos conservan los símbolos minificados y sus offsets originales en el nombre.

Se recuperaron los contratos y el flujo de:

- Acceso con Google y consulta de disponibilidad; inicialización por correo/modo de registro y destino interno de retorno en el login.
- Página `/ingresar/google`, canje de ticket sin reintentos, conservación de la sesión y navegación al destino validado.
- Página `/mi-cuenta`, acceso restringido al cliente, historial, perfil de contacto/entrega, edición, seguimiento, carga de comprobante y ayuda por WhatsApp.
- Rutas de tenant y enlace de cuenta del encabezado.
- Confirmación de entrega desde `?confirmarEntrega=1`, con confirmación manual, disponibilidad únicamente en estado despachado y refresco del tracking.
- Selección explícita de medio de pago en checkout, disponibilidad según storefront y recargo PSP redondeado por unidad y luego por total, según las funciones `bR` y `fm` productivas.
- Contratos adicionales de tracking/pedidos y clientes HTTP de cancelación de impagos y actualización masiva de precios.

**Los JSX y tipos TypeScript reconstruidos son una interpretación legible de la evidencia, no los fuentes originales recuperados.** El diseño de la cuenta y callback es aproximado. Se preservó el diseño anterior de las demás páginas y parte de su comportamiento, que debe reconciliarse con el frontend productivo.

Los clientes de precios masivos y cancelación están implementados, pero **sus nuevas interfaces de selección, vista previa, filtros y acciones de pedidos todavía no están conectadas**. La pantalla local de precios sigue usando su flujo individual anterior. No se presenta esto como una recuperación completa de la interfaz.

## Validaciones ejecutadas

| Comprobación | Resultado | Evidencia |
| --- | --- | --- |
| Compilación inicial de la copia antigua | Correcta | `logs/backend-baseline.log` |
| Primera compilación recuperada | Falló por representación de CFR; corregido | `logs/backend-recuperado-compile.log` y siguientes |
| Primera suite ejecutable | 244 pruebas, 5 fallos y 15 errores | `logs/backend-tests-tercera.log` |
| Suite ajustada a contratos productivos | 244 pruebas correctas | `logs/backend-tests-ajustadas.log` |
| OAuth/stock: pruebas nuevas iniciales | 7 correctas | `logs/backend-tests-recuperacion.log` |
| Empaquetado final con suite completa | **253 correctas**, JAR generado | `logs/backend-package-y-tests-final.log` |
| `npm.cmd run build` | TypeScript y Vite correctos; advertencia por bundle grande | `logs/frontend-build-final.log` |
| `npm.cmd run lint` | 30 errores y 19 advertencias | `logs/frontend-lint.log` |
| Lint antiguo/reconstruido con el mismo ESLint | Misma cantidad; sin diagnósticos nuevos | `comparacion-lint.json` |

El lint dirigido a los últimos archivos editados también devuelve código 1 por problemas heredados de `StorefrontCheckoutPage.tsx` (cuatro errores y una advertencia); no debe interpretarse como un lint limpio. Detalle: `logs/frontend-lint-ultimos-cambios.log`.

Las expectativas antiguas se ajustaron únicamente cuando el JAR demostraba un contrato diferente: mocks de nuevas dependencias, creación del pedido propio con método/recargo, emisión del evento de compra pública solo al solicitar transferencia (el checkout PSP pendiente no lo emite), remitente de correo según SMTP propio y contacto comercial vacío cuando la tienda no lo configuró. No se deshabilitaron las pruebas que fallaban.

Las nueve pruebas nuevas cubren PKCE/state/nonce y retorno externo, ticket vencido, tienda incorrecta, alias `www` y consumo único secuencial, cancelación que recupera stock una sola vez, rechazo de cancelación pagada/de otra tienda, y restricciones de precios/perfil. El bloqueo concurrente real del repositorio no se ejercitó sobre una base de datos.

Artefactos locales: `backend/target/backend-0.0.1-SNAPSHOT.jar` y `frontend/dist/`. Son compilaciones nuevas de esta reconstrucción, diferentes de los originales productivos. No deben confundirse con los originales del rescate.

## Pendientes concretos para Astra, en orden sugerido

1. **Interfaz del propietario:** reconstruir selección, búsqueda, filtros, ajuste porcentual, vista previa editable y confirmación masiva de precios. Conectar `ajustarPreciosMasivamente`; recuperar acciones y filtros de pedidos cancelados y conectar `cancelarPedidoImpago`. Revisar también datos de pago e imágenes en las bandejas. Usar `frontend-pendientes-extraidos.txt`, el cliente productivo `XR`, el cliente `pA` y sus funciones llamadoras. Los endpoints y backend ya están disponibles.
2. **Pagos y checkout:** contrastar `StorefrontCheckoutPage.tsx` con el fragmento `bR-728948.js`, incluyendo textos, confirmación/retorno, detalle de importes y representación de las opciones. La selección y fórmula de recargo se recuperaron, pero no hay pruebas de navegador ni prueba integral que compare los importes de UI/API/PSP. La ficha de producto `qR-773334.js` muestra precio de contado/transferencia y tarjeta con recargo: esa presentación todavía falta en el fuente local.
3. **Tracking y postventa:** comparar el fragmento `OR-764684.js` con `StorefrontPedidoTrackingPage.tsx`. Producción usa una página simplificada con enlaces de producto y ayuda directa. La reconstrucción agregó confirmación de entrega, pero conserva la interfaz RMA anterior y no reproduce aún toda esa reorganización. Resolver la diferencia usando la evidencia productiva.
4. **Cuenta del cliente y Google:** revisar contratos, roles y aislamiento entre tiendas, callback, canje y refresh; completar pruebas con un JWKS y servidor OAuth simulados. Revisar concurrencia del consumo de tickets sobre MySQL y la imposibilidad de adquirir roles operativos. Comparar presentación de perfil e historial con `FA`, `VA`, `GA` y `QA`; no están restaurados píxel por píxel.
5. **Migraciones e integración:** levantar una base MySQL local descartable, validar el esquema y V54, relaciones JPA y comportamiento transaccional. Ejercitar correos asíncronos y transferencias con SMTP/PSP simulados. Esta etapa no aplicó migraciones ni restauró datos, uploads o configuración privada del VPS.
6. **Diferencias textuales finales del backend:** revisar `NotificacionInternaEmailService.notificarVentaInterna`, `RotuloEnvioService.generarQr`, `StorefrontAuthService.loginClienteExistente`, `TransferenciaService.validarArchivo` y `WebhookPspService.procesarEstadoNoAprobado`. La inspección muestra variables temporales, asignaciones y forma del switch/recursos como causas de las diferencias, pero eso es una interpretación; ampliar pruebas o contrastar bytecode antes de certificar equivalencia. Revisar también APIs/constructores generados por Lombok, excluidos de la comparación textual.
7. **Frontend restante y lint:** contrastar las demás funciones/cadenas productivas con los componentes locales para detectar cambios que no estén en los flujos anteriores. Corregir los problemas de lint heredados en una pasada separada; evitar mezclarlos con decisiones de recuperación sin evidencia.

Para continuar no es necesario volver a buscar carpetas en los discos ni descargar producción. Los originales y las descompilaciones están disponibles. Comenzar por este informe, los pendientes y sus fragmentos, y ejecutar pruebas dirigidas tras cada módulo.

## Evolución posterior: cierre del checkout y continuidad histórica (24/11/2026)

El paso 3 de la evolución a catálogo está documentado en [PASO_3_CATALOGO.md](PASO_3_CATALOGO.md), con evidencias propias en `artifacts/paso-3-catalogo/`. Es lógica nueva autorizada, no una recuperación adicional del compilado productivo.

Se completaron los accesos públicos y privados del modelo anterior para tiendas en modo catálogo; las referencias guardadas de pedidos se conducen al seguimiento validado por token y host, sin recrear compras ni reanudar cobros automáticamente. Se incorporó una entrada explícita para retomar el pago de un pedido existente y se conservaron pedidos, comprobantes y operaciones históricas. El panel y la cuenta distinguen la gestión anterior de las consultas nuevas por WhatsApp. No se borraron pedidos ni configuraciones de pagos, ni se migró moneda o envío por inferencia.

Validaciones locales: backend con 367 pruebas correctas; frontend compilado, 23 pruebas de catálogo, 23 de transición y 11 regresiones. Chrome verificó recorridos con API, tenant y sesión sintéticos. No se validó la integración navegador/Java/MySQL ni pagos externos. Lint mantiene deuda heredada, sin diagnósticos nuevos.

Sigue siendo bloqueante revisar pagos históricos, en especial la confirmación heredada de transferencias de pedidos cancelados; también autorización de superficies PSP anteriores, concurrencia/idempotencia y notificaciones. Las migraciones V55/V56 y las anteriores siguen sin validación en MySQL local. Falta unificar completamente el alta de ficha y precio del catálogo y revisar datos/textos por tienda antes de activar. No se activaron tiendas reales ni se desplegó.

## Evolución posterior: operaciones históricas (24/11/2026)

El [PASO_4_CATALOGO.md](PASO_4_CATALOGO.md) detalla las correcciones y evidencias en `artifacts/paso-4-catalogo/`. Se corrigieron la confirmación de transferencias canceladas, el acceso entre tiendas a pedidos, el endpoint PSP por UUID, la entrada de webhook sin firma, la correlación de reembolsos y los estados engañosos en retorno/seguimiento. Los comprobantes se revalidan bajo bloqueo y los correos internos del webhook se disparan después del commit. Los resultados finales de pruebas constan en el informe del paso 4.

El paso 3 describía estos puntos como pendientes; esta sección actualiza su estado sin borrar aquella línea base. El cierre local ejecutó 383 pruebas backend, 23 de catálogo, 34 de transición y 11 regresiones; compilaciones correctas y seis estados de retorno revisados en Chrome con API sintética. Siguen sin verificación MySQL real, callback sandbox del proveedor ni una garantía distribuida para preferencias aceptadas durante un timeout. La resolución `REEMBOLSADO` del RMA no demuestra devolución de dinero; los cobros tardíos quedan auditados para conciliación. No se activaron tiendas ni se desplegó.

## Evolución posterior: administración unificada del catálogo (24/11/2026)

El [PASO_5_CATALOGO.md](PASO_5_CATALOGO.md) documenta la implementación y sus evidencias en `artifacts/paso-5-catalogo/`. Se unificaron ficha, precio y envío con imágenes en una operación transaccional; se mantuvieron las categorías propias de cada tienda de revendedor, su revisión de impacto y la actualización del precio calculado al cambiar sus importes. Se agregaron control de versión de ficha, recuperación de reintentos de alta y limpieza de archivos nuevos al revertir la transacción.

V57 permite fichas de catálogo sin inventar precio, stock ni paquete del circuito anterior. Se protegen las entradas anteriores frente a estas fichas. Los decimales administrativos se entregan como cadenas JSON y los formularios admiten coma o punto decimal sin separadores de miles. Los contactos existentes permanecen y se incorporó ayuda para revisar textos personalizados sin sobrescribirlos. Esta etapa es evolución funcional nueva autorizada; no una afirmación de equivalencia con los compilados recuperados.

Los resultados de la ejecución final constan en el informe del paso 5 y su `verificacion-final.json`. Siguen pendientes Flyway/JPA y concurrencia real con MySQL, el recorrido navegador/Java/base, la conciliación de archivos tras cierres abruptos y los pendientes de pagos/notificaciones del paso 4. No se activaron tiendas ni se desplegó. Las fichas nuevas no adquieren compatibilidad con checkout por desactivar una bandera: la reversión requiere un plan explícito.

Cierre del paso 5: 408 pruebas backend, 31 de catálogo DOM, 34 de transición y 11 regresiones correctas; TypeScript, Vite y JAR generados. Chrome comprobó la ficha y actualización de categoría en móvil/escritorio y 17 recorridos de transición con API/sesión sintéticas. Lint conserva 30 errores y 18 advertencias de la línea de base, sin nuevos diagnósticos. La migración V57 está escrita y revisada estáticamente, todavía sin ejecución en MySQL.

## Evolución posterior: integración real local y cierre del paso 6 (24/11/2026)

El [PASO_6_CATALOGO.md](PASO_6_CATALOGO.md) documenta la validación con MySQL Community 8.0.44 portable y datos exclusivamente sintéticos. Se aplicaron los 45 archivos de migración disponibles hasta V57 sobre una base vacía y se validó JPA. Otra base se actualizó desde V54 conservando importes/stock anteriores; su copia se restauró y se compararon 45 tablas. Esto actualiza el pendiente local del paso 5: no constituye una migración del VPS ni verifica su historial efectivo.

Se corrigió un problema real de altas simultáneas: REPEATABLE_READ podía conservar una instantánea anterior al bloqueo y devolver conflicto en el reintento idéntico. Las escrituras de catálogo usan ahora READ_COMMITTED con el mismo bloqueo por tienda y las mismas protecciones de versión/unicidad. Se probaron dos procesos Java mediante esperas comprobadas en MySQL. También se corrigieron el rechazo de propiedad que respondía 500 en pedidos históricos, el límite multipart y la exposición de detalles internos en errores inesperados. Se retiraron valores incrustados de proveedor de los archivos de configuración actuales, sin usar esas credenciales.

El cierre ejecutó 411 pruebas backend, 31 de catálogo frontend, 34 de transición y 11 regresiones, con compilaciones correctas. Se añadieron 18 escenarios reales de catálogo y 8 de históricos, incluidos rollback con archivos, carreras transferencia/cancelación y webhooks mediante proveedor HTTP local. Chrome ejercitó login contra Java, imagen y guardado, categoría propia, precio actualizado desde otra sesión y contacto WhatsApp interceptado. Las evidencias distinguen ese recorrido de los navegadores con API simulada de pasos anteriores. Lint conserva 30 errores y 18 advertencias, sin diferencias por archivo/regla/severidad.

El [PLAN_ACTIVACION_CATALOGO.md](PLAN_ACTIVACION_CATALOGO.md) prepara activación gradual y reversión, sin ejecutarlas. Siguen pendientes historial/esquema reales —los fuentes disponibles saltan de V33 a V46—, proveedores en sandbox autorizado, decisiones de conciliación financiera, proxy/HTTPS, almacenamiento y preparación manual de cada tienda. Los listeners asíncronos no son una outbox durable. Quitar la bandera no vuelve compatibles con checkout las fichas nuevas. No se afirma equivalencia con producción ni preparación incondicional para desplegar. No se modificaron originales/rescate ni se activaron tiendas reales.

## Evolución posterior: preparación del piloto Mega Reconquista (24/11/2026)

El propietario confirmó que los precios anteriores de las tiendas incluían envío y eligió Mega Reconquista como piloto. Se identificó mediante lectura del VPS el revendedor `ea37fc94-8f19-4b8b-862d-dc23f71b144d`, dominio `www.megareconquista.com`: 176 productos ARS válidos, 144 activos y 32 inactivos.

Se agregó la herramienta operativa `backend/scripts/PrepararCatalogoInicial.java`, con vista previa, confirmación mediante token, bloqueo transaccional y auditoría. Prepara solamente configuraciones faltantes de una tienda explícita en ARS con envío incluido; conserva configuraciones existentes y deja precios inválidos/otras monedas para revisión. No activa tiendas ni toca datos históricos. No se modificó el código de aplicación ni las migraciones.

Pasaron 12 escenarios sobre MySQL real local: conversión de los 176 precios, rechazo de revisiones obsoletas, rollback, preservación de USD previamente configurado, exactitud decimal, aislamiento, dos procesos concurrentes y API real mostrando los 144 activos con contacto del titular. La copia de artículos empleó únicamente datos necesarios del catálogo y contactos sintéticos. Se retiraron esos fixtures al finalizar. Estos resultados adicionales no se confunden con una nueva ejecución de las 411 pruebas previas.

La inspección productiva resolvió parte de las incertidumbres anteriores: MySQL 8.0.45; historial V1–V54; los 42 archivos disponibles coinciden por SHA-256 y checksum. V34–V45 siguen sin fuentes tras revisar 41 JAR. El perfil productivo ya ignora migraciones faltantes. En un clon local de la estructura real, sin filas de negocio, se verificó exactamente esa lista de faltantes y se reprodujo la política existente: V55–V57 aplicaron y el backend arrancó con JPA validate correcto. No hubo repair ni alteración de checksums. Eso valida el ensayo estructural; no restituye archivos perdidos ni certifica todos los datos productivos.

El alcance, resultados, paquete preparado y procedimiento restante están en [PILOTO_MEGA_RECONQUISTA.md](PILOTO_MEGA_RECONQUISTA.md) y `artifacts/piloto-mega/`. Se completó una copia en lectura de base/archivos/configuración y se restauraron localmente las 46 tablas y el tar; el JAR coincide y las dos copias SQL fueron idénticas. No se arrancaron aplicaciones contra esa copia con datos reales. Nginx valida, pero su límite multipart es 20 MB frente a 26 MB del backend. Las variables Google y de credenciales MP están presentes; falta `MERCADOPAGO_WEBHOOK_SECRET`, requerido por el nuevo verificador de firmas. Ese pendiente debe resolverse antes de sustituir el backend compartido para no afectar pagos de otras tiendas. Siguen pendientes integraciones reales y activación controlada. Durante esta preparación no se desplegó, reinició ni escribió en producción. El catálogo público de Mega Reconquista sigue en su versión anterior.


## 24/11/2026 - Retiro de Mercado Pago y piloto autorizado

El propietario autorizo el despliegue de MercadoDigital y retiro Mercado Pago de toda su operacion, incluidas membresias. Vidra es otra aplicacion del VPS y debe conservar su integracion: sus servicios, archivos, variables y credenciales quedan fuera del alcance. Ver `RETIRO_MERCADO_PAGO.md` y `PILOTO_MEGA_RECONQUISTA.md`.

Validacion posterior al cambio: 432 pruebas backend, 78 frontend, compilacion Java/TypeScript/Vite correctas; prueba HTTP local con ocho bloqueos de operaciones retiradas, cero escrituras y cero solicitudes al proveedor simulado. Se conservan adaptadores historicos para trazabilidad y pruebas explicitas, deshabilitados por defecto. El resultado efectivo de publicacion se documenta por separado; compilar no prueba equivalencia con el binario rescatado.


## Piloto publicado y verificado - 24/11/2026

Release piloto-catalogo-20261125-r4 activa en https://www.megareconquista.com. V55-V57 aplicadas; 176 configuraciones ARS con envio incluido, 144 productos publicados con importes anteriores exactos. No se ajustaron precios por dolar. Solo Mega tiene catalogo activo; Mercado Pago queda deshabilitado en todo MercadoDigital.

La comprobacion real detecto telefono comercial en formato local: se normalizo al mismo destino WhatsApp del boton historico, con auditoria CATALOGO_NORMALIZACION_CONTACTO. Ficha, selector movil y mensaje del producto verificados en Chrome; sin envio de mensajes. API privada protegida e inicio de Google 302, sin completar login con una cuenta real.

Vidra mantiene PID 1374, entorno y 23 archivos identicos, HTTPS 200. No se modificaron ni reiniciaron Vidra, Nginx, Docker o MySQL. No se tocaron credenciales externas compartidas. El respaldo tomado con MercadoDigital detenido se copio localmente, se verifico por SHA-256 y se restauraron sus 46 tablas y V54 sin ejecutar aplicaciones contra datos reales.

Evidencias y pendientes operativos: PILOTO_MEGA_RECONQUISTA.md, RETIRO_MERCADO_PAGO.md y artifacts/piloto-mega/despliegue-resultados.json, verificacion-publica.json, respaldo-final-resultados.json. La reconstruccion no se declara equivalente al binario rescatado: incorpora cambios de producto expresamente autorizados. Quedan pendientes el login completo del propietario, su revision comercial de precios/contactos y, fuera de este piloto, el limite Nginx de 20 MB. Los enlaces externos de pago antiguos no fueron cancelados y requieren atencion manual si se utilizan.
