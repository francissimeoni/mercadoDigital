> Actualización posterior: la release vigente es `storefront-20261125-r1`. Consultar [INFORME_MEJORA_STOREFRONT_PUBLICO.md](INFORME_MEJORA_STOREFRONT_PUBLICO.md); los procedimientos de reversión anteriores pertenecen a sus respectivas releases.

# Corrección del dashboard de MercadoDigital

> Actualización posterior del 25/11/2026: consultar [el informe vigente](INFORME_MEJORAS_ASESORES_PRODUCTOS_DIFUSION_METRICAS.md), release `mejoras-20261125-r1` y migración V58. Este documento conserva el contexto de la corrección inicial. Para revertir la versión actual usar únicamente el procedimiento del informe vigente.

Fecha de la solicitud y release: 24/11/2026. Estado: **publicado y verificado**, con las limitaciones indicadas abajo.

## Resultado y alcance

Release activa: `/var/www/mercado-digital-releases/dashboard-20261124-r1`, mediante el enlace `/var/www/mercado-digital-web`.

Se corrigió el panel de las tiendas con modo catálogo. Mega Reconquista sigue siendo el único piloto. Se preservó el panel anterior para las demás tiendas y los datos históricos. No se modificaron backend, contratos HTTP, migraciones, variables productivas ni precios de artículos. No hubo reinicios. El backend conserva PID 25165 y SHA-256 `6e8366e8d71e8e9cb44b95d89911e1def8c82b709dddb46aee35a4fcf261a2d5`.

Vidra conserva PID 1374, servicio activo, entorno y los 23 archivos registrados en la comprobación anterior, con hashes idénticos y HTTPS 200. No se modificaron Nginx, Docker, configuración de MySQL, servicios compartidos ni credenciales externas. La única escritura de negocio productiva fue el contacto autorizado de Ayelén. Mercado Pago permanece retirado de MercadoDigital; la integración de Vidra está intacta.

## Causas verificadas y correcciones

| Problema | Corrección |
| --- | --- |
| En Mi página el aviso era el primer hijo de un grid de dos columnas; el formulario ocupaba la columna de 340 px. | El aviso queda fuera del grid; el formulario ocupa el espacio principal. En la prueba de 1366 px su ancho fue 1066 px. La cuenta pasa a una columna auxiliar solo cuando hay espacio suficiente. |
| Navegación horizontal de aproximadamente 1200 px en móvil. | Navegación lateral en desktop y selector nativo con las seis secciones en móvil/tablet. |
| Productos y Configurar catálogo montaban la misma pantalla. | Secciones independientes de Productos, Precios y envíos, Asesores y contacto, Difusión, Mi página y Métricas. |
| Productos, cotización, envíos y asesores mezclados con listados extensos. | Responsabilidades separadas. Listados de productos y difusión paginados de a 12. El editor conserva filtros y página al volver. |
| Administración de asesores después de los 144 productos compartibles. | Pantalla propia; muestra asesores inmediatamente después del responsable. Estado comercial explícito y acceso al panel opcional. |
| Dashboard de catálogo conservaba pagos, pedidos y consultas anteriores. | Se retiraron sus accesos, paneles, avisos y consultas. Los parámetros antiguos o desconocidos se normalizan a una sección vigente; no montan las pantallas retiradas. |
| Productos y rubros se consultaban incluso en métricas o apariencia. | Cada sección consulta sus propios datos. Claves de caché privadas separadas por tienda. |
| Mi página duplicaba la edición del WhatsApp y conservaba transferencias/SMTP sin función en el catálogo. | El WhatsApp se administra en Asesores y contacto. Se ocultan transferencias y SMTP del panel de catálogo. Antes de guardar apariencia se releen y conservan teléfono y configuración histórica actuales, incluida la contraseña SMTP existente mediante envío vacío conforme al contrato. |
| Protección incompleta de borradores. | Se protegen navegación, enlaces, recarga y atrás/adelante. La prueba detectó que registrar POP solo en un efecto era demasiado tarde: React Router podía desmontar el formulario antes de ejecutar la protección. El listener se instala al cargar el módulo, antes del router, y actúa únicamente si hay borradores. |
| Cinco imágenes de 5 MB podían exceder los 20 MB del proxy. | Se mantiene 5 MB por imagen y se limita la selección del producto a 19 MB por guardado, dejando margen multipart. Apariencia valida 5 MB por archivo y muestra errores. Nginx continúa con `client_max_body_size 20M`. |

Se conservan los cálculos y precios del backend, las cadenas decimales, las revisiones de impacto, versiones y tokens. El cambio de dólar/categoría sigue requiriendo revisión y confirmación. Stock no condiciona publicación ni contacto. Los productos preparados del piloto conservan ARS y envío incluido.

## Ayelén Fernández

Lectura productiva inicial: asesora activa de Mega Reconquista, ID `cf5f45cc-46a2-4ef1-a2f1-02308b231b3c`, sin usuario y con `whatsapp_comercial` NULL. El filtro público de contactos funcionaba correctamente al excluir números ausentes o inválidos.

El propietario proporcionó `3482670015` y confirmó mantener esa exclusión. Se guardó `+5493482670015`, con condición de tienda, ID, activo y valor anterior NULL. Se respaldó la fila antes del cambio y se registró auditoría `CATALOGO_CONTACTO_ACTUALIZADO`, ID `3a4f7478-e495-4596-bc3b-3151dce2e1cc`. Las tablas de configuración del catálogo conservaron sus hashes.

**Verificado en producción:** la API y el selector público ofrecen Mega Reconquista como responsable y Ayelen Fernandez como asesora. Chromium comprobó la selección a 390 y 1366 px. Elegirla prepara `wa.me/5493482670015` con producto y precio. La navegación a WhatsApp fue interceptada: no se envió ningún mensaje. El formato no certifica que exista una cuenta de WhatsApp ni que el asesor responda.

## Arquitectura y archivos

`StorefrontOwnerPage.tsx` valida resolución, sesión, rol y coincidencia de tienda; delega al panel de catálogo o al panel anterior. La implementación anterior se conserva en `LegacyStorefrontOwnerPage.tsx` para las tiendas fuera del piloto.

La carpeta `frontend/src/storefront/dashboard/` contiene layout/navegación, contactos, difusión, apariencia, métricas y estilos/estados compartidos. `CatalogoAdminSection` permite montar exclusivamente productos, precios/envíos o contacto, preservando la preparación integral anterior. `CatalogoProductManager`, `CatalogoProductEditor`, `CatalogoAdvisorContact` y `useCatalogoDraft` contienen los ajustes de sus recorridos respectivos.

Se actualizaron las pruebas de catálogo y transición y se incorporó `frontend/tests/dashboard-browser-check.mjs`. El arnés de transición anterior también se adaptó al retiro global de Mercado Pago y al nuevo menú: conserva la prueba del checkout por transferencia únicamente en su escenario anterior explícito; proveedor sin pasarela muestra el contacto externo. No se habilitó Mercado Pago para hacer pasar pruebas.

Los 770 archivos de partida se transfirieron al VPS preservando cambios locales no confirmados y se verificaron por SHA-256. Se excluyeron secretos, bases, rescates, respaldos y dependencias. Trabajo remoto: `/opt/mercado-digital/work/dashboard-20261124`. Los fuentes corregidos se sincronizaron luego a `C:\Sistemas\MercadoDigital-Reconstruido`: los 253 archivos frontend coinciden con el VPS. También se sincronizó `frontend/dist`, cuyos cinco archivos coinciden con la release publicada. El compilado local anterior se conserva en `artifacts/dashboard-vps/dist-local-anterior`. No se modificaron el original ni el rescate.

## Validaciones ejecutadas en el VPS

- `npm run build`: TypeScript y Vite correctos.
- `npm run test:catalogo`: 31/31.
- `npm run test:catalogo-transicion`: 44/44, incluidas rutas retiradas, consultas innecesarias y formatos/estados de WhatsApp.
- ESLint sobre los componentes, hooks y módulos activos modificados: sin errores ni advertencias.
- `npm run test:dashboard-browser`: 30 combinaciones de seis secciones y cinco tamaños, 360×800, 390×844, 768×1024, 1366×768 y 1920×1080. Sin desbordamientos de página ni controles fuera de pantalla; cero errores de JavaScript.
- Once grupos de comprobaciones de navegador: paginación/retorno, edición USD con envío y token, revisión de dólar, revisión de categoría usada, contacto sin usuario, conservación de configuración oculta, borradores y Atrás, error/reintento, archivo excesivo y preparación de imagen válida, conflicto conservando borrador, y selector público con foco/elección explícita. Las operaciones de imagen están incluidas en el grupo de conflicto del JSON.
- `npm run test:catalogo-transicion-browser`: conserva las comprobaciones de históricos, aislamiento, recarga/atrás/adelante, cierre del pago y foco; cero errores JavaScript, cero solicitudes externas y cero nuevas operaciones de pedidos.
- HTTPS publicado: archivos estáticos verificados por SHA-256; mismos 144 productos públicos con las mismas respuestas e importes antes/después; Ayelén presente en ambos tamaños; visitante del panel redirigido a ingreso; API privada 403 sin sesión.
- Resolución pública: Mega, con y sin www, mantiene catálogo activo; Cleostore, Infonel y Navarro permanecen fuera del catálogo nuevo. Mercado Pago falso en esas respuestas de MercadoDigital.
- Backend, entorno de MercadoDigital y comprobación de Vidra idénticos antes/después.

Herramientas aisladas: Node 24.15.0, compatible con los engines de las dependencias; Playwright 1.56.1/Chromium y bibliotecas extraídas bajo `/opt/mercado-digital/tools`. No se instalaron servicios ni se cambiaron paquetes globales del sistema. Los navegadores y APIs simuladas se cerraron al terminar.

Ejemplo de ejecución reproducible de la nueva suite en el VPS:

```sh
export PATH=/opt/mercado-digital/tools/node-v24.15.0-linux-x64/bin:$PATH
export PLAYWRIGHT_BROWSERS_PATH=/opt/mercado-digital/tools/browsers
export LD_LIBRARY_PATH=/opt/mercado-digital/tools/browser-libs/usr/lib/x86_64-linux-gnu
export DASHBOARD_PLAYWRIGHT_MODULE=/opt/mercado-digital/tools/browser/node_modules/playwright/index.mjs
export DASHBOARD_OUT=/opt/mercado-digital/work/dashboard-20261124/evidencia
cd /opt/mercado-digital/work/dashboard-20261124/frontend
npm run test:dashboard-browser
```

## Evidencia y respaldo

En el proyecto local: `artifacts/dashboard-vps/`. Resultados principales:

- `evidencia/navegador-resultados.json` y capturas por sección/tamaño.
- `evidencia/publico-resultados.json` y capturas públicas de contactos.
- `transicion-browser/verificacion-navegador.json`.
- `build.log`, `tests-catalogo.log`, `tests-transicion.log`, `lint-cambios.log`.
- `publicacion-resultados.json`, `release-manifest.json` y manifiestos de fuentes.

Los archivos `fallo.*` son diagnósticos de intentos previos del arnés; los resultados vigentes son los JSON y logs finales indicados. No se interpreta cada fallo del arnés como un defecto de la aplicación.

Respaldo privado: `/opt/mercado-digital/backups/dashboard-20261124`.

- Frontend anterior: `piloto-catalogo-20261125-r4`.
- `frontend.tgz`, SHA-256 `3f2570983c3db2ae1f4a7df73f2acec9f5bab317ba2137c00b080dce3f155d21`.
- `base.sql`, 6208397 bytes, SHA-256 `39748c7d77a6ad5ed2ba63b1654c4617f0d28fd72449b59dd9c91eace40e4a52`.
- Fila anterior del contacto, auditoría y manifiesto del respaldo.

Se verificaron hashes, lectura del tar y estructura/cierre del dump. **No se ensayó la restauración completa de este dump en esta ejecución.** No se ejecutaron aplicaciones ni pruebas de escritura contra una copia de datos productivos.

### Reversión

El script `revertir-frontend.py`, junto al informe remoto, verifica que esté activa esta release y restablece atómicamente el enlace al frontend anterior. No reinicia servicios ni restaura la base. No debe ejecutarse después de otra publicación sin revisar el estado.

```sh
python3 /opt/mercado-digital/work/dashboard-20261124/revertir-frontend.py
```

El contacto de Ayelén es una corrección independiente autorizada y se conserva al revertir el frontend. La fila anterior y su auditoría permiten una reversión específica si el propietario la solicita; no corresponde restaurar toda la base por un cambio de interfaz.

## Límites y pendientes

1. **No se completó ingreso con una cuenta real del propietario en producción.** Se verificaron los componentes privados con sesión/API sintéticas en Chromium, y la protección de la API real sin sesión. Falta la revisión final autenticada del propietario; no se fabricaron tokens ni se desactivó seguridad.
2. Los guardados de dólar, envío, productos e imágenes del navegador usaron API simulada y validan interfaz/contratos, no una nueva integración Java/MySQL. No cambió el backend y no se ejecutó de nuevo su suite de 432 pruebas; esa cifra pertenece a la etapa previa.
3. Vite conserva la advertencia de bundle mayor de 500 kB. No se actualizó masivamente el proyecto ni se afirma que todo el repositorio tenga ESLint limpio: la comprobación limpia corresponde al alcance modificado indicado.
4. El número de WhatsApp fue proporcionado por el propietario. No se verificó mediante envío real, cuenta del proveedor ni conversación.
5. SMTP, transferencias y datos históricos permanecen almacenados; retirarlos de este dashboard no implica borrarlos ni rediseñar otros paneles administrativos.
6. Los tamaños móviles se emularon en Chromium; no se realizaron pruebas físicas en Android/iOS ni una certificación de otros navegadores.

## Uso para el propietario

1. Entrar a **Mi tienda**. En móvil, usar **Sección del panel**.
2. **Productos:** buscar, filtrar activo/inactivo, crear o editar ficha. Elegir ARS/USD, precio original y envío incluido o categoría propia. Revisar el precio ARS, marcar activo cuando corresponda y guardar.
3. **Precios y envíos:** indicar pesos por 1 USD, revisar impacto y confirmar. Crear categorías en ARS; al editar un costo, revisar productos afectados y confirmar. No se cambia el precio ARS original por actualizar el dólar.
4. **Asesores y contacto:** administrar el responsable y el WhatsApp de los asesores. “Disponible para compras” requiere activo y número válido; una cuenta de acceso al panel es opcional. Ayelén ya cumple esos requisitos.
5. **Difusión:** buscar productos publicados, elegir asesor del enlace si se desea, copiar o compartir un artículo o una lista de hasta 20. La elección final del contacto pertenece al comprador.
6. **Mi página:** identidad, portadas, textos, galería y redes. **Métricas:** visitas, productos vistos y clics de WhatsApp; no equivalen a ventas ni mensajes enviados.

Comprobación comercial: abrir una ficha pública → Comprar por WhatsApp → elegir Ayelén o al responsable → revisar el mensaje en WhatsApp. El comprador decide enviarlo; MercadoDigital no registra un pedido ni cobra.
