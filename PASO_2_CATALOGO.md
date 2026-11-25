# Paso 2: catálogo público y contacto por WhatsApp

Fecha: 24/11/2026. Evolución funcional autorizada posterior a la recuperación de producción.

## Estado de la entrega

Implementado en `C:\Sistemas\MercadoDigital-Reconstruido`: catálogo con un precio ARS calculado por tienda, publicación independiente del stock, selector de contactos comerciales, revalidación antes de abrir WhatsApp y bloqueo de nuevas compras para las tiendas habilitadas en ese modo.

**Desactivado por defecto.** No se habilitó ninguna tienda real, no se inició el backend con configuración productiva, no se ejecutaron migraciones en una base ni se desplegó. El original y el rescate permanecen fuera de las escrituras de este trabajo. Se conservaron los cambios locales anteriores.

Backend compilado y empaquetado: **353 pruebas correctas**. Frontend compilado: **23 pruebas de catálogo correctas**, más **11 regresiones recuperadas**. Navegador real con API HTTP simulada local: escritorio y móvil, teclado, errores y cambios de precio verificados. **No equivale a una prueba navegador → Java → MySQL ni a una entrega lista para producción.**

## Comportamiento público

- El backend reutiliza `CatalogoPrecioCalculator`: ARS original, o USD original por cotización de la tienda, más la categoría ARS únicamente si el envío no está incluido. No intervienen PSP, seguro, comisión ni tarifas logísticas anteriores.
- Configuración incompleta, categoría ajena, USD sin cotización o resultado inválido: el producto no aparece en el catálogo y el detalle/contacto responde 404. No se reemplaza por el precio anterior. El panel muestra los motivos pendientes.
- Se corrigió un límite del calculador del paso 1: un resultado positivo que redondea a `0.00` queda pendiente, sin precio publicable. Tiene prueba específica.
- Se respeta `activo`; el stock cero o nulo no filtra productos. No se muestran contadores, etiquetas de disponibilidad ni filtro/orden de stock en el recorrido nuevo. Los parámetros antiguos `stock=1&sort=stock-desc` no lo condicionan.
- Listado, ficha, relacionados, mensajes compartidos del propietario y metadatos del enlace utilizan el precio calculado. La ficha presenta las condiciones configuradas sin inventar plazos ni cobertura.
- Se conserva el campo numérico anterior de precio por compatibilidad y se agrega `precioPublicoArs` decimal como cadena. En el recorrido nuevo se formatea y ordena esta cadena sin pasarla por `Number`, conservando centavos incluso en importes grandes. Ambos campos representan el mismo precio final.

## Contactos y flujo del visitante

1. El visitante pulsa **Comprar** en la tarjeta o **Comprar por WhatsApp** en la ficha.
2. Se consulta el producto y los contactos actuales de la tienda resuelta por el servidor. Se abre un diálogo con producto, precio y condiciones.
3. El responsable aparece primero y destacado; su identidad visible es el nombre comercial de la tienda. Se agregan únicamente asesores activos de esa tienda con WhatsApp comercial válido. No se publican usuarios internos, emails de acceso, credenciales ni configuración de costos.
4. Al elegir un contacto se vuelve a consultar el estado. Si precio o teléfono cambiaron, o el contacto desapareció, se muestra el resultado y se exige una nueva elección. Nunca se redirige silenciosamente a otra persona.
5. Con información vigente se navega en la misma pestaña a `wa.me`, evitando depender de ventanas emergentes. El mensaje incluye producto, SKU cuando existe, precio ARS y el enlace compartible existente con `storefrontHost` y versión de imagen. Pide información para concretar la compra, medios de pago y entrega.

El mensaje queda **preparado**; el visitante decide enviarlo en WhatsApp. No se confirma una compra ni se crea cliente, pedido, pago, envío o reserva. La métrica existente `CLICK_WHATSAPP` representa el intento de contacto; no acredita un mensaje enviado ni una venta.

Los parámetros recibidos `asesorId`, `asesor` o teléfonos arbitrarios no deciden el destinatario. Si se elige un asesor, el enlace del mensaje conserva la atribución de esa elección expresa. Para el responsable no se arrastra un asesor anterior.

Con un solo contacto se presenta una acción directa identificada con esa persona. Con cero contactos se informa la ausencia; no se inventan números ni se hereda uno central. Se comprueba el formato telefónico; no la existencia de una cuenta activa de WhatsApp.

El diálogo usa la modalidad nativa del navegador, cierre con Escape y botón, ciclo de Tab/Shift+Tab, foco de retorno, bloqueo del fondo y cancelación de solicitudes al cerrar. Durante una apertura no admite solicitudes o navegaciones duplicadas. Tiene estados de carga, error y reintento.

## Contratos y resolución de tienda

Rutas relativas a la base de API:

| Método y ruta | Respuesta |
| --- | --- |
| `GET /public/catalogo/tienda?host=...` | Listado público de la tienda resuelta. |
| `GET /public/catalogo/tienda/productos/{id}?host=...` | Detalle público, precio final y condiciones. |
| `GET /public/catalogo/tienda/productos/{id}/contacto?host=...` | Producto vigente y contactos comerciales mínimos; solo en modo catálogo de revendedor. |

El servidor usa `StorefrontService.resolverPorHost`, valida disponibilidad pública y obtiene allí el revendedor/proveedor. El frontend público envía el host, sin decidir el revendedor mediante un ID libre. Producto, configuración, categoría y asesores se restringen a la tienda; se verifica también la pertenencia en el servicio nuevo.

Las rutas públicas anteriores permanecen por compatibilidad. Para un revendedor habilitado también pasan por la lógica independiente: no permiten recuperar el precio anterior como alternativa. Los proveedores conservan su catálogo anterior.

Las respuestas de catálogo/contacto y HTML compartible se sirven con `Cache-Control: no-store`. Las imágenes conservan su caché anterior. El catálogo y el detalle se refrescan al recuperar foco y cada 60 segundos mientras están activos; guardar configuración invalida las consultas públicas del mismo frontend. El contacto revalida siempre. No se implementó notificación instantánea entre todos los navegadores.

Los nuevos enlaces de WhatsApp y los mensajes del panel incluyen el precio en su parámetro de versión, conservando la estructura de vista previa. Esto distingue enlaces de precios distintos; no permite controlar las cachés externas de WhatsApp ni actualizar mensajes que alguien ya envió.

## Activación controlada por tienda

`CatalogoModoService` lee `catalogo.publico.revendedores`, enlazado a `CATALOGO_PUBLICO_REVENDEDORES`. Es una lista de UUID separados por coma y queda vacía por defecto. Un UUID mal formado impide iniciar esa configuración, en vez de habilitar otra tienda.

`StorefrontResponse.modoCatalogo` informa el modo público efectivo. No lo decide la URL ni un formulario del navegador. El campo administrativo `modo: PREPARACION` del paso 1 sigue siendo informativo; **no es el interruptor**. El panel distingue visualmente el catálogo activo usando el estado público efectivo.

Para una futura prueba integrada, primero preparar una base MySQL desechable con datos sintéticos y configuración exclusivamente local. Aplicar todas las migraciones, crear una tienda de prueba, configurar sus productos/contactos y usar únicamente su UUID en la variable. Por ejemplo, en una consola de ese entorno aislado:

```powershell
$env:CATALOGO_PUBLICO_REVENDEDORES = '00000000-0000-0000-0000-000000000002'
```

Este ejemplo por sí solo no prepara una base ni autoriza iniciar servicios con la configuración heredada. No se ejecutó durante esta entrega. Antes de habilitar una tienda se debe revisar su contacto, condiciones y productos pendientes; la lista permite simular también los estados incompletos para comprobar su tratamiento.

Quitar un UUID restaura la selección del recorrido anterior después del próximo inicio local. **No constituye una reversión automática de datos:** los productos nuevos sin stock o sin precio anterior válido requieren revisión antes de volver a habilitar el checkout.

## Bloqueos de nuevas compras e historial

| Entrada | Control en modo catálogo |
| --- | --- |
| `/producto/{id}/checkout` y `/producto/{id}/resumen-compra` | `CatalogoCompraGate` redirige a la ficha sin montar el checkout. Ambas pantallas comprueban además el modo del detalle recibido, por si la información del tenant quedó anterior. |
| `POST /public/pedido/directo` | `PublicPedidoDirectoService` rechaza antes de buscar/crear cliente y relaciones. |
| `POST /pedidos` y `POST /pedidos/canonico` | `PedidoService` rechaza antes de crear el pedido o modificar stock. |
| Entradas internas `crearPedidoPublicoPorProducto`, `crearPedidoCanonicoPublico`, `crearPedidoProductoPropio` | Comparten los controles del servicio; no permiten eludirlos invocando otra variante. |

La respuesta de bloqueo es **409**, con explicación del contacto por WhatsApp. El inventario de `new Pedido()` en el código de aplicación tiene tres construcciones, todas dentro de estos métodos controlados.

No se agregaron bloqueos de modo a `/{pedidoId}/pago` sobre pedidos existentes, retorno de PSP, comprobantes, tracking, cancelaciones ni webhooks. Se conservaron sus pruebas de regresión. Eso no certifica todos los recorridos históricos con base real: su revisión integral corresponde al paso 4.

## Panel y persistencia

- Las fichas conservan datos y precio anteriores. En modo catálogo el stock deja de solicitarse; editar una ficha no altera el stock histórico, aunque un cliente lo envíe. Una ficha nueva puede tener stock nulo, sin cantidades ficticias.
- Se agregó **V56__stock_opcional_catalogo.sql**: permite `NULL` en `revendedor_productos.stock`, sin actualizar filas. La entidad acompaña el DDL. En modo anterior el servicio sigue exigiendo stock; la validación de importes negativos permanece.
- La creación de fichas sigue usando el formulario anterior; su precio se identifica como **“Precio anterior ARS (no publica)”**. El precio ARS/USD publicable se configura en Catálogo con la vista previa del paso 1. No se sincronizan ambos precios ni se infiere envío incluido. Es una transición administrativa explícita; unificar completamente la creación puede hacerse después sin cambiar la fórmula pública.
- La pestaña Precios utiliza la configuración nueva en tiendas de catálogo. Redes sociales obtiene los precios publicables desde la API pública, omite productos pendientes y no vuelve al precio anterior ante errores. Los totales compartidos usan decimales exactos. Los contactos comerciales mantienen su gestión existente.
- Se agregaron cargas explícitas de media/categoría al listado de configuraciones para evitar búsquedas individuales en la proyección pública. El comportamiento real de consultas y bloqueos requiere MySQL.
- No se modificó V55 ni las migraciones anteriores. No hay backfill, migraciones ejecutadas ni alteraciones de operaciones históricas.

## Validación y evidencias

Directorio: `artifacts/paso-2-catalogo/`.

| Comprobación | Resultado |
| --- | --- |
| `mvn.cmd -B package` | **353 pruebas, cero fallos/errores/omitidas**, JAR generado. `backend-package-final.log`. Incluye 26 casos nuevos sobre la línea base de 327. |
| Backend nuevo | Fórmulas, precio cero por redondeo, stock nulo/cero, publicación, categorías/productos ajenos, cambios de costos, contactos, privacidad, host manipulado, acceso anónimo con filtros de seguridad reales y bloqueos de todas las variantes de creación. |
| `npm.cmd run build` | TypeScript/Vite correctos. Persiste aviso de bundle mayor de 500 kB. `frontend-build-final.log`. |
| `npm.cmd run test:catalogo` | **23 correctas:** siete del panel anterior y 16 del catálogo público. DOM, React, router y React Query reales; transporte y navegación externa simulados. `frontend-tests-final.log`. |
| Regresión reconstruida | **11 correctas**, precios masivos y tracking. `frontend-regresion.log`. |
| Chrome real | 1280 y 390 px sin desborde horizontal; selector, foco, Tab, Escape, retorno de foco, cero/un contacto, errores, reintento y actualización antes de elegir. Ninguna excepción JS, solicitud externa o creación de pedido en ese recorrido simulado. `verificacion-navegador.json`, `solicitudes-navegador.json`, `navegador-final.log` y cinco capturas PNG. |
| Revisión visual | Inspeccionadas capturas del catálogo, ficha y selector. Se ajustó la separación de la acción principal y se permitió envolver importes largos. |
| Lint | 30 errores y 19 advertencias heredados. Comparación por archivo/regla/mensaje, sin diagnósticos nuevos; los desplazamientos de línea no se cuentan como regresiones. `comparacion-lint.json`. No se declara lint limpio. |
| Conservación | `baseline-sha256.json`, `git-inicial.txt` e inventario de cambios propios `archivos-paso-2.json`. PASO_1_CATALOGO.md permanece intacto. |

Fallos encontrados durante la implementación: el primer backend necesitaba inyectar la nueva dependencia en sus pruebas anteriores; se adaptaron los mocks, sin omitir pruebas. TypeScript detectó un formateador anterior sin usos y se retiró. Chrome detectó que el recorrido nativo de Tab podía salir de los controles del diálogo; se agregó el ciclo explícito y una prueba. Una segunda ejecución del arnés falló por escape de comillas de su acción de cierre; se corrigió el arnés. Se conservan los registros iniciales y la ejecución final pasó.

Comprobaciones reproducibles sin base ni servicios externos:

```powershell
# Desde backend
mvn.cmd -B package

# Desde frontend
npm.cmd run build
npm.cmd run test:catalogo
node tests/catalogo-publico-browser-check.mjs
npm.cmd run preview:catalogo-publico

# Desde la raíz
node --test recuperacion/frontend-precios.test.mjs recuperacion/frontend-tracking.test.mjs
node artifacts/paso-2-catalogo/verificar-entrega.mjs
```

El preview usa datos sintéticos, API HTTP simulada y navegación externa sustituida. No sirve para acreditar cálculos Java, autenticación del propietario ni persistencia. El navegador usa Chrome instalado en Windows, perfil temporal propio y bloqueo de solicitudes externas; lo elimina comprobando su ruta. Los scripts `integrar-ui.mjs`, `integrar-panel.mjs` y `ajustar-compartidos.mjs` son registros de edición de **una sola ejecución**, no utilidades para volver a aplicar cambios.

## Pendientes que impiden un futuro despliegue

1. **MySQL local desechable:** no se encontraron MySQL/MariaDB, Docker/Podman ni servicios disponibles. Ejecutar V55/V56 y las anteriores, validar esquema/JPA, filas existentes/nuevas, graph de consultas, transacciones y concurrencia. Las pruebas actuales simulan repositorios.
2. **Integración completa:** verificar con navegador, Java y base locales los cuatro precios, cambio concurrente de cotización/envío, alta de ficha sin stock, ownership y publicación. El navegador de esta entrega usa una API simulada.
3. **Paso 3:** revisar y simplificar los accesos restantes del modelo anterior, menús/cuenta, términos, estados guardados de checkout y ayudas del panel. Los bloqueos mínimos de creación ya están implementados; no retirar las vías históricas a ciegas. Evaluar la unificación del formulario de ficha y precio.
4. **Paso 4:** probar pagos anteriores, comprobantes, callbacks, reintentos, cancelaciones y notificaciones. Sigue pendiente el defecto heredado de confirmar una transferencia de un pedido cancelado, documentado en INFORME_RECUPERACION.md; esta entrega no lo corrige ni lo considera comportamiento válido para desplegar.
5. **Paso 5:** revisar cada tienda y sus productos antes de activar. No inferir moneda, envío incluido, cotización ni teléfono. Revisar también textos comerciales anteriores que prometan logística, seguro o checkout.
6. **Paso 6:** completar validación local integral y configuración propia del futuro entorno, resolver la deuda técnica heredada correspondiente y definir la operación. No copiar credenciales productivas ni interpretar compilación correcta como equivalencia con producción.

Lo implementado aquí responde a las reglas nuevas autorizadas; no se presenta como lógica recuperada del JAR productivo. No se enviaron mensajes reales ni se verificaron cuentas, cierres de ventas o entregas por fuera de la plataforma.
