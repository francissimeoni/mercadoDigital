# Paso 6: integración y cierre técnico local del catálogo

Fecha: 24/11/2026. Proyecto: `C:\Sistemas\MercadoDigital-Reconstruido`.

La validación incorporó MySQL real, autenticación contra Java y navegador con la aplicación completa. Se corrigieron defectos que las pruebas anteriores con dependencias simuladas no detectaban. La aceptación local y sus límites están respaldados en `artifacts/paso-6-catalogo/`; no hubo despliegue ni activación de tiendas reales.

## Correcciones

### Altas concurrentes y lecturas después del bloqueo

Con el aislamiento predeterminado REPEATABLE_READ, la consulta de propiedad realizada antes de esperar el bloqueo de storefront podía establecer una instantánea anterior. Después de que otra instancia confirmara el alta, la segunda solicitud no veía todavía su clave de idempotencia. La unicidad de MySQL impedía duplicar el producto, pero se devolvía 409 en lugar de recuperar la ficha guardada.

Se estableció READ_COMMITTED explícitamente en las escrituras de `CatalogoFichaService` y `CatalogoAdminService`, conservando el bloqueo por tienda, las versiones, los tokens y la unicidad. Así, las consultas posteriores al bloqueo observan los cambios confirmados. El cambio es específico de esas transacciones y no cambia globalmente el aislamiento del datasource.

La regresión real mantiene una fila bloqueada mediante una conexión independiente, envía solicitudes a dos procesos Java y comprueba al menos dos esperas en `performance_schema.data_lock_waits` antes de liberar. El alta idéntica devuelve el mismo ID en ambas respuestas. Las dos confirmaciones de categoría y las dos ediciones de ficha producen un ganador y un conflicto, sin sobrescritura silenciosa.

La evidencia anterior al arreglo está en `integracion-antes-correccion-aislamiento.json`. Las respuestas 200/409 anteriores no eran equivalentes a un reintento idempotente satisfactorio.

### Respuestas de errores

El rechazo de propiedad de un pedido histórico se convertía en HTTP 500 por el manejador genérico. Se agregó el tratamiento global de AccessDeniedException con 403 y mensaje sin detalles internos. Las cargas que exceden los límites multipart responden 413. Los errores inesperados devuelven un mensaje recuperable y dejan el diagnóstico en el registro del servidor, sin entregar SQL/rutas internas al navegador.

Se añadieron tres regresiones MVC. La integración comprueba además el rechazo real entre tiendas y la carga de más de 5 MB sin crear el producto. El manejador específico de catálogo conserva sus contratos existentes de conflicto y permisos.

### Secretos de configuración heredados

Se reemplazaron los valores incrustados de `client-secret`, `access-token` y `public-key` de Mercado Pago en `application.yml`, `application-local.yml` y `application-prod.yml` por variables de entorno. Los tres archivos se cargaron con el parser YAML de Spring y se verificó que esos campos provengan del entorno. No se contactó al proveedor ni se probó su validez. No se reproducen los valores anteriores en este informe.

La aplicación local de esta etapa usa un archivo externo completo y un entorno de proceso limitado, con valores sintéticos. La limpieza del archivo actual no certifica la eliminación de copias históricas ni la renovación operativa de credenciales.

## Entorno y migraciones

- MySQL Community 8.0.44 portable, sin servicio global, escucha en 127.0.0.1:13316. La serie 8.0 coincide con `backend/docker-compose.yml`; no se inspeccionó la versión/configuración del VPS.
- Java 17, Spring Boot 3.5.11, Hibernate 6.6.42.Final y herramientas del proyecto. Los procesos Java de integración usan 18086/18087, seguridad JWT activa y JPA `validate`.
- Base principal exclusivamente sintética: `mercadodigital_paso6`. Usuario de aplicación limitado a esa base. Bases auxiliares de actualización/restauración separadas.
- Archivos, configuración, copias SQL y Chrome en `artifacts/paso-6-catalogo/runtime/`, excluido de Git. El root de MySQL recibió una contraseña aleatoria local antes de crear los fixtures.
- PSP/OAuth apuntan a loopback; email usa transporte log y payout noop. No se heredó un perfil ni variables productivas. El proveedor HTTP de históricos es un doble local que solo responde a pagos sintéticos.

Se aplicaron los **45 archivos de migración disponibles**, V1–V33 y V46–V57, sobre una base vacía sin baseline automático ni repair. El arranque JPA validó el esquema y los repositorios se ejercitaron realmente. Se verificaron nulabilidad de precio/stock/paquete heredados, versión e índice único de alta por tienda. No se añadió ni modificó ninguna migración en este paso.

En otra base se aplicó hasta V54, se insertaron una ficha anterior con ARS 123.456,78 y stock 17 y un pedido con importes conocidos, y se ejecutaron V55–V57. Los valores permanecieron idénticos y no aparecieron configuraciones de catálogo inferidas. Esa base también arrancó con JPA `validate`.

El dump sintético se restauró en una tercera base y se compararon las cantidades de filas de sus **45 tablas** y los valores de los fixtures. Es evidencia de ese ensayo local, no de un respaldo/restauración del VPS.

Los comentarios de V46/V47 hacen referencia a migraciones anteriores perdidas. La base sintética no contiene un historial productivo de versiones ausentes. Verificar el historial real antes de preparar un despliegue sigue siendo indispensable; no se inventaron checksums ni se configuró ignorar esas discrepancias.

## Qué se probó de punta a punta

### Catálogo y tiendas

Los escenarios reales usan tiendas A/B con propietarios distintos, administrador, cliente, asesor y tienda en modo anterior. Se comprobaron:

- Login real, roles y rechazo de operaciones administrativas por visitantes, clientes, asesores o propietarios ajenos.
- Categorías con igual nombre y distintos IDs por tienda; consulta/asignación ajena rechazada; eliminación de categoría vinculada bloqueada.
- Alta y edición unificadas, ARS/USD, envío incluido o adicional y campos heredados nulos.
- USD 100 × ARS 1.200 + envío ARS 8.000 = ARS 128.000; incremento a ARS 10.000 = ARS 130.000. El incluido queda en ARS 120.000, la tienda B conserva su importe y el pedido histórico no cambia.
- Cambio de cotización que afecta USD y conserva ARS. Vistas previas anteriores rechazadas.
- Stock cero/nulo sin impedir el catálogo; productos inactivos no publicados; faltantes de configuración sin precio alternativo inventado. Un envío cero se guarda como elección explícita.
- Precio `99999999999999999.99` conservado en MySQL, respuesta privada y cadena pública; metadatos de compartir con el importe vigente.
- Imagen real, reintento idéntico sin duplicar relaciones y clave reutilizada con otros datos rechazada.
- Altas, confirmaciones y ediciones concurrentes entre dos JVM mediante barrera real.
- Fallo deliberado de auditoría al confirmar la transacción: producto, precio y auditoría revertidos y archivo nuevo eliminado. Se comparó el inventario recursivo previo/posterior del almacenamiento para preservar los anteriores.
- Multipart mayor a 5 MB rechazado antes de persistir.

`integracion-resultados.json` contiene **18 escenarios** y sus estados HTTP. Las pruebas directas de servicios de pasos anteriores siguen aportando casos unitarios adicionales; estos 18 escenarios no pretenden sustituirlas.

### Operaciones históricas

`historicos-resultados.json` contiene **8 escenarios** sobre MySQL y dos JVM:

- Transferencia frente a cancelación: un resultado válido y una transición rechazada, con stock consistente.
- Dos cancelaciones: reposición una sola vez.
- Lecturas y comprobantes rechazados para otra tienda; comprobante sintético disponible para su propietario.
- Webhooks firmados repetidos: un único efecto de confirmación; rechazo posterior no revierte un cobro confirmado.
- Cobro tardío auditado sin reabrir pedido ni alterar stock.
- Cuenta receptora, moneda e importe incorrectos sin confirmar el pago.
- Firma inválida rechazada antes de consultar el proveedor local.
- Fallo al commit revierte pago/notificaciones persistidas; no se observó auditoría de correo del pedido fallido durante la ventana asíncrona de 1,5 segundos.

La última comprobación se complementa con la revisión de los listeners AFTER_COMMIT. No prueba una cola durable, entrega SMTP ni garantiza lo que sucedería tras perder el proceso. Sin SMTP de tienda configurado se conserva el comportamiento de omitir/auditar; no se anuncia entrega de un correo real.

### Navegador real

Chrome ejecutó React completo a través de Vite, con proxy a Java y sesiones obtenidas desde el formulario de login. Los endpoints locales no se sustituyeron por respuestas inventadas.

Los **8 controles agrupados** verifican login, rechazo de un decimal ambiguo, borrador conservado, creación unificada persistida, archivo desde el selector, actualización de categoría por interfaz y actualización de precio desde otra sesión. El selector vuelve a consultar el precio, avisa el cambio, destaca al titular y ofrece solo el asesor habilitado de la tienda.

El recorrido adicional de interfaz pasa de ARS 141.000 a 142.000 al confirmar una categoría; otra sesión la incrementa y el contacto muestra ARS 143.000 con aviso. Son datos sintéticos de esa fase, distintos del ejemplo inicial 128.000→130.000.

Se interceptó la navegación a WhatsApp antes de salir de la computadora y se comprobó destinatario/texto, sin enviar mensajes ni crear pedidos/pagos/envíos. Las capturas a 1280 y 390 px se revisaron visualmente: sin desborde horizontal ni campos sin etiqueta en la ficha y sin excepciones JavaScript en los recorridos registrados.

No se cronometró un ciclo completo de polling entre pestañas. Se mantiene el refresco existente de hasta 60 segundos en pestañas activas y la revalidación inmediata del contacto. No se controla la caché externa de WhatsApp.

### Volumen y reversión de configuración

Con 300 productos sintéticos adicionales, tres muestras calientes por listado y sin otros comprobadores enviando solicitudes, se observaron 9 SELECT por respuesta. Listado privado: 313 filas, unos 134 KB y 72–85 ms. Público: 308 filas, unos 119 KB y 54–74 ms. La medición no mostró crecimiento de consultas por cada producto en esos listados; se conservaron los EntityGraph existentes.

Son mediciones de loopback en esta computadora, no un ensayo de carga concurrente. Los contratos actuales devuelven listas completas; no se incorporó paginación nueva ni se certificó un volumen superior.

Un tercer proceso con la lista de catálogo vacía comprobó que una ficha nueva sin precio heredado permanece fuera del detalle y checkout anteriores, incluso con stock positivo. Los productos heredados válidos siguen disponibles. No se creó pedido, pago ni usuario al intentar la compra rechazada. No se ejecutó un binario antiguo contra el nuevo esquema.

## Validaciones finales

| Comprobación | Resultado | Evidencia |
| --- | --- | --- |
| Backend completo y JAR | 411 pruebas, 0 fallos/errores/omitidas | `final-backend.log`, `ejecucion-final.json` |
| TypeScript y Vite | Correctos; advertencia existente de tamaño del bundle | `final-typescript.log`, `final-vite.log` |
| Catálogo / transición / regresiones frontend | 31 / 34 / 11 correctas | Registros `final-*.log` |
| Integración catálogo Java/MySQL | 18 escenarios | `integracion-resultados.json` |
| Históricos Java/MySQL/proveedor local | 8 escenarios | `historicos-resultados.json` |
| Navegador con API real | 8 controles agrupados | `navegador-real.json`, capturas `integral-*.png` |
| Migración limpia y actualización | Flyway y JPA correctos en bases sintéticas | `esquema-verificado.txt`, `flyway-upgrade-*.log`, `jpa-upgrade.json` |
| Dump/restauración | 45 tablas y fixtures comparados | `migraciones-resultados.json` |
| Lint | 30 errores / 18 advertencias heredados, sin diferencias por archivo/regla/severidad | `comparacion-lint.json` |
| Reversión de configuración local | Ficha de catálogo excluida del checkout anterior | `reversion-local.json` |

La línea de base se ejecutó nuevamente en esta etapa: 408 backend, 31/34/11 frontend y compilaciones correctas. El inventario previo abarca 882 archivos. Los cambios anteriores no se descartaron. La última compilación y suite backend se repitieron después de completar la limpieza del perfil prod; la integración HTTP utiliza una configuración externa que no carga ese perfil. Las pruebas frontend no requirieron cambios de fuentes.

Los fallos iniciales del arnés se conservaron o describen en sus evidencias: relación cliente/tienda faltante en fixture; expectativa 201 donde la API de ficha devuelve 200; delimitador de trigger; finales de línea de MySQL en Windows; sesión persistida de Chrome; condición incorrecta al interceptar WhatsApp; reutilización de ID remoto al repetir el escenario histórico. Se corrigieron fixtures/arneses según los contratos observados. Los dos defectos de aplicación principales se corrigieron en código y volvieron a probarse.

## Límites y decisión de cierre

| Estado | Pendiente concreto | Impacto y responsable |
| --- | --- | --- |
| No ejecutado | Inventario de esquema/historial y configuración reales, especialmente versiones Flyway ausentes | Preparación del despliegue: operador y desarrollador deben reconciliarlo antes de migrar |
| No ejecutado | Google real y callback Mercado Pago en sandbox autorizado | Proveedores: responsable de cuentas y desarrollador deben verificar contratos, secretos y retornos efectivos |
| Decisión pendiente | Preferencias ambiguas, cobros tardíos, reembolsos y seguimiento de avisos | Operaciones históricas: definir responsable y procedimiento; no se automatizó una decisión comercial |
| No ejecutado | Proxy/HTTPS, almacenamiento entre instancias y respaldo real del VPS | Infraestructura: comprobar en preproducción; el rollback local no resuelve almacenamiento distribuido |
| Procedimiento definido | Archivos huérfanos por terminación abrupta | Operador: informe, referencias completas y cuarentena; no hay borrado automático |
| Preparación manual | Datos, contactos y textos de cada tienda real | Propietario/operador: completar explícitamente antes de habilitarla |
| Límite conocido | Listas completas, polling y deuda de lint | Desarrollo: capacidad verificada solo en el escenario documentado, no en cualquier volumen |

El cierre técnico local queda respaldado por las validaciones enumeradas y las correcciones aplicadas. Una activación futura sigue condicionada por el entorno real y la aceptación operativa, según `PLAN_ACTIVACION_CATALOGO.md`. No se afirma equivalencia con el compilado productivo ni autorización para desplegar.

Para repetir, consultar `artifacts/paso-6-catalogo/REPRODUCIR.md`. Los procesos propios se cierran con `cerrar.ps1`; las bases y archivos sintéticos quedan conservados para revisión. El cierre nativo del MySQL portable quedó esperando pese a no haber transacciones activas; se terminó su monitor y proceso hijo, verificando que pertenecieran exclusivamente a esta instancia desechable. El script ahora limita la espera y conserva ese recurso de cierre. La próxima apertura del directorio puede requerir recuperación normal de InnoDB; no se afirma un apagado limpio de esa instancia. `limpieza-final.json` confirma que no quedan los puertos locales de prueba ni Chrome propio activos. No se instaló un servicio global.
