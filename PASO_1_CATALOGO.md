# Paso 1: preparación del catálogo independiente

Fecha: 24/11/2026. Evolución funcional posterior a la reconstrucción de producción.

## Estado y alcance

Implementado: configuración privada por tienda, categorías comerciales de envío, precios originales ARS/USD por producto, cálculo centralizado, vista previa e impacto antes de guardar, contactos comerciales y formularios integrados al panel.

La entrada está en **Panel propietario → Preparar catálogo** (`/mi-tienda?tab=catalogo`). El formulario de precios de producto está dentro de esa sección y no pide stock. La gestión de WhatsApp de asesores está integrada en **Redes sociales**, junto a sus formularios existentes.

**El nuevo catálogo público no está activado.** No se modificaron los botones públicos ni el checkout, ni se crearon pedidos, pagos o envíos como parte de la configuración. No se desplegó ni se ejecutaron migraciones o servicios productivos. No se modificaron el proyecto antiguo ni el rescate.

El paso 1 compila y tiene pruebas locales correctas. **No está validada la migración ni la persistencia en MySQL; no constituye una entrega lista para producción.**

## Reglas implementadas

- Cada producto tiene una configuración privada con moneda ARS/USD, precio original positivo, respuesta explícita sobre envío incluido y categoría propia cuando corresponde.
- Cada tienda tiene cotización manual positiva con hasta seis decimales; puede permanecer pendiente si no usa USD. No se permite quitar una cotización utilizada por productos configurados en USD.
- Fórmula: `precioPublicoARS = precioOriginal × cotizacion` para USD, o `precioOriginal` para ARS, más el costo de envío únicamente cuando no está incluido.
- Cálculo con `BigDecimal`; se conserva precisión intermedia y se aplica `HALF_UP` a dos decimales solamente al resultado final. Se rechazan importes finales que exceden 17 dígitos enteros.
- No intervienen pasarela, seguro, comisión, margen global ni stock.
- Con envío incluido se ignora cualquier categoría recibida y se guarda el vínculo como nulo. Volver a excluirlo obliga a elegir una categoría válida de esa tienda.
- Las categorías tienen nombre único por tienda e importe ARS no negativo. Se bloquea su eliminación si están vinculadas a productos.
- La cotización y los costos se consultan al calcular: sus cambios se reflejan en los precios preparados sin sobrescribir los precios originales ni los importes de pedidos históricos.
- La vista previa de producto y las tablas de impacto usan exclusivamente el cálculo del backend. No se mantiene una fórmula equivalente en el frontend.
- Los datos incompletos se devuelven como `PENDIENTE`, con motivos y precio final nulo. No se completan con ceros o cotizaciones ficticias.

Ejemplos cubiertos por pruebas: ARS 100.000 incluido → 100.000; más envío 8.000 → 108.000; USD 100 a 1.200 incluido → 120.000; más envío 8.000 → 128.000.

## Aislamiento del modelo anterior

Se eligieron tablas separadas. El precio configurado en USD **nunca se escribe en `revendedor_productos.precio_venta` ni en su campo `moneda`**. Esos campos continúan siendo la entrada ARS del catálogo y checkout actuales.

Las ediciones y ajustes masivos actuales operan sobre ese precio anterior; no actualizan la configuración privada nueva. Se agregó una aclaración en su interfaz y se rechaza un ajuste masivo legado sobre una fila cuya moneda no sea ARS. No se convierten monedas silenciosamente.

Esto significa que durante la preparación existen dos configuraciones administrativas distintas, aunque el público sigue viendo únicamente el recorrido anterior. No deben mezclarse ni sincronizarse automáticamente. La unificación del formulario de creación de fichas y la activación pública requieren la transición del paso 2 y el cierre de entradas al checkout previsto para el paso 3.

El nuevo formulario configura productos existentes; no requiere ni altera existencias. El formulario anterior de creación/edición de fichas conserva su contrato legado mientras siga activo el checkout.

## Persistencia y migración

Se agregó `backend/src/main/resources/db/migration/V55__preparacion_catalogo_independiente.sql`. V54 era la versión más alta presente antes de este paso. No se modificaron migraciones anteriores.

| Recurso | Contenido |
| --- | --- |
| `catalogo_configuraciones` | Una fila por revendedor: cotización nullable, condiciones de entrega y versión. |
| `catalogo_categorias_envio` | Nombre e importe ARS por revendedor, con versión y referencias. |
| `catalogo_productos_configuracion` | Una fila por producto: moneda, precio original, envío incluido, categoría y versión. |
| `asesores_venta.whatsapp_comercial` | Columna nullable de 16 caracteres, suficiente para E.164 con `+`. |

Las tablas nuevas incluyen claves foráneas, unicidad, fechas y restricciones de integridad. El código valida también la pertenencia de la categoría a la tienda del producto. Las categorías son independientes de `tarifas_transportista`.

No hay backfill: los productos anteriores quedan sin configurar para este nuevo modelo. No se infiere envío incluido, moneda nueva, cotización, teléfono ni categoría. Se conservan productos, stock, asesores y operaciones históricas.

Se reutiliza `RevendedorStorefront.telefonoComercial`; no se creó un teléfono duplicado del revendedor. Guardarlo desde esta sección actualiza ese contacto comercial compartido y refresca el branding del panel. Los precios públicos siguen sin cambiar.

## Contactos y permisos

`WhatsAppComercial` normaliza a E.164 con `+` y permite volver a guardar el mismo valor normalizado. Admite números internacionales explícitos, el prefijo argentino `54` y formatos móviles argentinos como `011 15 ...` y `0351 15 ...`. Rechaza números locales ambiguos; no inventa un país o área faltante. La validación comprueba formato, no una cuenta activa de WhatsApp.

Los asesores pueden crearse como contactos sin email ni contraseña. Para crear acceso al panel siguen siendo necesarios ambos datos juntos. Actualizar un contacto no agrega roles ni reemplaza credenciales; omitir WhatsApp conserva el anterior y enviar una cadena vacía permite quitarlo. Omitir la foto también la conserva.

Los endpoints nuevos son privados y están bajo `/revendedores/**`: requieren REVENDEDOR o ADMIN_MD. Cada operación verifica además `RevendedorOwnershipService`; producto y categoría se buscan por tienda. Los asesores no adquieren acceso por figurar como contactos.

Los DTO de este módulo son administrativos. **No reutilizarlos como respuesta pública del paso 2:** contienen configuración interna. El futuro listado público de vendedores deberá exponer solamente identidad comercial y contacto, sin email de acceso ni credenciales.

## Contratos y concurrencia

Prefijo: `/revendedores/{revendedorId}/catalogo-config`, relativo a la base de API existente.

| Método y ruta | Uso |
| --- | --- |
| `GET /` | Configuración, versión, pendientes y modo constante `PREPARACION`. |
| `POST /vista-previa` | Impacto del cambio de configuración en productos USD. |
| `PUT /` | Guardar configuración con `impactoToken`. |
| `GET /categorias` / `POST /categorias` | Listar o crear costos propios. |
| `POST /categorias/{id}/vista-previa` | Comparar precios vinculados antes del cambio. |
| `PUT /categorias/{id}` | Guardar categoría con `impactoToken`. |
| `DELETE /categorias/{id}?version=...` | Eliminar una categoría sin vínculos. |
| `GET /productos` | Todos los productos de la tienda, incluidos los de stock cero, y su configuración. |
| `POST /productos/{id}/vista-previa` | Obtener `{calculo, token}` para un borrador. |
| `PUT /productos/{id}` | Guardar precio con `previewToken`. |

Las rutas de la tabla se concatenan al prefijo sin exigir una barra final adicional.

Los importes de entrada pueden ser cadenas decimales, como `"100.00"`. El precio final enviado por un cliente no es una fuente de cálculo. Para un alta se envía `version: -1`; para una edición, la versión leída.

Las escrituras del nuevo módulo se serializan por tienda mediante bloqueo de su storefront. Las filas de configuración tienen `@Version`; una versión obsoleta se rechaza. Los tokens de revisión son hashes de los valores y resultados; se recalculan al guardar y se rechazan si cambió el escenario mostrado. No son tokens de autenticación. El orden de los productos del impacto es determinista por ID.

Cambios de precio, cotización y categoría registran actor, tienda y valores anteriores/nuevos en `auditoria_eventos`, dentro de la misma transacción. La atomicidad y los bloqueos están implementados, pero aún requieren prueba real en MySQL.

## Comportamiento de los formularios

- Estados de carga, errores y guardado; controles deshabilitados durante operaciones de guardado.
- Vista previa con clave por borrador y cancelación mediante `AbortSignal`; una respuesta atrasada no se aplica al formulario nuevo.
- Revisión y confirmación separadas para cotización y costos. Editar los valores invalida la revisión anterior; un conflicto obliga a revisar nuevamente.
- Los borradores conservan su versión de partida y no se reemplazan con recargas de datos de otros formularios.
- Protección al salir de pestaña, enlaces o página con cambios sin guardar. Hay una acción explícita para recargar configuración y descartar el borrador con confirmación.
- Formularios con etiquetas, navegación nativa por teclado y disposición adaptable a móvil.

## Validaciones ejecutadas y evidencia

Directorio de evidencia: `artifacts/paso-1-catalogo/`.

| Verificación | Resultado / evidencia |
| --- | --- |
| Backend completo | `mvn.cmd -B package`: **327 pruebas, 0 fallos, 0 errores, 0 omitidas**, JAR generado. `backend-package-final.log`. |
| Reglas nuevas | Monedas y redondeos; faltantes y desborde; ownership y HTTP/JWT; categorías ajenas/en uso; versiones y tokens; stock cero sin cambios; precio legado intacto; contactos sin cuenta y compatibilidad de credenciales. 54 casos agregados sobre los 273 previos. |
| Frontend | `npm.cmd run build`: TypeScript y Vite correctos. Conserva aviso de bundle mayor de 500 kB. `frontend-build-final.log`. |
| Interfaz con DOM | `npm.cmd run test:catalogo`: **7 casos correctos**, React/React Query y DOM reales con transporte simulado; respuestas atrasadas, envío, tokens, pendientes, borradores y versiones. `frontend-tests-final.log`. |
| Regresión recuperada | **11 casos correctos** de precios y tracking con los arneses existentes. `frontend-regresion.log`. |
| Navegador | Chrome aislado con datos simulados: 1280 px y 390 px sin desborde horizontal; campos etiquetados; Tab pasa de cotización a teléfono; sin excepciones JS. `verificacion-navegador.json`, `navegador.log` y tres capturas PNG. Se inspeccionaron visualmente las capturas de escritorio y del formulario móvil. |
| Lint | **30 errores y 19 advertencias preexistentes, sin diagnósticos nuevos**. `lint-inicial.json`, `lint-final.json` y `comparacion-lint.json`. Los cinco archivos nuevos de aplicación también pasaron lint dirigido. |
| Alcance | Hashes previos de fuentes y estado Git en `baseline-sha256.json` y `git-inicial.txt`; inventario propio en `archivos-paso-1.json`. |

Se agregaron únicamente dependencias de desarrollo para el arnés: `jsdom` y declaración explícita de `esbuild`, que ya era dependencia transitiva de Vite. No se agregaron dependencias del backend ni servicios externos.

En la primera ejecución del arnés DOM quedaron temporizadores de recolección de mutaciones pendientes; se corrigió su configuración local. Otra revisión detectó claves React repetidas entre formularios; se diferenciaron y el arnés ahora falla ante advertencias de React. La validación final terminó normalmente y sin advertencias de renderizado.

### Limitaciones verificadas

- No se encontró MySQL/MariaDB, Docker/Podman ni un servicio local disponible. **No se ejecutó Flyway, no se validó JPA contra un esquema real ni se probaron bloqueos/rollback concurrentes en base.** Se revisó correspondencia estática entre DDL y entidades, sin equipararla a una prueba de persistencia.
- Los tests de servicio simulan repositorios; MVC utiliza filtros JWT reales pero servicios simulados. Las pruebas de interfaz y navegador simulan el transporte. No se ejecutó el recorrido completo navegador → API → MySQL.
- La vista de navegador se ejecutó aislada del layout completo de autenticación del propietario. No certifica navegación de cuenta ni integración real con WhatsApp.
- No se probó que los números tengan una cuenta activa, ni se enviaron mensajes.
- Los problemas heredados documentados en `INFORME_RECUPERACION.md`, incluida confirmación de transferencia de un pedido cancelado, no forman parte de este cambio y siguen pendientes para una futura entrega productiva.

## Cómo probar localmente

Pruebas sin base ni servicios externos, desde los directorios indicados:

```powershell
# Desde backend
mvn.cmd -B package

# Desde frontend, con dependencias del lockfile instaladas
npm.cmd run build
npm.cmd run test:catalogo
npm.cmd run lint

# Desde la raíz del proyecto
node --test recuperacion/frontend-precios.test.mjs recuperacion/frontend-tracking.test.mjs
```

Vista visual sin backend, desde `frontend`, después del build:

```powershell
node tests/catalogo-preview.mjs
```

Abrir la URL `127.0.0.1` que imprime. Muestra fixtures fijos claramente identificados: **no guarda datos ni sirve para validar cálculos al editar**. Detener con Ctrl+C.

La comprobación automatizada de navegador se ejecuta con `node tests/catalogo-browser-check.mjs`. Usa Chrome en su ubicación Windows habitual y un perfil desechable propio; si no está allí, ajustar únicamente la ruta del ejecutable. El perfil se elimina al finalizar con comprobación del destino.

Para probar persistencia y el panel completo primero se necesita una base MySQL desechable con datos sintéticos, todas las migraciones y una configuración explícitamente local. No iniciar el JAR con credenciales o URLs productivas. Habilitar seguridad, crear un propietario y storefront de prueba; abrir el panel con frontend local conectado a esa API. Verificar las cuatro fórmulas, categorías propias/ajenas, cambios concurrentes y rollback de auditoría/configuración. No usar un volcado productivo con contactos o credenciales reales.

El script `artifacts/paso-1-catalogo/integrar-panel.mjs` registra una edición de única ejecución: **no volver a ejecutarlo** sobre el panel ya modificado.

## Entrega concreta al paso 2

### Reutilizar el contacto existente

`frontend/src/storefront/pages/StorefrontProductDetailPage.tsx` ya construye `whatsappUrl` y presenta **Consultar por WhatsApp**. La función `buildProductWhatsAppUrl` preserva el enlace de publicación con `storefrontHost`, versión de imagen y parámetros `asesorId`/`asesor`; el texto actual comienza con «Hola! Quiero saber más sobre este producto…». Mantener esa estructura al agregar selección de vendedor y precio público.

`frontend/src/storefront/utils/storefrontContact.ts` contiene `normalizeWhatsAppPhone` y `buildStorefrontWhatsAppUrl`. El normalizador actual elimina símbolos, por lo que los nuevos teléfonos E.164 con `+` son compatibles para generar el número sin signos que requiere `wa.me`. Revisar sus reglas permisivas antes de usarlo como validación pública: la validación del paso 1 vive en backend.

Los parámetros de referido existentes no deben decidir silenciosamente el destinatario. El selector futuro debe permitir elección expresa y validar contactos activos de esa tienda.

### Retirar stock del recorrido nuevo

- `PublicCatalogoService.getCatalogo`: consulta `findByRevendedor_IdAndActivoTrueAndStockGreaterThanOrderByNombreAsc(..., 0)`.
- `PublicCatalogoService` al resolver detalle: filtra por activo y stock positivo; además devuelve `stockDisponible`.
- `StorefrontCatalogPage.tsx`: filtro `stock=1`, checkbox de disponibilidad y orden `stock-desc`.
- `StorefrontProductCard.tsx`: cálculo `disponible`, etiquetas Disponible/Sin stock y cantidad.
- `StorefrontProductDetailPage.tsx`: prioridad por stock entre opciones y métrica de disponibilidad.
- El checkout y `PedidoService.crearPedidoProductoPropio` siguen teniendo controles, bloqueo y descuento de stock. No sustituirlos por cantidades ficticias: dejar de usarlos en el recorrido nuevo y tratar las operaciones históricas por separado.

### Activación pendiente

`modo: PREPARACION` es informativo y constante; no existe un interruptor oculto que publique estos precios. El paso 2 debe introducir explícitamente la selección del recorrido de catálogo, consumir este cálculo desde un contrato público limitado y verificar contactos/configuración pendientes.

Coordinar su habilitación con el paso 3: el nuevo precio no debe mostrarse junto a un enlace que todavía permita comprar con el precio legado. Implementar el selector reutilizando el contacto existente, retirar todas las condiciones de stock del recorrido nuevo y preparar el cierre de sus entradas al checkout, conservando historial y pagos previos.
