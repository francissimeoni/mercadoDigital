# Paso 5: administración unificada del catálogo

Fecha: 24/11/2026. Implementación local en `C:\Sistemas\MercadoDigital-Reconstruido`. Es evolución funcional autorizada posterior a la recuperación; no representa una reconstrucción adicional de lógica del compilado productivo.

## Resultado y alcance

Se integraron ficha, moneda, precio, envío e imágenes en un único formulario y guardado. Cada tienda de revendedor conserva sus propias categorías de envío, editables por su propietario o el administrador autorizado. La categoría se referencia por ID: su importe se consulta al calcular el precio, sin copiarlo como costo fijo en cada artículo.

Se reutilizaron las tablas, el calculador, las categorías, los contactos y los mecanismos de revisión del paso 1, y el circuito público de los pasos 2 y 3. No se crearon otro modelo de precios ni categorías globales. Las operaciones y protecciones históricas del paso 4 se mantienen.

El modo público continúa apagado por defecto. No se modificaron los originales ni el rescate, no se activaron tiendas reales, no se ejecutaron migraciones ni se utilizaron servicios o credenciales productivos. Los conteos de productos y tiendas de las pruebas corresponden a datos sintéticos; no se inventarió producción.

## Recorrido del revendedor

1. Abrir **Preparar catálogo** (`/mi-tienda?tab=catalogo`). En una tienda con modo catálogo, **Productos** abre la misma administración unificada.
2. Configurar la cotización manual si utilizará USD, el WhatsApp del titular y las condiciones de entrega. Los cambios requieren revisar su impacto y luego confirmarlos.
3. Crear categorías de envío de su tienda con nombre e importe ARS. Un cero explícito es válido; un costo faltante no se convierte en cero.
4. Elegir **Nuevo producto de catálogo** o **Editar ficha y precio**. Cargar datos, rubro, moneda y precio original. Indicar explícitamente si incluye envío; si no, elegir una categoría propia.
5. Revisar el desglose privado y el único precio final en ARS calculado por el backend. Es posible ir a cotización o categorías dentro de la misma página conservando el borrador del producto.
6. Agregar imágenes por URL o seleccionar hasta cinco archivos nuevos PNG/JPEG/WEBP de hasta 5 MB cada uno, con un máximo de veinte imágenes totales. La primera imagen es la portada; las anteriores se conservan hasta confirmar el guardado. El video existente no se elimina al editar imágenes.
7. Elegir estado activo, promoción y novedad, y **Guardar producto y precio**. El alta comienza inactiva en la interfaz para que el operador decida su estado. No se solicita stock, precio de compatibilidad ni tipo de paquete anterior.

El listado permite buscar y filtrar precios configurados o pendientes. Su estado proviene del cálculo del backend, no de una fórmula alternativa del navegador. Un precio configurado no significa producto activo, tienda habilitada ni autorización para desplegar.

El guardado requiere una configuración calculable. Los borradores incompletos permanecen en memoria del formulario, con protección para salir o descartar; no se implementó un autoguardado de borradores en el servidor. Ante errores no se borra el formulario. Si el guardado terminó pero falla la actualización del listado, se informa ese resultado sin anunciar que el producto no se guardó.

Durante la preparación, los productos nuevos carecen de los campos del checkout anterior y no aparecen en ese circuito. En productos existentes, nombre, descripción, imágenes, rubro, destacados y estado son datos compartidos: sus cambios se aplican al guardar también en la tienda anterior. La pantalla lo advierte. Los dos modelos de precio no se sincronizan automáticamente.

## Categorías de envío e incrementos

Cada categoría pertenece a un `revendedor_id`, la clave de la tienda de revendedor en este modelo. Dos tiendas pueden usar el mismo nombre con distintos importes. El servidor valida propiedad al listar, crear, editar, eliminar y asignar la categoría a un producto. Las tarifas logísticas anteriores permanecen independientes.

La administración muestra importe y cantidad de productos vinculados que no incluyen envío. Para editar:

1. Cambiar nombre o costo y seleccionar **Revisar impacto en precios**.
2. Revisar costo anterior/propuesto y precios de los productos afectados. La tabla muestra páginas de 25 filas.
3. Seleccionar **Confirmar y guardar categoría**. Se envían versión e `impactoToken`.
4. Si cambió el escenario revisado, el servidor rechaza la confirmación y exige recargar/revisar. El precio original y la cotización no se sobrescriben.

Los nuevos precios se calculan al consultar el catálogo. Se invalidan las consultas administrativas, ficha, listados y detalles públicos del mismo cliente. Otros navegadores conservan el refresco periódico existente de hasta 60 segundos en una pestaña activa; el selector de contacto consulta de nuevo el precio y advierte cambios antes de continuar. No se promete actualización instantánea de una pestaña suspendida ni control de la caché externa de WhatsApp.

No cambian productos con envío incluido, otras categorías, otras tiendas ni importes de pedidos históricos. No se renegocian automáticamente compras acordadas por fuera de la plataforma. Una categoría vinculada no se puede eliminar: deben reasignarse primero los productos.

La regresión sintética utiliza USD 100 × ARS 1.200 + envío ARS 8.000 = ARS 128.000. Cambiar esa categoría a ARS 10.000 devuelve ARS 130.000 desde el servicio público real con repositorios simulados. El producto con envío incluido conserva ARS 120.000 y la otra tienda conserva ARS 128.000.

## Precio, precisión y contratos

Se mantiene `CatalogoPrecioCalculator`: conversión primero, suma del envío ARS después, precisión intermedia y redondeo final HALF_UP a dos decimales. No intervienen stock, comisión, pasarela, seguro ni precio tarjeta.

Los decimales del DTO administrativo ahora salen como cadenas JSON, incluidos cotización, categorías, originales e impactos. El consumidor local fue actualizado; cualquier cliente privado externo deberá aceptar ese contrato. Las entradas siguen aceptando decimales compatibles con Jackson. El formulario normaliza coma o punto decimal sin convertir dinero a `Number`; rechaza separadores de miles y exceso de precisión. Se probó el importe `99999999999999999.99` sin perder centavos. Los campos heredados conservan su contrato numérico y ahora declaran explícitamente la posibilidad de valores nulos cuando corresponda.

Nuevas rutas privadas, relativas a la base existente:

| Método y ruta | Contrato |
| --- | --- |
| `GET /revendedores/{id}/catalogo-config/fichas/{producto}` | Ficha, versión de ficha y configuración de precio |
| `POST .../fichas/vista-previa` | Revisión de alta, JSON |
| `POST .../fichas/{producto}/vista-previa` | Revisión de edición, JSON |
| `POST .../fichas` | Alta unificada, multipart |
| `PUT .../fichas/{producto}` | Edición unificada, multipart |

El multipart contiene `ficha` como JSON e `imagenes` como archivos opcionales. La ficha incluye `operacionId`, `versionFicha`, versión del precio, descriptores SHA-256 de archivos y `previewToken`. El token de revisión cubre ficha, imágenes declaradas y cálculo vigente; no sustituye autenticación. El contenido recibido se contrasta con los descriptores de archivo.

## Persistencia, reintentos y archivos

La nueva migración es **V57__ficha_catalogo_unificada.sql**. No se modificaron V55/V56 ni las migraciones anteriores. V57 permite `precio_venta` y `categoria_paquete` nulos, agrega versión optimista de ficha y una clave de alta única por tienda con hash del contenido original. Inicializar la versión técnica de filas existentes no infiere precios ni reglas comerciales.

`CatalogoFichaService` guarda ficha, relaciones de imágenes, configuración de precio y auditoría dentro de una transacción. Bloquea storefront y producto y conserva las versiones del borrador. Las ediciones de imágenes por las rutas anteriores también modifican la fila de producto para participar del control de versión.

Un reintento de alta con la misma clave y contenido recupera el producto guardado sin repetir escrituras ni subidas. Si se reutiliza la clave con otros datos, devuelve conflicto. No se afirma idempotencia distribuida comprobada: faltan MySQL y pruebas entre procesos. Un reintento de edición con versión anterior puede devolver conflicto si el primer guardado ya terminó; se debe recargar y revisar el resultado. La clave del alta se conserva mientras vive el formulario; no es un borrador persistente tras cerrar el navegador.

Los archivos nuevos se guardan dentro de la transacción con extensión determinada por el tipo y firma inicial PNG/JPEG/WEBP comprobados. La firma inicial no equivale a un análisis completo del contenido de la imagen. Un callback elimina exclusivamente los archivos nuevos si la transacción revierte; se probó su comportamiento en un directorio temporal. No se borran archivos anteriores que podrían compartirse por URL. Una terminación abrupta del proceso puede dejar archivos huérfanos: queda pendiente una política operativa de conciliación/limpieza.

El límite multipart se fija en 5 MB por archivo y 26 MB por solicitud. Un proxy futuro debe admitir límites compatibles. No se modificó ningún proxy ni servicio real.

El listado y detalle públicos anteriores y la creación del pedido anterior rechazan productos sin su configuración heredada. El editor, ajuste masivo e importador anteriores rechazan utilizar una ficha sin precio heredado para completar silenciosamente ese modelo. Los listados administrativos anteriores excluyen estas fichas y permiten gestionarlas desde Preparar catálogo. No hay relleno con ceros ni conversión implícita de USD a precio heredado.

## Contactos y contenido

Se conservan el WhatsApp compartido del titular, los contactos de asesores y el selector público existente. La preparación enlaza a **Contactos y difusión** y **Mi página**. El titular sigue destacado y los asesores de otra tienda no se incorporan al selector. Ser contacto no concede permisos de gestión.

Comprar abre WhatsApp con el mensaje preparado y la persona decide enviarlo. No crea pedido, pago, reserva ni envío. Los permisos, autenticación y acceso con Google se mantienen cubiertos por las regresiones existentes, sin modificar su implementación.

Mi página incorpora una revisión orientativa de textos personalizados de portada, pie y quiénes somos. Señala expresiones relacionadas con checkout, seguro, logística o condiciones de compra para revisión manual. No reescribe contenido. No detectar una expresión no certifica el contenido ni sus condiciones. No se realizó una auditoría de textos reales de tiendas porque no se consultó una base productiva.

## Verificación y evidencia

El directorio `artifacts/paso-5-catalogo/` conserva hashes previos de 707 archivos, estado Git inicial, registros de ejecuciones, pruebas dirigidas, capturas y resultados finales. `validar.mjs` permite repetir las comprobaciones locales de backend o frontend. El inventario propio identifica los archivos cambiados respecto de esa línea base; los cambios previos del usuario no se descartaron.

| Verificación final | Resultado | Evidencia y límite |
| --- | --- | --- |
| Backend: suite completa y JAR | **408 pruebas correctas**, 0 fallos/errores/omitidas; empaquetado correcto | `backend-package-final.log`, `ejecucion-backend.json`; sin MySQL |
| Frontend: TypeScript y Vite | Correctos | `frontend-build-final.log`, `frontend-vite-final.log`; conserva advertencia por tamaño del bundle |
| Catálogo DOM | **31 casos correctos** | `frontend-catalogo-final.log`; incluye 8 casos nuevos de formulario unificado, precisión y categorías |
| Transición DOM | **34 casos correctos** | `frontend-transicion-final.log`; expectativas adaptadas a la carga unificada |
| Precios/tracking recuperados | **11 regresiones correctas** | `frontend-regresiones-final.log`; circuito anterior conservado |
| Chrome: ficha e incremento | Correcto en 1280 y 390 px; sin desborde, campos sin etiqueta ni excepciones JS | `verificacion-navegador.json`, `ficha-desktop.png`, `ficha-mobile.png`, `ficha-mobile-precio.png`; API sintética |
| Chrome: transición completa | **17 recorridos correctos**, incluido panel, historial de navegación y acceso a contacto | `transicion-browser/verificacion-navegador.json`, `navegador-transicion.log`; tenant y sesiones sintéticos |
| Lint comparado con línea de base | **30 errores y 18 advertencias heredados; ningún diagnóstico agregado ni eliminado** por archivo/regla/severidad | `lint-baseline.json`, `lint-final.json`, `comparacion-lint.json`; no se declara limpio |
| MySQL/Flyway/JPA y concurrencia entre procesos | **No ejecutados** | Guion reproducible y matriz de aceptación en `artifacts/paso-5-catalogo/PLAN_PRUEBAS_MYSQL_PASO_6.md` |

La línea de base ejecutada en esta etapa tuvo 383 pruebas backend, 23 de catálogo, 34 de transición y 11 regresiones correctas. Los registros de esa ejecución se conservan; no se tomaron sus resultados del informe anterior.

Los tests de servicios simulan repositorios. MVC usa filtros JWT reales y servicios simulados. El navegador utiliza Chrome real con transporte sintético. Las pruebas de archivos ejercitan callbacks transaccionales, no un commit de MySQL. Esas diferencias limitan qué puede considerarse verificado.

Las primeras pasadas detectaron expectativas antiguas de interfaz y tipos que aún suponían precio/paquete siempre presentes. Se actualizaron los contratos y las pruebas sin quitar los controles de seguridad. El arnés nuevo de navegador tuvo un error de escape en su selector JavaScript; se corrigió y se añadió propagación explícita de excepciones de evaluación antes de repetir la prueba.

El cierre previo de 408 pruebas detectó un fixture histórico que omitía el precio heredado, ahora reservado como nulo para fichas nuevas de catálogo. Se completó el fixture para representar una fila histórica válida y se mantuvo la aserción de conservar stock 17; la última suite completa pasó. El log previo fallido se conserva en `backend-package-previo-fixture.log`.

## Pendientes concretos del paso 6

1. Disponer de MySQL desechable aislado. Ejecutar Flyway hasta V57 y validación JPA, inspeccionando nulos, FK, índice único y versiones. No se encontró MySQL/MariaDB, Docker ni Podman local durante esta etapa.
2. Conectar navegador, Java y esa base con dos tiendas y sesiones sintéticas. Verificar alta/edición, imágenes, categorías, cotización, publicación/contacto y roles. Chrome verificó también la navegación del panel con sesión sintética; falta una sesión autenticada realmente contra Java y su persistencia.
3. Probar dos sesiones y dos procesos: categoría contra precio, actualización contra edición de imágenes, claves de alta repetidas y rollback después de escribir archivos. Verificar una sola alta, ausencia de publicación parcial y rechazo de versiones desactualizadas.
4. Definir el tratamiento de archivos huérfanos tras un cierre abrupto. Comprobar límites multipart del futuro proxy en un entorno aislado.
5. Completar la revisión manual de datos y textos por tienda. No importar automáticamente moneda, envío incluido ni categoría desde el precio anterior. Una futura importación/edición masiva de catálogo requiere alcance propio; el ajuste anterior no se reutiliza como equivalente.
6. Conservar los pendientes del paso 4: callback y secreto de Mercado Pago en sandbox autorizado, preferencias de resultado ambiguo, conciliación de cobros tardíos/reembolsos y notificaciones sin outbox durable. El estado REEMBOLSADO de un RMA no prueba devolución financiera.
7. Preparar un plan de activación y reversión por tienda. Quitar una tienda de la lista de catálogo no completa precio, paquete o stock de fichas nuevas para el circuito anterior. La reversión no debe reabrir checkout indiscriminadamente.

La compilación y estas pruebas locales no acreditan equivalencia con producción ni habilitan un despliegue. La activación pública real permanece pendiente de esas validaciones y decisiones.
