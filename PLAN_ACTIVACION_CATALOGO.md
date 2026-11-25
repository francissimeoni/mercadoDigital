# Activación futura del catálogo por tienda de revendedor

Este procedimiento está preparado para revisión. No autoriza ni registra un despliegue. Las pruebas del paso 6 utilizaron exclusivamente procesos locales, bases desechables y datos sintéticos.

## Condiciones previas

1. Seleccionar una versión conjunta de backend, frontend y migraciones. Registrar hashes del JAR, frontend y SQL. Los decimales administrativos se entregan como cadenas; los consumidores privados deben admitirlos. El backend requiere V57 y las migraciones anteriores disponibles, más cualquier evolución posterior explícita.
2. Inventariar el esquema y el historial Flyway del entorno de destino mediante un procedimiento autorizado. Los archivos disponibles saltan de V33 a V46. Los comentarios de V46/V47 mencionan versiones aplicadas cuyos fuentes se perdieron. Las pruebas locales NO reconstruyen el historial efectivo del VPS. Si hay versiones aplicadas sin archivo o checksums diferentes, detener la preparación y resolver la trazabilidad. No ejecutar `repair`, modificar checksums ni ignorar migraciones faltantes indiscriminadamente.
3. Confirmar versión efectiva de MySQL, modo SQL, collation y configuración de aislamiento. Se probó MySQL 8.0.44 en Windows; no se verificó el motor/configuración actuales del VPS. Las escrituras de catálogo utilizan READ_COMMITTED y bloqueo por storefront; la garantía requiere que todas las instancias ejecuten una versión compatible.
4. Retirar del circuito cualquier credencial histórica incrustada. Configurar secretos mediante el mecanismo operativo elegido, sin valores reales en Git, frontend, informes o línea de comandos. La limpieza del archivo actual no elimina copias históricas ni sustituye la revisión/renovación de credenciales por el responsable.
5. Disponer de una copia consistente de base, archivos y configuración, conservando sus permisos. Verificar restauración en un entorno aparte y acordar objetivos de recuperación. El ensayo del paso 6 restauró una base sintética de 45 tablas; no certifica un respaldo del VPS ni la recuperación de sus archivos.
6. Validar en preproducción el proxy, HTTPS, host público, rutas `/api`, archivos, límites multipart, callback OAuth y almacenamiento compartido si hay varias instancias. Backend: 5 MB por archivo y 26 MB por solicitud. El proxy real no se probó en esta etapa.

## Preparación por tienda

- Confirmar propietario y personas autorizadas. Ser asesor de contacto no concede administración.
- Completar explícitamente moneda y precio original de cada artículo, envío incluido o categoría propia. Para Mega Reconquista se confirmó posteriormente que todos los precios anteriores eran ARS con envío incluido: usar la preparación revisable de `PILOTO_MEGA_RECONQUISTA.md` solamente para configuraciones faltantes de esa tienda. Respetar las configuraciones existentes y dejar valores inválidos pendientes.
- Configurar cotización manual positiva para productos USD. Los precios ARS no dependen de ella.
- Crear categorías de envío de ESA tienda. Reasignar productos antes de eliminar una categoría utilizada.
- Revisar los impactos de cotización/costos y confirmar valores vigentes. Producto configurado, activo y tienda habilitada son estados distintos; el stock no interviene en el catálogo.
- Comprobar teléfonos y disponibilidad de contactos. Un formato válido no acredita una cuenta WhatsApp activa. Destacar al titular y revisar asesores habilitados de la misma tienda.
- Revisar manualmente portada, pie, condiciones de entrega y otros textos personalizados. No prometer compra confirmada, seguro, logística centralizada ni envío gratuito por estar incluido en el precio.
- Inventariar pedidos, transferencias, pagos y reclamos anteriores, con responsable de seguimiento.

## Circuito histórico y proveedores

El catálogo nuevo no genera pagos ni pedidos. Las operaciones anteriores siguen requiriendo atención. Antes de decidir una activación, determinar qué superficies históricas permanecen operativas y qué pendientes las afectan.

La firma y correlación del webhook se probaron con un proveedor HTTP local. Falta el callback efectivo y secreto de la cuenta de Mercado Pago en sandbox autorizado. También falta autenticación real contra Google con los dominios y retornos definitivos. No usar credenciales reales para resolver esos pendientes mediante pruebas locales.

Procedimiento de conciliación:

| Caso | Información que debe conservarse | Acción del responsable |
| --- | --- | --- |
| Preferencia aceptada durante timeout | Pedido, cuenta, hora, importe, referencia y respuesta disponible | Consultar el estado del proveedor y conciliar antes de autorizar otro intento; no asumir que el timeout cancela la preferencia |
| Cobro posterior a cancelar | Pedido cancelado, pago remoto, importe, cuenta y auditoría `COBRO_TARDIO_CONCILIACION` | Registrar la decisión de devolución o cumplimiento por el circuito autorizado; no reabrir pedido/stock automáticamente |
| Cobro duplicado | Referencias de ambos pagos y pedido asociado | Correlacionar con el proveedor y resolver el excedente con autorización trazable |
| RMA `REEMBOLSADO` | Resolución del reclamo y evidencia financiera independiente | Confirmar si se realizó una devolución efectiva; el estado del RMA no la demuestra |
| Correo omitido o perdido | Estado persistido, auditoría y destinatario autorizado | Usar consulta administrativa y un seguimiento operativo; no prometer entrega garantizada ni exactamente una vez |

Las notificaciones utilizan ejecución asíncrona sin outbox durable. El paso 6 verificó rollback de efectos y listeners posteriores al commit con transporte local; no verificó entrega SMTP externa. No ampliar este procedimiento con decisiones de devolución automática aún no aprobadas.

## Secuencia de activación a ejecutar posteriormente

1. Acordar responsables, ventana, criterios de aceptación y suspensión. Congelar escrituras durante el cambio si la compatibilidad entre versiones no está demostrada.
2. Obtener y verificar el respaldo anterior al cambio. Confirmar el procedimiento de restauración.
3. Aplicar el paquete de migraciones validado para el historial real. Desplegar el backend compatible con la bandera sin nuevas tiendas habilitadas y luego el frontend correspondiente. La fase de mantenimiento debe impedir que binarios anteriores gestionen fichas nuevas con campos heredados nulos.
4. Verificar autenticación, permisos, esquema, archivos y operaciones históricas. Preparar los datos de una tienda piloto.
5. Agregar únicamente esa tienda al mecanismo existente `CATALOGO_PUBLICO_REVENDEDORES`. Comprobar el mecanismo efectivo de actualización de configuración en todas las instancias; no asumir recarga dinámica.
6. Probar como visitante y propietario: precio ARS, carga/edición, categoría propia, incremento, cotización, stock cero/nulo, selector y enlace WhatsApp. Comprobar accesos directos antiguos y ausencia de pedidos/pagos nuevos por contacto.
7. Revisar errores, bloqueos, latencia, auditoría y consistencia de precios. Ampliar por tiendas solo con aceptación documentada.

## Suspensión y reversión

Suspender la ampliación ante acceso entre tiendas, importes incorrectos, escrituras parciales, duplicados, errores de migración o afectación de operaciones históricas. El método de cierre temporal debe estar disponible en la infraestructura operativa; no se implementó una nueva bandera de mantenimiento en esta etapa.

- **Configuración:** quitar una tienda de la lista vuelve al circuito anterior. No completa precio, paquete ni stock de los productos nuevos. Se comprobó localmente que esas fichas siguen excluidas del detalle y checkout anteriores, incluso con stock positivo. Esa protección no convierte la tienda completa en apta para vender por checkout.
- **Aplicación:** preferir una corrección sobre esquema compatible. Volver a un binario anterior requiere comprobar que tolera los campos nulos, el versionado y los contratos actuales. No se ensayó la ejecución de un binario antiguo contra V57 y no se considera una reversión segura por defecto.
- **Datos:** no bajar migraciones ni eliminar columnas como respuesta inmediata. Una restauración requiere detener escrituras, evaluar los cambios posteriores al respaldo y conciliar pagos/operaciones externas que no desaparecen al restaurar una base. Preservar evidencia antes de actuar.

## Archivos tras una terminación abrupta

El rollback normal elimina archivos nuevos; un cierre abrupto puede dejar huérfanos. Inventariar periódicamente el directorio efectivo y referencias de imágenes, portadas, logos, galerías y otros consumidores. Clasificar referenciados, compartidos, recientes y candidatos sin referencia. Las URLs o JSON personalizados también pueden contener referencias.

Empezar con un informe sin borrado. Durante una ventana sin escrituras, respaldar y trasladar candidatos antiguos a una cuarentena propia del almacenamiento, con rutas absolutas verificadas y período de retención acordado. Confirmar que no existen referencias antes de cualquier eliminación posterior. Una antigüedad arbitraria o la ausencia en una sola tabla no autorizan borrar un archivo.

## Decisión pendiente

La aceptación técnica local del paso 6 no acredita equivalencia con los compilados de producción. La autorización para desplegar depende del historial/esquema reales, datos de cada tienda, secretos, proveedores, proxy y aceptación del tratamiento de operaciones históricas. No se activó ninguna tienda real.
