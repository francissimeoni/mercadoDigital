# Extensión del catálogo autónomo a tiendas de revendedores

Fecha: 25/11/2026. Estado: **migración aplicada, cuatro tiendas habilitadas y verificadas; sin pendientes de activación**.

## Regla aplicada

Los importes anteriores son precios en pesos argentinos con envío incluido. Las configuraciones faltantes se crean como ARS, mismo precio decimal exacto, incluye envío y sin categoría adicional. No se convierten monedas ni se agregan recargos. Las configuraciones existentes se conservan íntegramente, incluida Mega Reconquista.

El diseño público y el panel ya estaban disponibles en el código compartido. Esta operación prepara datos y amplía la lista explícita de revendedores habilitados; no modifica el diseño, el JAR ni la release frontend. Tampoco agrega migraciones SQL al esquema.

## Implementación

Herramienta: `backend/scripts/ExtenderCatalogoTiendas.java`.

- `preview`: lectura consistente, inventario de productos/configuraciones, posibles problemas y token SHA-256 de la revisión.
- `apply token`: transacción por tienda con bloqueo del storefront y de las filas existentes. Se compara el token para rechazar cambios ocurridos después de la simulación. Sólo se insertan configuraciones faltantes.
- `revert recibo.json`: revierte únicamente registros creados por la operación y el teléfono normalizado. Comprueba valores, versiones y fechas para rechazar el borrado de ediciones posteriores. Conserva la auditoría histórica.
- La repetición con una nueva simulación no crea filas adicionales ni modifica configuraciones previas.
- Se crea una configuración general vacía cuando no existe: sin cotización ni condiciones inventadas. Los productos ARS con envío incluido no necesitan cotización ni categorías de envío. Cada tienda puede configurar sus propios valores posteriormente.
- Los teléfonos locales se normalizan al destino que ya construía `normalizeWhatsAppPhone`: diez dígitos argentinos -> +549 y esos mismos dígitos, validado con libphonenumber. No se agregan teléfonos ausentes ni se habilitan asesores sin número.
- Se conserva el dominio legado de Jaam, sin completar ni cambiar su campo `dominio_publico`.

Orquestación y evidencia: `artifacts/extension-catalogo-20261125/`. Trabajo remoto: `/opt/mercado-digital/work/extension-catalogo-20261125`.

## Validaciones

- Diez pruebas de migración con MySQL local y datos sintéticos: tienda nueva, parcial y configurada, idempotencia, precio decimal máximo, stock cero, inactivos, reversión, protección de cambios posteriores, error intermedio y transacciones concurrentes.
- Cinco grupos de integración con el mismo JAR SHA-256 que producción: tres tiendas sintéticas, resolución, catálogo, ficha, contacto, stock cero, inactivos, permisos e identificación de productos de otra tienda.
- Regresión del panel sobre datos sintéticos: 35 vistas y 18 flujos, sin errores JavaScript.
- Simulación productiva de sólo lectura antes de escribir. Comparación de todos los precios públicos vigentes y de los identificadores esperados.
- Comprobaciones de resolución, catálogo y contactos después de habilitar; preservación de configuraciones previas y checksums de productos, medios, revendedores, asesores, categorías de envío, pedidos y pagos.
- La prueba sintética de reversión detectó un problema de codificación del proceso Java en Windows. Se fijó UTF-8 explícitamente y se repitieron las diez pruebas en una base sintética nueva. La integración corrigió un valor esperado calculado manualmente; no requirió cambiar lógica comercial.

## Respaldo y reversión

Respaldo privado: `/opt/mercado-digital/backups/extension-catalogo-20261125`. Contiene el dump de la base, el entorno anterior y el plan exacto. Se verifican tamaño, terminación y SHA-256 del dump. No se afirma haber probado su restauración completa. Las credenciales se leen exclusivamente en el VPS y el archivo temporal de conexión se elimina al terminar.

La release frontend anterior se conserva. La ampliación no cambia el enlace publicado ni el JAR.

Reversión preparada, no ejecutada en producción:

```sh
python3 /opt/mercado-digital/work/extension-catalogo-20261125/revertir.py
```

Primero comprueba que sigan vigentes el JAR, el entorno, la release y todos los registros de los recibos. Rechaza la operación si encuentra cambios posteriores. Luego detiene sólo MercadoDigital, revierte las altas por tienda y el formato telefónico, restaura la lista anterior y reinicia el servicio. No restaura toda la base ni borra ediciones nuevas. Si una reversión queda impedida por cambios posteriores, debe revisarse cada tienda antes de actuar.

## Uso por los revendedores

En su dominio, iniciar sesión y entrar en Mi tienda. El panel incluye Productos, Precios y envíos, Asesores y contacto, Difusión, Mi página y Métricas.

Los productos migrados comienzan en ARS y envío incluido. El revendedor puede editar posteriormente moneda y precio. Sólo necesita cotización propia para productos USD. Si excluye el envío del precio, debe crear y seleccionar una categoría de envío propia. La revisión de impacto y la presentación de cotización con dos decimales continúan vigentes.

Los compradores eligen contacto desde el producto y coordinan por WhatsApp. No se generan pedidos ni cobros nuevos. Se conservan todos los registros históricos; los asesores sin teléfono válido permanecen fuera del selector.

## Límites

Las verificaciones públicas no crean sesiones administrativas productivas ni envían mensajes de WhatsApp. La edición autenticada se comprueba con sesiones y datos sintéticos. No se certifican dispositivos físicos ni lectores de pantalla reales. La validación de un número y su destino no acredita que exista una cuenta activa de WhatsApp.

## Resultado final en producción

| Tienda | Configuraciones creadas | Públicos antes | Públicos después | Precios conservados |
|---|---:|---:|---:|---|
| Jaam Electronica | 307 | 299 | 300 | Sí |
| INFONEL | 105 | 93 | 96 | Sí |
| Cleo Store | 26 | 26 | 26 | Sí |
| Navarro ventas | 129 | 108 | 109 | Sí |
| Mega Reconquista | 0 | 144 | 144 | Sí |

Se crearon **567 configuraciones de producto y cuatro configuraciones generales vacías**. Se normalizaron los cuatro teléfonos de titulares al mismo destino histórico. Se conservaron las 176 configuraciones previas de Mega, su cotización, sus condiciones y sus contactos. Ninguna tienda pendiente; no había storefronts de proveedores dentro del inventario.

Los 526 artículos ya publicados de las cuatro tiendas conservan exactamente su precio. Se mantienen los 36 productos inactivos fuera del catálogo. Se agregan cinco artículos activos antes ocultos exclusivamente por stock cero:

- Jaam Electronica: Heladera Goodweather 320L Inox 2 Puertas GW-43WR — SKU `HEL-GOODW-GW43WR-320-INOX`, ID `eca8957b-0ed3-4d47-8a85-5189070316f8`.
- INFONEL: ❄️ SPLIT HD PLAY 3000 FRIGORÍAS — SKU `SPLITHDPLAY3000`, ID `01194fe8-4ce1-48cd-a64c-db069fc1a432`.
- INFONEL: 🎶 JBL PARTYBOX 720 — SKU `JBLPARTYBOX720`, ID `46066b02-9f19-481e-8454-7467c9530b0d`.
- INFONEL: 🎮 PS5 SLIM DISC – 1 TB — SKU `PS5SLIMDISC–1TB`, ID `b037d16b-ff54-41d4-a2ec-998dfe5f9ab4`.
- Navarro ventas: JUEGO DE LLAVES COMBINADAS TIPO RAQUET — SKU `TOT24`, ID `9bfb84e0-6a28-4136-b292-db814a856af3`.

Se verificó la segunda aplicación sin altas ni modificaciones para cada una de las cuatro tiendas, dentro de la ejecución productiva. Las filas previas se compararon íntegramente. Los checksums de productos, imágenes/medios, revendedores, asesores, categorías de envío, pedidos y pagos permanecen idénticos. No se escribieron campos de productos ni se publicó ninguna tienda o producto inactivo.

Release activa: `/var/www/mercado-digital-releases/storefront-composicion-20261125-r2`. JAR SHA-256: `71c4e82e56114f6af8b6b156d662bf69fc7b531f8aaa14529d662e92f58ff8aa`. El PID de MercadoDigital pasó de 62627 a 71717 por un reinicio para recargar la lista; Vidra conserva PID 1374, entorno, archivos y respuesta HTTPS. Sólo cambió en `.env` la línea `CATALOGO_PUBLICO_REVENDEDORES`.

Dump de respaldo: 6251002 bytes, SHA-256 `170b25e0b28f194ee9460ba4227aaec6d097b2cfc79ecc9b5b895038ed167200`.

La comprobación HTTPS real cubrió **30 capturas** (catálogo, ficha y selector; mobile y desktop; cinco dominios), búsqueda y regreso a resultados, acceso privado protegido y contacto de otro revendedor rechazado con 404. Se interceptaron cinco destinos WhatsApp correctos, sin enviar mensajes y bloqueando eventos de métricas. Cero errores JavaScript. La primera comprobación del selector necesitó esperar la carga asíncrona de contactos; se corrigió la espera y pasó el recorrido completo.

Se verificaron las cinco cuentas titulares vinculadas, activas y con rol de revendedor mediante lectura de relaciones, sin usar sus credenciales. El acceso autenticado de producción queda para la revisión del titular; los seis apartados y las operaciones se probaron en el entorno sintético.

Se comprobaron 762 fuentes/configuraciones por SHA-256 y se conservaron en el proyecto reconstruido y en el trabajo remoto. La única incorporación de código es la herramienta operativa de migración; no cambia el binario de la aplicación. Las configuraciones productivas y el dump permanecen privados en el VPS.
