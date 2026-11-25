# Paso 4: operaciones históricas de pedidos y cobros

Fecha: 24/11/2026. Trabajo local en el proyecto reconstruido. El modo catálogo sigue desactivado por defecto; no se activaron tiendas, ejecutaron migraciones ni usaron credenciales o servicios productivos. El proyecto antiguo y el rescate permanecen intactos. Esta etapa corrige riesgos del circuito anterior; no vuelve a habilitar compras nuevas para tiendas en modo catálogo.

El [inventario SHA-256 de 42 archivos](artifacts/paso-4-catalogo/archivos-paso-4.json) identifica los fuentes, pruebas y documentos afectados. Se tomó una línea base propia antes de los cambios (`git-inicial.txt`, `baseline-sha256.json`); no se limpiaron ni descartaron cambios previos del árbol de trabajo.

## Estado y evidencia

**Implementado y verificado con pruebas locales:** se impide confirmar transferencias de pedidos cancelados; se valida propiedad de tienda en la lectura, cancelación y creación operativa de pedidos; se protege el endpoint PSP anterior por token y host; se bloquea el webhook genérico sin autenticidad; se verifica la firma de Mercado Pago y luego se consulta el pago al proveedor antes de mutar el pedido; se endurecen comprobantes y estados de las pantallas anteriores. Las pruebas y el navegador emplearon datos sintéticos y dependencias simuladas.

**Validación parcial:** el bloqueo pesimista de pedido y stock está implementado, pero no se probó contra MySQL ni con dos instancias del servidor. Las preferencias PSP se reutilizan cuando su URL quedó persistida; un timeout después de que el proveedor pudo aceptar una preferencia continúa siendo ambiguo. No se promete idempotencia distribuida en ese caso. Los correos internos del webhook se programan después del commit, pero el ejecutor asíncrono no es una cola durable ni garantiza entrega exactamente una vez.

**Pendiente de decisión de negocio:** cómo conciliar un cobro confirmado después de cancelar localmente, y cómo registrar/ejecutar un reembolso real. `RmaService` puede registrar resolución `REEMBOLSADO`, pero esa rama no llama al PSP ni demuestra devolución del dinero; por ello no se agregó una devolución automática. Se conserva el hecho financiero en el webhook y se audita `COBRO_TARDIO_CONCILIACION`, sin reabrir pedido, envío ni stock.

**Bloqueos para futuro despliegue:** contrato efectivo del callback de la cuenta Mercado Pago que se utilizará, secreto de firma configurado y prueba aislada de ese callback; base MySQL desechable para Flyway, JPA y concurrencia; resolución operativa de preferencias ambiguas, cobros tardíos y reembolsos. La compilación no acredita equivalencia con producción ni autorización para desplegar.

## Problemas encontrados y correcciones

1. La implementación heredada permitía confirmar una transferencia tras cancelar el pedido y reponer stock. `TransferenciaService` ahora bloquea el pedido, revalida `CREADO + PENDIENTE + TRANSFERENCIA` antes de cualquier efecto y exige comprobante. Una confirmación ya efectuada conserva su estado y no repite evento ni notificación. La antigua prueba de caracterización se reemplazó por una regresión del comportamiento seguro. Se rechazó también la carga de un comprobante tras la cancelación.
2. `POST /public/pedido/{pedidoId}/pago` aceptaba un UUID sin capacidad. Ahora exige `trackingToken` y `storefrontHost`, verifica que el token corresponda a ese pedido público y que el host pertenezca a la tienda. El cliente de checkout anterior envía ambos. La ruta de reanudación explícita por token se conserva. Las respuestas públicas de seguimiento no se almacenan en caché y los errores ya no incluyen el token completo.
3. `POST /webhooks/psp` aceptaba un estado arbitrario sin firma. La ruta genérica devuelve 404 y queda denegada por seguridad. La única entrada pública permitida es `/webhooks/psp/mercadopago`: requiere `x-signature`, `x-request-id`, `data.id` y `MERCADOPAGO_WEBHOOK_SECRET` configurado; verifica HMAC-SHA256 del manifiesto documentado; compara el ID notificado; consulta el pago con un token de cuenta resuelto por servidor; comprueba ID, moneda ARS, cuenta receptora, pedido, método e importes. No se confirma por la URL de retorno. Sin secreto, el callback rechaza con 401.
4. El webhook usa un identificador que distingue estados sucesivos del mismo pago y guarda la recepción antes de los efectos. Eventos repetidos no vuelven a confirmar ni a notificar. Un rechazo antiguo no revierte un pago confirmado. Un reembolso posterior del mismo pago se conserva; uno de un pago distinto se audita sin alterar el cobro confirmado. Un cobro posterior a la cancelación se audita para conciliación sin reactivar la operación. El pedido y el stock usan bloqueo de repositorio en las transiciones conflictivas.
5. La carga de comprobantes revalida token, método, estado y pedido bajo bloqueo; verifica tamaño y firma real de PDF, PNG, JPEG o WEBP además del tipo declarado. La lectura verifica pertenencia de tienda y ruta real dentro del directorio de comprobantes. La descarga usa `attachment` y `nosniff`. La escritura nueva se elimina si la transacción revierte. No se registran contenidos ni tokens en los artefactos.
6. El retorno y seguimiento clasifican `PENDIENTE`, `CONFIRMADO`, `APROBADO`, `RECHAZADO`, `REEMBOLSADO` y `CANCELADO` a partir de la consulta al servidor. Solo `CONFIRMADO` junto con pedido pagado anuncia éxito. El panel no ofrece confirmar una transferencia cancelada. Se conserva la referencia al pedido ante errores, sin crear compras o reintentar cobros al montar la pantalla.
7. La lectura y cancelación operativa por `/pedido/{id}` tenían un hueco entre tiendas: el rol `REVENDEDOR` bastaba para operar por UUID. Se incorporó `RevendedorOwnershipService` en el servicio para lectura, cancelación y las dos entradas operativas de creación. El administrador mantiene su alcance explícito. La cancelación sigue exigiendo pago no confirmado y repone stock una vez.
8. Los correos internos asociados al webhook podían salir antes del commit. El webhook publica un evento específico; su listener lo atiende después del commit y vuelve a leer el pedido persistido. Las notificaciones en base y el evento de postcompra existente permanecen ligados a una única transición. El resto de envíos y la entrega asíncrona siguen requiriendo revisión operativa; no hay outbox.

El protocolo de firma y la consulta del pago se contrastaron con la [documentación oficial de webhooks de Mercado Pago](https://www.mercadopago.com.ar/developers/es/docs/checkout-bricks/additional-content/your-integrations/notifications/webhooks) y la [descripción del manifiesto firmado](https://www.mercadopago.com.ar/developers/en/docs/checkout-pro-preferences/additional-settings/optional-notifications). La [API oficial de creación de preferencias](https://www.mercadopago.com.ar/developers/en/reference/online-payments/checkout-pro-preferences/create-preference/post) no documenta en ese contrato la misma clave de idempotencia de la API Orders; no se atribuyó esa garantía a las preferencias.

## Matriz de estados y efectos

| Operación | Estado admitido | Transición y efectos | Repetición o conflicto |
| --- | --- | --- | --- |
| Confirmar transferencia | Pedido `CREADO`, pago `PENDIENTE` por transferencia, comprobante cargado | Pedido `PAGADO`, pago `CONFIRMADO`, envío pasa de espera de pago a preparación, evento de pago | Si ya está confirmado, no repite cobro ni evento; pedido cancelado o método ajeno: rechazo sin efecto |
| Cargar comprobante | Pedido `CREADO`, pago `PENDIENTE` por transferencia, token válido | Guarda archivo y referencia; emite evento tras commit | Archivo duplicado o pedido cancelado: rechazo; archivo nuevo se elimina al revertir transacción |
| Cancelar impago | Pedido `CREADO`, pago pendiente/rechazado | Pedido, pago y envío cancelados; stock histórico restituido bajo bloqueo | Segunda cancelación: rechazo sin nueva restitución; pago confirmado: rechazo |
| Preparar/reanudar PSP | Pedido público `CREADO`, pago PSP `PENDIENTE`, token y host válidos | Crea preferencia solamente si no hay URL persistida | Reutiliza URL persistida; timeout antes de persistir sigue ambiguo y requiere conciliación |
| Webhook aprobado | Firma válida, pago remoto ARS de la cuenta correspondiente, pedido `CREADO`, pago PSP pendiente/rechazado, montos iguales | Pago `CONFIRMADO`, pedido `PAGADO`, envío preparado, auditoría y eventos | Mismo evento o pedido pagado: sin duplicar efectos; otro pago: auditoría de conciliación |
| Webhook rechazado/cancelado | Pago PSP del pedido, importes iguales | Pago `RECHAZADO`, pedido conserva estado; aviso al revendedor | No revierte pago confirmado/reembolsado; estado igual no notifica otra vez |
| Webhook reembolsado/contracargo | Pago PSP del pedido, importes iguales; si ya hay pago confirmado, debe coincidir su referencia | Pago `REEMBOLSADO`; pedido no se recalcula ni se reabre | Evento repetido sin efectos; referencia distinta se audita y no modifica el cobro confirmado |
| Cobro tras cancelar | Pedido `CANCELADO` | Registra webhook y `COBRO_TARDIO_CONCILIACION` | No reabre pedido, envío ni stock; devolución/entrega requieren decisión humana |
| RMA con resolución reembolsado | Etapa de RMA habilitada por el flujo previo | Marca el RMA y audita su resolución | No acredita por sí misma devolución PSP; conciliación financiera pendiente |

Los estados de pedido, pago y envío se comprueban por separado. `APROBADO` no se equipara automáticamente a `CONFIRMADO` en la interfaz; `REEMBOLSADO` tampoco significa que un RMA haya ejecutado dinero saliente.

## Matriz de autorización y compatibilidad

| Ruta / acción | Actor y prueba de alcance | Cambio de contrato |
| --- | --- | --- |
| `GET /pedido/{id}`, `POST /pedido/{id}/cancelar` | JWT de revendedor dueño o administrador; propiedad comprobada en el servicio | El UUID ajeno ya no da lectura ni cancelación |
| `POST /pedido`, `POST /pedido/canonico` | JWT operativo y propiedad de la tienda objetivo; el modo catálogo sigue bloqueando compras nuevas | Revendedor de otra tienda ya no puede crear el pedido operativo |
| `POST /public/pedido/{pedidoId}/pago` | Token de seguimiento del pedido público + host equivalente a la tienda | Consumidores antiguos que solo envíen UUID deben actualizarse; el frontend local ya lo hace |
| `POST /public/pedido/{trackingToken}/reanudar-pago` y seguimiento | Token de capacidad + host de tienda; origen público y estado revisados en servidor | Enlaces de seguimiento vigentes conservados; no se crea otro pedido |
| `POST /public/pedido/transferencia/{token}/comprobante` | Token de transferencia y pedido pendiente de ese método; tipo real y ruta seguros | Token de transferencia no habilita confirmar cobro |
| `GET /revendedores/{revendedorId}/pedidos/{pedidoId}/comprobante` y confirmación | JWT del dueño/administrador, propiedad de tienda/pedido, método y estado en servicio | Archivos ajenos no se sirven; el panel oculta acciones inválidas |
| `POST /webhooks/psp/mercadopago` | Firma HMAC, secreto, ID de consulta, cuenta receptora y pago remoto verificados | Requiere configurar secreto y callback compatible antes de cualquier operación real |
| `POST /webhooks/psp` | Denegado | Un callback antiguo dirigido aquí deja de procesarse; requiere inventario y reconfiguración controlada |
| RMA | Rol administrativo y reglas del flujo RMA | Su resolución `REEMBOLSADO` no es confirmación financiera |

## Validación efectivamente ejecutada

| Tipo | Resultado | Evidencia y límite |
| --- | --- | --- |
| Backend: empaquetado y suite completa | **383 pruebas correctas, JAR generado** | `backend-package-final-paso4.log`; JUnit/MockMvc y dependencias simuladas, sin MySQL |
| Frontend: TypeScript + Vite | Correcto | `frontend-build-ultima.log`; advertencia heredada por tamaño del bundle |
| Catálogo y transición DOM | 23 y 34 casos correctos | `frontend-catalogo-cierre.log`, `frontend-transicion-ultima.log` |
| Regresiones de precios/tracking | 11 casos correctos | `frontend-regresiones-final.log`; el comparador conserva pasos logísticos y ajusta únicamente el nuevo texto de pago pendiente |
| Chrome local | Seis estados de retorno correctos, sin errores JS ni llamadas externas; escritorio 1280 px y móvil 390 px sin desborde | `verificacion-navegador.json`, `retorno-reembolsado-desktop.png`, `retorno-rechazado-mobile.png`; API sintética, no Java/MySQL/PSP real |
| Lint | 30 errores y 18 advertencias, mismo total y reglas heredadas | `comparacion-lint.json` y `lint-ultima.json`: ninguna diferencia por archivo, regla y severidad respecto del paso 3; no se declara limpio |
| MySQL y PSP externo | No ejecutados | No hay MySQL/MariaDB, Docker/Podman local disponible ni autorización para usar servicios productivos |

Las pruebas de concurrencia con mocks comprueban el orden de las operaciones y el rechazo de estados en servicio; no demuestran serialización entre procesos. Para validarla, preparar MySQL desechable con datos sintéticos, aplicar migraciones hasta V56, iniciar dos sesiones sobre un pedido, coordinar cancelación/confirmación y verificar exactamente un estado final, una restitución de stock y una auditoría coherente. Repetir para dos preparaciones PSP y dos webhooks del mismo pago. La consulta y autenticación reales del callback también requieren un sandbox del proveedor con secreto propio. No se aplicaron migraciones nuevas en este paso.

La primera pasada de cierre (`backend-package-cierre-definitivo.log`) tuvo 381 pruebas y un fallo de fixture: `CatalogoCompraBloqueoTest` no inyectaba el nuevo validador de propiedad y lanzaba `NullPointerException` antes de comprobar el bloqueo de catálogo. Se agregó el mock sin reducir la aserción; `backend-regresion-cierre.log` confirma los casos afectados. Se añadieron además dos regresiones de conciliación antes de la última suite completa.

## Riesgos y próximos pasos

| Riesgo | Corrección | Prueba | Límite restante |
| --- | --- | --- | --- |
| Transferencia cancelada confirmada | Validación transaccional y bloqueo | `TransferenciaRecoveryTest` | Falta MySQL concurrente |
| Lectura/cancelación entre tiendas | Ownership en servicio | `PedidoServiceCanonicoTest` | Revisar otras superficies operativas en integración completa |
| UUID PSP y callback falsificable | Token/host, firma y pago remoto | Controladores, seguridad, verificador y webhook | Configuración real del callback y secreto |
| Reembolso de pago distinto | Correlación por referencia | `WebhookPspServiceTest` | Reconciliación financiera real |
| Doble efecto o evento tardío | Evento único, locks y auditoría | Pruebas de servicio y adapter | Falta concurrencia MySQL y resolución del cobro tardío |
| Correo previo al commit | Listener posterior al commit | Suite backend y revisión de flujo | Sin outbox durable; falla del ejecutor puede perder aviso |
| Estado visual falso | Clasificación explícita del servidor | 34 DOM + seis Chrome | Falta navegador conectado a Java y base reales |

**Paso 5:** unificar formularios y datos de productos, contactos, cotización, envío y textos de cada tienda; el cambio aquí no modifica esos modelos. **Paso 6:** validar migraciones, JPA, bloqueo y recorridos navegador/Java/MySQL en entorno descartable; probar roles y sesiones locales, callback sandbox y plan de conciliación operativa. La activación por tienda permanece apagada hasta completar esas decisiones y validaciones.
