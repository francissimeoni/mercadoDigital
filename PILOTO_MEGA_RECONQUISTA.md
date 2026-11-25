# Piloto de catálogo: Mega Reconquista

> Actualización operativa del 25/11/2026: piloto conservado únicamente para Mega Reconquista; release vigente `mejoras-20261125-r1`, backend con V58. Ver [mejoras, validación y reversión vigente](INFORME_MEJORAS_ASESORES_PRODUCTOS_DIFUSION_METRICAS.md). El JAR anterior sin el recurso V58 no es una reversión compatible con el historial actual; usar el paquete y script preparados en ese informe.

Publicado el 24/11/2026. Estado: **piloto activo y verificado en https://www.megareconquista.com**.

## Resultado efectivo del despliegue

- Release: `piloto-catalogo-20261125-r4` (fecha UTC; 24/11 en Argentina). Backend y frontend verificados por SHA-256. Se aplicaron únicamente V55–V57; se conservaron los checksums e historial V1–V54.
- Se prepararon 176 productos de Mega Reconquista: ARS, precio anterior exacto y envío incluido. Permanecen 144 activos y 32 inactivos. No se recalculó el dólar ni se actualizaron importes.
- Solo Mega tiene la bandera de catálogo. Cleostore, Infonel y Navarro fueron comprobadas con catálogo nuevo desactivado. Mercado Pago se retiró globalmente de MercadoDigital por instrucción posterior del propietario.
- El teléfono comercial estaba en formato local y no aparecía el responsable en el selector. Se guardó el mismo destino internacional que ya construía `normalizeWhatsAppPhone` en el botón anterior, sin inventar otro contacto; auditoría `CATALOGO_NORMALIZACION_CONTACTO`, operación `ba8fda23-9938-4727-8ead-57b5e99d841f`.
- Chrome real sobre HTTPS: ficha, selector móvil, responsable destacado, foco del diálogo y mensaje correcto del producto. Se interceptó la navegación a WhatsApp en la prueba; no se enviaron mensajes. Sin excepciones de JavaScript en el recorrido final.
- La API privada rechaza visitantes sin sesión. El inicio de Google devuelve 302 hacia `accounts.google.com`; el ingreso completo con una cuenta real queda por comprobar con el propietario.
- Vidra conserva PID 1374, servicio activo, entorno y 23 archivos de configuración/binarios idénticos; HTTPS 200. No se reinició ni modificó Vidra, Nginx, Docker ni MySQL. No se revocaron credenciales externas de Mercado Pago.

Evidencia vigente en `artifacts/piloto-mega/`: `despliegue-resultados.json`, `verificacion-publica.json`, `retiro-http-resultados.json`, `pruebas-conversion.json`, `esquema-resultados.json` y `ejecucion-retiro-mp.json`. Los paquetes preparados anteriormente conservan su fecha y no describen el estado actual.

Respaldo inmediatamente anterior: `/opt/mercado-digital/backups/piloto-catalogo-20261125-r4`, con SQL, archivos, JAR y entorno anterior. Copiado a carpeta local privada excluida de Git. Se restauraron sus 46 tablas en una base local separada, comprobando V54, los 176 productos y el JAR anterior; ninguna aplicación se inició con esas credenciales. Ver `respaldo-final-resultados.json`.

Incidencias resueltas: JSON operativo con BOM de Windows rechazado antes de detener servicios; teléfono local normalizado con auditoría; arnés del navegador adaptado para interceptar navegación de página en lugar de `window.open`. Esta última corrección no modificó la aplicación.

## Cómo usar el piloto

1. Ingresar a **Mi tienda → Configurar catálogo** con la cuenta del revendedor.
2. En **Pesos por 1 USD**, fijar la cotización propia, revisar los cambios y confirmar. No afecta a artículos cargados en ARS.
3. En **Categorías de envío de tu tienda**, crear categorías con nombre y costo en pesos. Al editar un costo, revisar los productos afectados y confirmar. Son exclusivas de esa tienda.
4. En cada producto, elegir ARS o USD y cargar el precio. Mantener **incluye envío** cuando corresponda; de lo contrario seleccionar una categoría propia. El público ve siempre el total en ARS.
5. Comprar abre el selector; elegir al responsable o a un asesor activo abre WhatsApp con el mensaje preparado. El comprador decide enviarlo. Ese contacto no crea pedidos, pagos ni reservas de stock.

Límites: el proxy conserva 20 MB por solicitud; no se probó un cobro real ni se cancelaron posibles preferencias/suscripciones externas antiguas. Los pagos antiguos que ocurran por fuera no se concilian automáticamente mediante MercadoDigital. El historial permanece. Ver `RETIRO_MERCADO_PAGO.md`.

## Identidad y regla confirmadas

- Dominio: `www.megareconquista.com`.
- Revendedor: `ea37fc94-8f19-4b8b-862d-dc23f71b144d`.
- Storefront: `c4f8b4af-524c-4472-ab8f-4dd75744ca25`.
- Inventario consultado en el VPS: 176 productos, 144 activos y 32 inactivos. Todos en ARS y con precio positivo; ningún activo con stock cero en ese inventario.
- El propietario confirmó que todos los precios anteriores contemplaban envío. Para este piloto se copia `precio_venta` a precio original ARS, con envío incluido y sin categoría adicional. No se agrega seguro ni comisión. Se conserva el estado activo/inactivo.
- La cotización del dólar queda sin inventar: estos productos continúan en pesos hasta que el revendedor cambie explícitamente moneda y precio.

Esta decisión posterior reemplaza la exigencia anterior de preparar individualmente los productos antiguos para determinar si incluían envío. Se aplica solamente a configuraciones faltantes de la tienda elegida.

## Implementación operativa

`backend/scripts/PrepararCatalogoInicial.java` es una herramienta administrativa, no una migración automática al arrancar ni un botón nuevo en el panel.

1. `preview` consulta una sola tienda, exige coincidencia de UUID y dominio y devuelve la revisión JSON y su token SHA-256.
2. Clasifica los productos en PREPARAR, CONSERVAR_CONFIGURADO, REVISAR_PRECIO o REVISAR_MONEDA. No transforma nulos, cero ni monedas distintas de ARS.
3. `apply` exige el token de la revisión del entorno de destino. Relee los datos con bloqueo de tienda y productos, dentro de una transacción READ_COMMITTED. Si cambiaron, rechaza la operación completa.
4. Crea únicamente configuraciones faltantes y registra IDs/importes/cuenta SQL en `auditoria_eventos`. No modifica fichas, imágenes, precios anteriores, stock, estados, pedidos, pagos, contactos, cotización ni otras tiendas.
5. Un fallo revierte configuraciones y auditoría. Repetir con un token viejo produce rechazo; una revisión nueva reconoce lo preparado y no duplica ni sobrescribe.

La herramienta no activa el modo público. La activación sigue dependiendo de `CATALOGO_PUBLICO_REVENDEDORES`. El paquete compartido de backend/frontend y las migraciones afectan al entorno general; la bandera limita el nuevo flujo a Mega Reconquista. No equivale a desplegar una aplicación físicamente aislada para esa tienda.

## Validación efectuada

`artifacts/piloto-mega/pruebas-conversion.json`: **12 escenarios correctos** con MySQL local real, incluidos:

- Conversión exacta de los 176 precios y preservación de 144 activos/32 inactivos.
- Vista previa sin escritura; dominio/UUID incorrectos rechazados; precio modificado después de revisar rechazado.
- Fallo deliberado de auditoría sin conversión parcial.
- Repetición segura y preservación de una configuración USD personalizada.
- Precio máximo decimal exacto, stock cero permitido, nulo/cero/moneda distinta pendientes.
- Tienda ajena intacta, fichas y pedidos históricos sin cambios.
- Dos procesos Java bloqueados realmente por MySQL: uno confirma y el segundo rechaza la revisión obsoleta; una sola configuración nueva.
- API del backend reconstruido: devuelve los 144 productos activos con importe ARS exacto, excluye inactivos y ofrece el contacto del titular. Se usó un teléfono sintético; no se enviaron mensajes.

Se copiaron únicamente datos de artículos necesarios para esta prueba a la base desechable; no cuentas ni clientes reales. Las fichas sintéticas se retiraron al terminar y se comprobaron los datos anteriores. La comprobación HTTP es local; no valida nuevamente toda la interfaz, fotografías, Google ni WhatsApp externos. La interfaz y sus recorridos generales tienen la evidencia previa del paso 6.

Los primeros intentos del arnés corrigieron dos incompatibilidades del entorno de lectura (charset del cliente MySQL y Python 3.10 sin `hashlib.file_digest`) y una expectativa incorrecta del nombre `responsable` en la respuesta de contacto. No se cambió el contrato de aplicación para acomodar las pruebas.

## Historial real del VPS y ensayo de esquema

- Servicio real: `mercado-digital.service`, activo. JAR SHA-256 `f597e376489d1733472f45bc14fe2a08bcc8cd438e2250499be5f1ff35ca27e5`, igual al rescate.
- MySQL productivo: 8.0.45, contenedor `md-mysql`. Pruebas locales: 8.0.44.
- La base tiene registradas V1–V54, todas exitosas. Los 42 archivos disponibles coinciden con los del JAR productivo por SHA-256 y con el historial por checksum.
- Faltan los fuentes V34–V45. Se inspeccionaron 41 JAR en backend/backups y no aparecieron. Tampoco se encontraron en el historial Git accesible ni como SQL en las ubicaciones revisadas. La ruta histórica de Lenovo no está disponible en esta computadora.
- El perfil prod del JAR activo ya contiene `ignore-migration-patterns: "*:missing"`. Este comportamiento existente no demuestra por sí solo compatibilidad.
- Se exportó **solo estructura, sin filas de negocio ni triggers**, y se restauró en `mercadodigital_piloto_esquema`, local. Se reprodujeron versión/script/checksum/success del historial; los demás metadatos del ensayo son sintéticos.
- El ensayo comprueba exactamente las 12 versiones faltantes, los 42 checksums y el estado V54 antes de reproducir esa política de Flyway, exclusivamente en el clon. Aplicó solo V55–V57. Luego el backend arrancó con **JPA validate correcto** y Flyway sin cambios pendientes.
- No se ejecutó repair, baseline ni alteración de checksums. No se modificó el historial productivo. El ensayo no recupera los originales perdidos y no equivale a probar una copia completa de los datos reales.

Evidencia: `inventario-vps.json`, `comparacion-migraciones.json`, `esquema-resultados.json`, `esquema-migraciones.log`, `esquema-jpa.log`.

## Paso restante para publicar

Se obtuvo en lectura un respaldo privado de base, directorio backend (incluido almacenamiento/configuración), frontend, unidad systemd y configuraciones Nginx relevantes. Las dos copias SQL tomadas antes/después del archivo tar fueron idénticas por SHA-256. Se restauraron las **46 tablas** en una base local separada, sin arrancar aplicaciones contra ella y con el programador de eventos local apagado. Se extrajo el tar, se verificó el JAR y la presencia de configuración/frontend. Los datos privados permanecen en `artifacts/paso-6-catalogo/runtime/`, excluido de Git. Evidencia sanitizada: `respaldo-resultados.json`. Es una copia observada en ese momento: antes de desplegar hay que comprobar si hubo nuevas escrituras y renovar el respaldo cuando corresponda.

Nginx pasa `nginx -t`. Mega Reconquista apunta al frontend compartido y al backend 8081; el proxy permite 20 MB por solicitud (el backend admite 26 MB totales y 5 MB por imagen). No se modificó ese límite. Si se requiere subir cinco imágenes cercanas al máximo en una solicitud, debe alinearse el límite del proxy antes de ofrecer ese flujo.

El propietario autorizo expresamente desplegar, migrar y reiniciar MercadoDigital. Posteriormente retiro Mercado Pago de todo MercadoDigital, incluidas membresias, y aclaro que Vidra debe seguir utilizandolo. El secreto de webhook ya no es un requisito de este despliegue: las rutas retiradas se bloquean antes de la verificacion de firma. Ver `RETIRO_MERCADO_PAGO.md` para alcance y validaciones. Las credenciales y la aplicacion externa de Vidra no se modifican.

Siguen pendientes la prueba de autenticación con los retornos reales y la revisión de contactos/textos de Mega Reconquista. Si siguen existiendo operaciones históricas, mantener su atención y validar las integraciones que utilizan.

La excepción histórica de Flyway debe acompañarse del control exacto de versiones/checksums/esquema ensayado. Un historial nuevo o distinto exige detenerse; no habilitar omisiones generales para resolver discrepancias nuevas.

Secuencia concreta del piloto:

1. Respaldo y verificaciones previas según `PLAN_ACTIVACION_CATALOGO.md`.
2. Despliegue conjunto de backend/frontend con el catálogo público todavía deshabilitado y aplicación de V55–V57 bajo el procedimiento validado.
3. Nueva vista previa **en destino**; contrastar contra los 176 productos inventariados. Si hubo altas o cambios desde esta revisión, revisarlos antes de confirmar. El token de la prueba local no sirve para producción.
4. Ejecutar la preparación únicamente para el UUID y dominio indicados; verificar importes y 144 publicaciones esperadas si el inventario no cambió.
5. Configurar la bandera únicamente con `ea37fc94-8f19-4b8b-862d-dc23f71b144d`, comprobar cómo la recibe el servicio y probar el dominio público y el panel del propietario.
6. Revisar una tienda ajena, historial operativo, imágenes, contactos, cambios de dólar y de envío. No ampliar a otras tiendas automáticamente.

No se desplegó, reinició, migró ni modificó ningún dato productivo durante este trabajo. No se cambió la bandera de producción. La restricción anterior de no desplegar sigue respetada mientras se prepara esta activación. Se solicitó confirmar ese cambio de alcance antes del despliegue; además, el pendiente del webhook impide sustituir sin más el backend compartido.

Paquete preparado: `artifacts/paso-6-catalogo/runtime/piloto-release-20261124-r2/`. Incluye JAR, frontend, migraciones, conversor compilado/dependencias e informes; no incluye credenciales ni copias de la base. `artifacts/piloto-mega/paquete-manifest.json` registra sus SHA-256. Para el conversor compilado en Linux: `java -cp 'conversion/classes:conversion/lib/*' PrepararCatalogoInicial conexion.properties UUID dominio preview` (para aplicar, reemplazar `preview` por `apply TOKEN`).

## Uso y repetición

Con Java 17 y los JAR de Jackson/MySQL presentes en `artifacts/paso-6-catalogo/runtime/lib`:

```powershell
java --class-path 'artifacts/paso-6-catalogo/runtime/lib/*' backend/scripts/PrepararCatalogoInicial.java RUTA_PRIVADA_CONEXION.properties ea37fc94-8f19-4b8b-862d-dc23f71b144d www.megareconquista.com preview
# Revisar el JSON; aplicar solamente con el token de esa revisión en ese entorno:
java --class-path 'artifacts/paso-6-catalogo/runtime/lib/*' backend/scripts/PrepararCatalogoInicial.java RUTA_PRIVADA_CONEXION.properties ea37fc94-8f19-4b8b-862d-dc23f71b144d www.megareconquista.com apply TOKEN_REVISADO
```

El archivo privado usa las propiedades `url`, `user`, `password`; no guardarlo en Git, el paquete público ni informes. El acceso SQL es administrativo; no se entrega al revendedor. No usar credenciales productivas en las pruebas locales.

Prueba de conversión repetible: iniciar únicamente el MySQL desechable del paso 6 y ejecutar `node artifacts/piloto-mega/probar-conversion.mjs`. Requiere los fixtures sintéticos del paso 6; rechaza sobrescribir una tienda piloto que ya exista y limpia solo las fichas que creó. La API local y los procesos de concurrencia se cierran al terminar. `ensayar-esquema.mjs` exige que su base dedicada no exista: no la sobrescribe para repetir. `validar-jpa.mjs` puede repetir el arranque sobre ese clon ya migrado.

Las evidencias del paso 6 conservan sus fechas y cantidades: no se presentan como pruebas reejecutadas en este paso. No cambiaron los fuentes del backend de la aplicación, el frontend ni las migraciones; se agregó una herramienta operativa independiente y sus verificaciones.


## Cambio de alcance: retiro de Mercado Pago

Se modificaron los fuentes de backend/frontend para retirar globalmente Mercado Pago solo de MercadoDigital. El comentario anterior sobre ausencia de cambios de aplicacion describe la preparacion inicial, antes de esta decision. Se ejecutaron 432 pruebas backend y 78 frontend (31 catalogo, 36 transicion/retiro, 11 regresiones), todas correctas; TypeScript y builds correctos. Ocho solicitudes HTTP reales locales fueron bloqueadas sin escrituras y sin llamadas al proveedor simulado, y el storefront informo Mercado Pago deshabilitado. Se repiten la conversion piloto y la validacion JPA con el nuevo JAR antes de empaquetar.

Vidra: servicio 8082 activo, HTTPS 200 y referencia de 23 archivos/configuraciones guardada para comparar despues. Solo se reiniciara el servicio de MercadoDigital 8081. No se modificara Nginx: permanece el limite existente de 20 MB.
