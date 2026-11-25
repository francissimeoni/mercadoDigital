> Revisión posterior publicada: ver [INFORME_COMPOSICION_STOREFRONT.md](INFORME_COMPOSICION_STOREFRONT.md). La release y reversión de este informe corresponden a la entrega anterior.

# Mejora del storefront público

Fecha: **25/11/2026**. Estado: **implementado, publicado y verificado**, con las limitaciones indicadas abajo.

## Publicación

- Sitio: https://www.megareconquista.com
- Release actual: `/var/www/mercado-digital-releases/storefront-20261125-r1`.
- Enlace publicado: `/var/www/mercado-digital-web`.
- Release anterior: `/var/www/mercado-digital-releases/mejoras-20261125-r1`.
- Trabajo remoto: `/opt/mercado-digital/work/storefront-20261125`.
- Proyecto sincronizado: `C:\Sistemas\MercadoDigital-Reconstruido`.
- JAR actual SHA-256: `71c4e82e56114f6af8b6b156d662bf69fc7b531f8aaa14529d662e92f58ff8aa`.
- PID de MercadoDigital: 52132 → 62627. Se reinició únicamente `mercado-digital.service` por la corrección del mapper de categorías.
- **Sin migraciones nuevas.** El historial de Flyway permanece idéntico, incluida V58.

Se comprobó igualdad SHA-256 de los 753 fuentes registrados al inicio entre el proyecto reconstruido y el trabajo remoto anterior; también se verificaron JAR y release activa antes de preparar la nueva carpeta. Se conservaron el estado Git y los cambios sin confirmar. El proyecto antiguo y el rescate no se modificaron.

La entrega verifica **760 archivos de código, recursos, pruebas y configuración**, con **27 archivos nuevos o modificados** dentro de ese alcance, y **11 archivos del frontend publicado**. `artifacts/storefront-20261125/cambios.json` y `fuentes-actuales.json` registran el detalle. La primera copia de trabajo incluyó también documentación y archivos auxiliares locales; no se utilizaron como configuración productiva. El inventario final se limita a fuentes y archivos de construcción/prueba.

## Conservación de datos y otras aplicaciones

Los 144 productos públicos conservan identificadores, precios, imágenes y demás valores. La única diferencia prevista en las respuestas del listado es la categoría comercial de cuatro artículos: de CHICO/MEDIANO a Otros. No se escribieron precios, productos ni configuraciones comerciales para probar.

Se compararon checksums de asesores, configuración de catálogo, categorías de envío, precios configurados, productos propios, storefronts, pedidos y pagos. Permanecieron idénticos al respaldo. También se comparó el historial de migraciones y las respuestas de resolución de Mega Reconquista, Cleostore, Infonel y Navarro. No se activaron otras tiendas ni se alteraron sus modos operativos.

Vidra conserva servicio/PID, entorno y los hashes de sus 23 archivos registrados; la comprobación HTTPS continúa correcta. Su integración de Mercado Pago no se modificó. No se cambiaron Nginx, Docker, MySQL productivo, configuraciones globales ni servicios compartidos.

## Catálogo y lenguaje visual

Se aplicó una base compartida en `frontend/src/storefront/storefront.css`: Inter autoalojada existente y su licencia, pesos moderados, fondos claros, bordes suaves, radios coherentes, contraste y foco visible. Su alcance es `.sf-storefront`; la corrección de imágenes de Difusión tiene clases específicas del dashboard. No se impuso el rojo de Mega Reconquista a las demás tiendas.

Los mockups aprobados se conservan como `referencia-desktop.png` y `referencia-mobile.png`. Son referencias de composición. Las capturas finales muestran datos y fotografías reales, conservando el banner, título y subtítulo configurados por el revendedor; no se sustituyeron por contenidos ilustrativos ni se añadieron afirmaciones comerciales.

- Encabezado compacto: logo legible, búsqueda expuesta, navegación simple y acceso secundario.
- Una sola opción Catálogo. `/` y `/catalogo` conservan sus rutas compatibles.
- En mobile, marca/menú y búsqueda en dos filas. Menú con estado accesible y cierre mediante Escape.
- Banner contenido y sin recortar su texto incrustado; composición de dos columnas en desktop y banner completo en mobile. Las portadas múltiples cambian manualmente y se amplían en diálogo accesible.
- Categorías horizontales compactas, filtros separados y orden visible.
- Grilla de dos columnas a 390 px, una a 320 px, tres donde corresponde y cuatro en desktop amplio.
- Imágenes con espacio reservado, proporción conservada y nombres/precios/acciones alineados. Los enlaces a fichas son nativos y permiten abrir otra pestaña.
- Tandas de 24 productos mediante Ver más; el listado no monta inicialmente los 144 artículos.
- Tarjetas de catálogo con un solo precio y contacto. Se mantiene el recorrido anterior en tiendas fuera del modo catálogo.
- Se retiró el botón flotante general en modo catálogo para evitar cubrir tarjetas y competir con el contacto de producto. El contacto general sigue disponible en Quiénes somos y en el pie.

En el caso medido a 1366×768, la primera tarjeta pasó de aproximadamente 1132 a **478 px** desde arriba. A 390 px pasó de aproximadamente 757 a **495 px**. Son medidas de esta tienda y contenido; no garantías para cualquier banner o configuración.

## Búsqueda y regreso

El buscador conserva el texto tal como se escribe; se normaliza al ejecutar la consulta. Buscar se ejecuta mediante Enter o el botón de lupa, también desde ficha y Quiénes somos. La limpieza del campo modifica el borrador; Buscar aplica la consulta vacía. Los chips permiten quitar filtros aplicados.

Se corrigió la pérdida del espacio en Samsung A56. La búsqueda tolera tildes, mayúsculas y palabras separadas: Samsung A56 encuentra los dos artículos reales que contienen ambas palabras, incluido Samsung Galaxy A56. Escribir y pegar producen el mismo resultado.

La URL contiene consulta, categoría, orden y cantidad mostrada. Volver desde una ficha restaura filtros, tanda, posición y foco del artículo. Atrás nativo también conserva la posición. Se desactivó el anclaje automático del contenido en el área principal porque desplazaba el retorno al cargar imágenes.

Los parámetros lista, asesor, asesorId y storefrontHost se conservan en los recorridos correspondientes. Una búsqueda y su limpieza permanecen dentro de una lista compartida; no la amplían silenciosamente al catálogo completo. Se valida que el retorno interno tenga una ruta de catálogo y se descartan destinos externos.

## Categorías comerciales

`PublicCatalogoService` usaba la categoría de paquete cuando faltaba categoría de producto. Ambos mappers de productos propios ahora responden Otros. Es una corrección de presentación del contrato, sin cambios de esquema, importes, asignaciones comerciales o categorías de envío. No se deduce un rubro automáticamente.

Pendientes de completar desde Productos:

| Artículo | ID | Etiqueta anterior |
| --- | --- | --- |
| Smart TV Onixx 43 | f008eb77-290f-4f41-a6d1-4814d51a7b33 | MEDIANO |
| Heladera Midas 249 litros | e2b969fb-70d9-4259-a5a2-7b481d3ea704 | MEDIANO |
| iPhone 15 128 GB negro | 58c6f8ef-bcd6-490b-9120-75db45ef01ac | MEDIANO |
| Motosierra eléctrica Oryx EL575 | 9427cc0a-2c42-4fcc-bdc2-9e8bb3225b9c | CHICO |

## Ficha y galería

Galería y resumen comercial en columnas desktop; presentación secuencial mobile. Nombre, precio y entrega aparecen antes de la descripción extensa. En mobile se mantiene una sola acción fija de producto con precio y espacio inferior reservado. Se oculta al enfocar el buscador para no competir con el teclado; el selector de contactos usa la capa modal del navegador.

Se conservan los saltos de línea mediante texto plano. Se eliminó Producto destacado, que aparecía sin respaldo. La imagen principal se carga con prioridad; las secundarias son diferidas. Los relacionados tienen navegación manual y respetan movimiento reducido. La ampliación usa un diálogo nativo con foco inicial, Tab contenido, Escape, fondo inerte y retorno al disparador.

No se inventaron especificaciones técnicas. El contrato disponible mantiene descripción libre; una ficha de atributos estructurados requiere un trabajo posterior de modelo/carga. Tampoco se sustituyeron fotografías de poca resolución por imágenes generadas.

El precio continúa usando las utilidades monetarias existentes y los valores del backend. No se tocaron conversión USD, dólar propio, categorías de envío, stock ni revalidación del contacto.

## Quiénes somos y pie

Presentación breve, contenido configurable, dirección y canales accionables. Si existe galería institucional se conserva; si falta, se utiliza composición tipográfica y no se recicla una publicidad como fotografía institucional. Se retiraron las promesas genéricas numeradas y el gran bloque oscuro de contacto.

En modo catálogo se explica el flujo elegir producto → elegir contacto → conversar por WhatsApp. No se añadieron historias, integrantes, horarios ni testimonios. Mega Reconquista sigue sin texto institucional propio ni galería: puede completarlos desde Mi página. Los asesores continúan apareciendo con sus datos públicos en el selector de producto; esta pasada no creó un endpoint institucional de equipo ni expuso usuarios internos.

El pie es compacto y sus canales son enlaces. En Quiénes somos omite repetir el listado completo de datos que ya presenta la sección de contacto.

## Difusión

Al seleccionar un producto se muestra su fotografía en tamaño útil, como imagen HTML con menú contextual nativo, título y Abrir imagen original. La selección múltiple ofrece un desplegable con cada imagen identificada. Hay alternativa ante ausencia/fallo.

Uso: seleccionar artículo → botón derecho sobre su foto → copiar imagen, o abrir la original. En mobile se utilizan las acciones nativas que ofrezca el navegador. El flujo de compartir continúa enviando texto/enlace a la composición de WhatsApp; no adjunta ni envía automáticamente fotografías o mensajes. Se conserva la revalidación de precios y contactos.

## Rendimiento y arquitectura

Se reutilizaron componentes y contratos existentes y se compartieron búsqueda, retorno y diálogo de imagen. Se difirió la carga de los paneles de propietario. El JavaScript inicial pasó de 1031819 a **831105 bytes** (aproximadamente 19,5 % menos). Sigue existiendo una advertencia de Vite por superar 500 kB; otros módulos operativos continúan en el paquete inicial. No se actualizó ninguna dependencia ni se alteró la configuración de compresión/caché de Nginx.

La regresión detectó que diferir el dashboard retrasaba el registro del guard de borradores respecto de BrowserRouter. Se conservó su importación inicial para instalar la protección de POP antes del router; las pantallas siguen cargándose bajo demanda. Se verificó cancelar/aceptar Atrás conservando el borrador cuando corresponde.

## Validaciones

| Comprobación | Resultado |
| --- | --- |
| Maven package | 445 pruebas, cero fallos/errores; JAR generado. |
| Java/MySQL aislado y reversión | 6 grupos: arranque/JPA/Flyway V58, categoría/precio, contacto, permisos, historial y JAR anterior. |
| Frontend TypeScript/Vite | Correcto, construido en la nueva carpeta del VPS. |
| ESLint de módulos TS/TSX modificados | Sin errores ni advertencias. |
| Catálogo / transición / teléfonos / navegación | 31 + 44 + 13 + 4 = 92 pruebas correctas. |
| Storefront Chromium con build real y transporte reproducido | 22 vistas/estados, 9 recorridos, cero errores JavaScript. |
| axe-core 4.10.3 | Cero infracciones detectadas en los casos ejecutados; conserva también los resultados incompletos para revisión manual. |
| Dashboard sintético | 35 vistas/tamaños y 18 grupos de casos, sin errores JavaScript. |
| Transición anterior de navegador | 17 casos correctos; sin pagos, pedidos nuevos o mensajes. |
| HTTPS publicado | 8 capturas desktop/mobile, búsqueda, retorno, selector, WhatsApp interceptado, protección del panel y otras tiendas. |
| Archivos publicados | 11 archivos cotejados por SHA-256; 760 fuentes cotejados entre ambos entornos. |

Se probaron 320, 390, 768, 1366 y 1920 px; carga vacía/error, branding alternativo, modo anterior, importe extremo, imágenes fallidas y aumento del tamaño raíz de texto. La prueba de tamaño raíz no equivale a ampliar cada texto fijo ni a operar el zoom nativo. Se revisaron capturas y se corrigieron el menú desktop, la superposición del botón flotante y la geometría de imágenes de tarjetas.

Las pruebas de integración utilizaron MySQL portable en 127.0.0.1:13316, la base de fixtures `mercadodigital_mejoras`, aplicaciones en 18090/18091 y configuración sintética con servicios externos deshabilitados/loopback. Se verificó que los usuarios pertenecieran a test.invalid. Las aplicaciones y el MySQL aislado se cerraron al terminar.

En producción se usaron lecturas anónimas. El navegador interceptó POST de métricas y destinos WhatsApp. Se verificó Ayelén con destino 5493482670015 sin enviar mensajes ni crear pedidos o cobros. No se falsificó una sesión de propietario.

## Respaldo y reversión

Respaldo privado: `/opt/mercado-digital/backups/storefront-20261125`. Incluye frontend, JAR, entorno separado, almacenamiento, fuentes iniciales y dump de MySQL. Se verificaron hashes, lectura completa de los tar y estructura/cierre del dump; no se restauró el dump productivo para las pruebas.

El JAR anterior de esta intervención ya incluye V58 y se inició satisfactoriamente contra ese esquema sintético. SHA-256: `8b19caccfbabe5336989d229dbc261fca48675e7e3dace7e56159fcff9bc83f3`.

Reversión preparada, **no ejecutada en producción**:

```sh
python3 /opt/mercado-digital/work/storefront-20261125/revertir.py
```

Verifica release, JAR y entorno antes de volver al código anterior; reinicia solo MercadoDigital y conserva base, archivos nuevos e historial V58. Se detiene si hay una publicación posterior. El despliegue dispone también de reversión automática ante un fallo de sus verificaciones.

## Evidencia y límites

Directorio local: `artifacts/storefront-20261125/`. Remoto: `/opt/mercado-digital/work/storefront-20261125/`.

Resultados vigentes: `publicacion-resultados.json`, `publico-resultados.json`, `navegador-resultados.json`, `integracion-resultados.json`, `java-tests.json`, manifiestos de fuentes/release y logs de validación. `evidencia/` usa el build real con respuestas reproducidas/estados sintéticos; `evidencia-publica/` corresponde a HTTPS real. `dashboard-evidencia/` y `transicion-browser/` conservan sus respectivos arneses aislados. Los archivos fallo y navegador-fallo corresponden a iteraciones de diagnóstico anteriores; no son el estado final.

Limitaciones concretas:

1. No se certificaron lector de pantalla, Safari/iOS, teclado físico móvil, zoom nativo ni copia de imagen desde cada navegador/dispositivo real. El escáner no demuestra conformidad integral WCAG.
2. Falta una revisión autenticada del propietario real en producción. Las escrituras del dashboard se probaron con sesiones y API sintéticas.
3. WhatsApp y su caché de vista previa dependen del cliente externo. No se verificó una cuenta real ni se enviaron mensajes.
4. Quiénes somos necesita texto y fotografías propios para una identidad más personal. No se fabricó contenido para llenar espacios.
5. Los cuatro productos indicados necesitan categoría comercial. Las fotos de baja resolución, los títulos extensos, símbolos y mayúsculas siguen siendo datos del catálogo a editar por el revendedor.
6. La API del listado se descarga completa y filtra localmente; el DOM se muestra por tandas. Una escala mucho mayor requeriría paginación/búsqueda de servidor. No se implementó virtualización general ni se eliminó todo código operativo del bundle inicial.
7. No se ensayó la restauración completa del dump productivo. Sí se probó arranque y compatibilidad del JAR de reversión contra el esquema V58 aislado.
