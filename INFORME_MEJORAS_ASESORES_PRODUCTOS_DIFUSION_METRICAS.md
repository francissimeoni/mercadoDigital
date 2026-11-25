> Actualización posterior: la release vigente es `storefront-20261125-r1`. Consultar [INFORME_MEJORA_STOREFRONT_PUBLICO.md](INFORME_MEJORA_STOREFRONT_PUBLICO.md); los procedimientos de reversión anteriores pertenecen a sus respectivas releases.

# Asesores, Productos, Difusión y Métricas

Fecha: **25/11/2026**. Estado: **implementado, publicado y comprobado**, con los límites indicados al final.

## Publicación

- Web: https://www.megareconquista.com
- Release: `/var/www/mercado-digital-releases/mejoras-20261125-r1`.
- Enlace activo: `/var/www/mercado-digital-web`.
- Release anterior: `/var/www/mercado-digital-releases/productos-20261125-r1`.
- Trabajo remoto: `/opt/mercado-digital/work/mejoras-20261125`.
- Proyecto local: `C:\Sistemas\MercadoDigital-Reconstruido`.
- Migración aplicada: **V58__asesores_zona_atencion.sql**.
- Backend publicado, SHA-256: `8b19caccfbabe5336989d229dbc261fca48675e7e3dace7e56159fcff9bc83f3`.

Se reinició exclusivamente `mercado-digital.service`: PID anterior 25165, actual 52132. Vidra conserva su PID, estado, entorno y hashes de los 23 archivos registrados, con comprobación HTTPS satisfactoria. No se modificaron sus integraciones de Mercado Pago, bases, credenciales, Nginx ni servicios compartidos.

Los **144 productos públicos conservan sus respuestas y precios**. Se comprobaron los datos comerciales mediante respuestas HTTP, checksums de tablas y comparación de los contactos anteriores. Foto, nombre, teléfono, actividad y pertenencia de los asesores existentes se conservaron; los campos nuevos quedaron NULL. Ayelén mantiene `+5493482670015`. No se cargaron zonas ni fotografías ficticias en producción.

Mega Reconquista continúa como piloto. La resolución pública de Cleostore, Infonel y Navarro conserva el modo anterior; Mercado Pago permanece deshabilitado en MercadoDigital. No se generaron pedidos, cobros ni mensajes de WhatsApp durante las verificaciones.

## Conservación y reproducción

Se preservaron los originales y el rescate. Se registró el estado Git inicial y se archivaron los fuentes con sus cambios sin confirmar antes de editar. El backend inicial coincidía con el manifiesto del piloto; el frontend coincidía con los fuentes publicados, excluyendo las diferencias esperadas de archivos de entorno. No se copiaron configuraciones locales sobre las productivas.

`artifacts/mejoras-20261125/cambios.json` identifica **38 archivos nuevos o modificados** respecto del inventario inicial. Se verificó igualdad SHA-256 de **753 archivos de código, recursos, pruebas y configuración** entre el reconstruido y el trabajo remoto. Los **8 archivos del dist local coinciden con la release publicada**. El dist local previo quedó en `artifacts/mejoras-20261125/dist-local-anterior`.

Paquete reproducible: `artifacts/mejoras-20261125/fuentes-finales.tgz`, SHA-256 `2b6b6f4123f2ed6adb19198d57cb078234cd10380cb6ca3afc6d9ef250143ac2`. Incluye configuraciones de compilación y wrappers; los archivos de entorno se mantienen separados.

## Cambios funcionales y visuales

### Lenguaje compartido

Se conserva Inter autoalojada. Se centralizan campos, acciones, encabezados, paneles, espaciados, estados y miniaturas en `dashboard.css` y `ui.tsx`, con alcance para el dashboard de catálogo. Se unificó también el bloque del responsable de la tienda. Títulos moderados, pesos principalmente 400/500/600, verde discreto, bordes suaves y foco visible. La vista previa del mensaje hereda la fuente del panel.

El editor del asesor se carga bajo demanda. Su chunk pesa 198418 bytes y el JavaScript inicial 1031819 bytes: la biblioteca telefónica no se agrega a la carga inicial del storefront. Permanece la advertencia de Vite por el tamaño del bundle inicial; no se realizó una reestructuración general de la aplicación.

### Asesores y selector público

- Alta y edición de nombre, WhatsApp, foto, provincia, localidad, detalle de zona y actividad en una sola ficha.
- País del teléfono con Argentina inicial; interpretación por país mediante `libphonenumber-js` 1.13.17 y Google libphonenumber 9.0.44 en Java.
- Confirmación explícita de celular argentino para interpretar números nacionales. Un internacional con `+` conserva su prefijo explícito. Se muestra el resultado y se ofrece **Probar contacto**.
- Los números históricos sin modificaciones conservan su interpretación compatible. La captura nueva valida con metadatos por país; no se ejecuta una normalización masiva. El formato no certifica una cuenta activa de WhatsApp.
- Foto opcional PNG/JPEG/WebP, hasta 5 MB y 20 megapíxeles. Se decodifica, recorta al centro y recodifica como JPEG cuadrado de hasta 600 px, con nombre asociado a la tienda. El frontend muestra vista previa y permite reemplazar o quitar.
- Foto y ficha se guardan juntas en una transacción. Un fallo revierte el registro y elimina el archivo nuevo. Quitar/reemplazar no elimina archivos anteriores que podrían seguir referenciados.
- `quitarFoto=true` quita la referencia; omitir foto la conserva. Para los campos de zona, omitir conserva y cadena vacía limpia.
- Zona opcional: provincia (80 caracteres), localidad (120), detalle (200). Se ofrecen provincias argentinas como sugerencias locales; la captura admite texto y no depende de una API geográfica externa. País telefónico y zona comercial son independientes.
- La zona ayuda a elegir asesor y no establece costo ni cobertura de envío. No se requiere ubicación del comprador ni se infiere por IP, GPS o teléfono.
- El selector público incluye avatar/iniciales, nombre, rol y zona. Mantiene el responsable destacado, la selección expresa y la revalidación de producto/contacto antes de abrir WhatsApp.
- Asesor inactivo o sin teléfono válido sigue excluido; foto, zona y cuenta de acceso al panel son opcionales.

**API:** se conservan los endpoints JSON de asesores y se agregan variantes multipart en las mismas rutas, con parte `datos` y foto opcional. El contrato público agrega únicamente foto y zona, sin email, credenciales o datos de acceso. Las escrituras y asignaciones de fotos validan propiedad por tienda; las fotos publicadas son recursos públicos por diseño.

### Productos

- Cabecera persistente en desktop con identificación, estado, regreso y una sola acción principal. Guardar está visible desde la primera pantalla a 1366×768 y durante el desplazamiento.
- Dos columnas independientes: información e imágenes; precio/envío y publicación. Una columna en tamaños menores con orden de lectura y tabulación coherente.
- Barra inferior en móvil con espacio reservado y adaptación a `visualViewport` para evitar quedar detrás del teclado cuando el navegador informa su reducción.
- Guardar se deshabilita sin cambios o mientras faltan datos/revisión válida. Se evita el doble envío.
- Los errores conservan el borrador y reciben foco; se mantienen cálculo, decimales exactos, imágenes, tokens y guardado conjunto.
- Volver recupera filtros, página, scroll y foco del listado.

No se alteraron reglas monetarias, cotizaciones, categorías propias de envío, stock, ni publicación de artículos.

### Difusión

- Filas con miniatura visible, nombre, precio y casilla de selección. Portada resuelta a partir del catálogo público, portada administrativa o medios del artículo, con alternativa visual ante ausencia/fallo.
- Lista y vista previa en desktop. En móvil, filtros secundarios desplegables y acceso **Ver mensaje** al seleccionar.
- Selección de hasta 20 artículos, conservada entre búsquedas, filtros y páginas.
- Compartir por WhatsApp, copiar mensaje y copiar enlace; se distingue artículo individual de lista y se conservan parámetros de tienda y asesor.
- Antes de actuar se releen productos públicos/privados y asesores. Un cambio detectado actualiza la vista previa y exige revisarla antes de compartir. Una selección retirada o contacto inhabilitado no se comparte silenciosamente.
- Los totales usan centavos exactos mediante BigInt; los precios proceden del catálogo público.
- Se comparten texto y enlace. La foto del panel, la vista previa Open Graph del enlace y un archivo adjunto son resultados distintos: este flujo no adjunta la fotografía ni envía mensajes automáticamente.

### Métricas

- Cabecera simple, períodos de 7/30 días y personalizado, validación de fechas y máximo de 366 días.
- Cuatro indicadores en catálogo: visitantes únicos estimados, visitas, vistas de productos y clics en WhatsApp.
- Rankings sobrios, miniaturas de productos y barras de rubros. Sin corona, degradados, efectos decorativos ni “Top 0”.
- La API agrega `imagenPrincipalUrl`. Las portadas se obtienen en un lote limitado a los IDs del ranking y a la tienda, sin consulta por fila ni descarga del catálogo completo.
- Se conservan eventos históricos aunque el producto no exista o esté inactivo; una foto ausente tiene alternativa visual.
- Visitantes no equivale a personas identificadas; clics no confirma mensajes, conversaciones ni ventas. No se alteró la semántica histórica de los eventos ni se inventaron conversiones.
- El panel anterior conserva su indicador de consultas cuando corresponde fuera del modo catálogo.

## Migración

V58 realiza un único ALTER TABLE en `asesores_venta`, agregando `provincia`, `localidad` y `zona_atencion`, todos opcionales. No modifica teléfonos ni valores comerciales, ni activa tiendas. Se comprobó contra un esquema V57 con datos sintéticos antes de aplicar en producción y se validó su aplicación única mediante Flyway/JPA.

La única dependencia frontend agregada es `libphonenumber-js`; la comparación del lockfile detectó cambios exclusivamente en esa entrada y en las dependencias directas del proyecto. No se actualizaron masivamente dependencias. Java agrega Google libphonenumber en versión explícita.

## Validaciones

| Entorno y comprobación | Resultado |
| --- | --- |
| Maven `package`, suite completa | 443 pruebas, cero fallos/errores/omitidas; JAR generado. |
| Build frontend | TypeScript y Vite correctos. |
| ESLint sobre los 13 módulos TS/TSX modificados | Sin errores ni advertencias. |
| Catálogo | 31/31. |
| Transición | 44/44. |
| Teléfonos nuevos | 13/13; números nacionales/internacionales y rechazo de entradas incompletas. |
| Dashboard Chromium | 35 combinaciones de vistas/tamaños, 18 grupos de casos, cero errores JavaScript. |
| Regresión de navegador anterior | 17 casos; cero errores JavaScript, solicitudes externas u operaciones nuevas de pedidos. |
| Java/MySQL aislado | 9 grupos, con sesiones reales de dos propietarios sintéticos y migración V58. |
| Reversión aislada | Código anterior con recurso V58: Flyway/JPA y catálogo correctos. |
| HTTPS de producción | Estáticos por hash, catálogo sin cambios, selector/WhatsApp interceptado, panel protegido y otras tiendas sin habilitación. |

La integración usa la base local `mercadodigital_mejoras`, creada a partir de fixtures anteriores verificadas como sintéticas, en MySQL aislado a 127.0.0.1:13316. La aplicación de prueba usa loopback 18090, credenciales sintéticas y transportes externos inactivos/loopback. No se usaron credenciales ni datos productivos para las pruebas.

Se verificaron persistencia y limpieza de foto ante rollback; alta sin usuario; conservación de zona/teléfono/foto en edición parcial y desactivación; rechazo de lectura/escritura/asignación de foto ajena; contrato público sin campos internos; portadas de métricas e históricos; rangos inválidos y metadatos del enlace compartido.

Navegador: 360, 390, 768, 1366 y 1920 px; editor separado, guardado visible durante scroll desktop y móvil, foco, reflow equivalente y zoom CSS al 200 %, imágenes y object URLs, borradores, conflictos, reintentos, filtros/paginación, selección y actualización de precios. Se probó reducción sintética de `visualViewport`; no constituye una prueba física de teclado móvil.

**Producción real:** se comprobó selector a 390 y 1366 px, responsable destacado y Ayelén con destino `wa.me/5493482670015`. Se interceptaron métricas POST y navegación a WhatsApp para evitar escrituras/mensajes de prueba. Acceso anónimo al dashboard redirige a ingreso y la API privada devuelve 403. No se inició sesión como propietario real.

## Respaldo y reversión

Respaldo privado: `/opt/mercado-digital/backups/mejoras-20261125`.

Incluye JAR original, entorno, frontend, almacenamiento, fuentes iniciales y dump de base. SHA-256 del dump: `52b77f490065380f3b12fb39d68e93fbd4f64221f51c218272858f49d826800e`. Tamaño: 6252303 bytes. Se verificaron hashes, lectura de archivos tar y estructura/cierre del dump. No se restauró este dump productivo en un entorno de prueba.

**Incidencia de reversión resuelta antes de publicar:** el JAR anterior sin V58 rechazó el esquema actualizado con “Detected applied migration not resolved locally: 58”. Se preparó un paquete de reversión que conserva byte a byte todas sus entradas, clases y dependencias y agrega únicamente el recurso SQL de V58. No requiere cambiar el entorno ni borrar datos. Su manifiesto registra la comparación completa del ZIP. Fue iniciado y probado contra el esquema V58 sintético.

Paquete compatible: `/opt/mercado-digital/work/mejoras-20261125/backend-reversion-compatible.jar`.
SHA-256: `b3b96edb77e323f5661dba78120ff16da6cf4d2f4cdc8cfef7195dbd443b5a58`.

Reversión preparada, **no ejecutada en producción**:

```sh
python3 /opt/mercado-digital/work/mejoras-20261125/revertir.py
```

Verifica release y hash actuales, vuelve al frontend anterior y al código anterior compatible, reiniciando únicamente MercadoDigital. Conserva V58 y datos nuevos; no restaura toda la base ni interviene Vidra. Si hay una publicación posterior, se detiene para revisión. Para futuras operaciones, no usar directamente el JAR original sin comprobar compatibilidad con el historial de migraciones.

## Evidencia y comandos

Directorio local: `artifacts/mejoras-20261125/`. Remoto: `/opt/mercado-digital/work/mejoras-20261125/`.

- `antes/`: capturas representativas de la publicación anterior, con API/sesión sintéticas.
- `evidencia/`: capturas de secciones, editor, asesor, selector y JSON de validación.
- `evidencia/publico-contacto-*.png`: selector sobre HTTPS real; las demás vistas privadas utilizan datos sintéticos.
- `integracion-resultados.json`, `reversion-resultados.json`, `reversion-compatible-manifest.json`.
- `publicacion-resultados.json`, `release-manifest.json`, `fuentes-validadas.json`, `cambios.json`.
- `backend-package.log`, `build.log`, `lint.log`, `catalogo.log`, `transicion.log`, `telefonos.log`, `browser.log`, `publico.log`.
- `fuentes-finales.tgz`, `frontend-publicado.tgz` y `evidencia-final.tgz`.

Los diagnósticos `fallo.*` corresponden a iteraciones previas del arnés (incluido el nombre accesible del selector de período); los resultados vigentes son los JSON finales y logs indicados. También se corrigió una referencia a requestAnimationFrame en la prueba DOM y la codificación del cliente SQL de fixtures. No quedaron fallos funcionales conocidos en los casos ejecutados.

Para repetir frontend en el entorno privado del VPS:

```sh
sh /opt/mercado-digital/work/mejoras-20261125/verificar-frontend.sh
sh /opt/mercado-digital/work/mejoras-20261125/regresion.sh
```

Para Java: `mvn -B package` en `backend`. Las pruebas HTTP locales están en `artifacts/mejoras-20261125/integracion.mjs`; requieren el MySQL de fixtures, la configuración sintética y el JAR compilado. Los scripts de transformación de archivos son evidencias de implementación y no deben reaplicarse sobre los fuentes finales. Los scripts de publicación/reversión pertenecen a esta release y contienen guardas de estado.

## Uso

1. **Asesores y contacto → Agregar asesor / Editar:** completar nombre y teléfono; seleccionar país y, si corresponde, celular argentino; revisar el número interpretado. Agregar foto y zona opcional; guardar. Probar contacto abre WhatsApp y no acredita por sí solo una cuenta activa.
2. **Zona:** completar provincia, localidad y detalle. El comprador ve estos datos junto al nombre al elegir contacto. No cambia envíos o precios.
3. **Productos → Editar:** modificar los datos y usar Guardar cambios en la cabecera desktop o barra inferior móvil. Se habilita cuando hay cambios válidos y una revisión de precio vigente.
4. **Difusión:** seleccionar uno o hasta 20 productos, elegir asesor si se desea y revisar el mensaje. Copiar mensaje/enlace o abrir WhatsApp. Si se detectan cambios, revisar nuevamente antes de compartir.
5. **Métricas:** elegir 7/30 días o un rango personalizado y consultar indicadores/rankings. Son medidas de interés, no ventas confirmadas.

## Límites y pendientes

1. Falta la revisión autenticada del propietario en producción. Las escrituras privadas se verificaron mediante integración Java/MySQL sintética y los recorridos del dashboard mediante API/sesión simuladas; no se afirma una sesión real del propietario ni una prueba de escritura contra productos reales.
2. Las vistas móviles y el teclado se emularon. No se certificó hardware Android/iOS, Safari ni teclado físico. Zoom se comprobó mediante reflow y CSS, no operando el control nativo de un navegador físico.
3. No se comprobó una cuenta real de WhatsApp ni su respuesta. No se enviaron mensajes. Los metadatos del enlace están verificados; la presentación final de su vista previa depende del cliente externo y su caché.
4. Recorte de avatar centrado automático, sin editor manual de encuadre. Los archivos anteriores se conservan; su limpieza futura debe contemplar referencias compartidas.
5. Vite mantiene advertencia por bundle inicial mayor a 500 kB. La instalación reporta 26 incidencias del árbol de dependencias; el lockfile solo cambió por la incorporación telefónica. No se hizo una actualización general ni se declara lint limpio de todo el repositorio.
6. El respaldo productivo fue comprobado por hash y lectura, sin ensayo de restauración completa. Sí se ensayó la reversión de código contra V58 en la base sintética. El informe no declara equivalencia con una plataforma sin limitaciones a partir de que compile.
