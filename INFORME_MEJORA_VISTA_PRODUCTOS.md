> Actualización posterior: la release vigente es `storefront-20261125-r1`. Consultar [INFORME_MEJORA_STOREFRONT_PUBLICO.md](INFORME_MEJORA_STOREFRONT_PUBLICO.md); los procedimientos de reversión anteriores pertenecen a sus respectivas releases.

# Mejora de la vista Productos

> Actualización posterior del 25/11/2026: la release vigente es `mejoras-20261125-r1`, con guardado persistente, asesores, Difusión y Métricas. Ver [informe vigente](INFORME_MEJORAS_ASESORES_PRODUCTOS_DIFUSION_METRICAS.md). Este documento conserva la evidencia de la publicación anterior; su script de reversión no corresponde a la release actual.

Fecha: **25/11/2026**. Estado: **frontend publicado y comprobado**, con los límites de validación indicados al final.

## Resultado

Release activa: `/var/www/mercado-digital-releases/productos-20261125-r1`, mediante `/var/www/mercado-digital-web`. Anterior: `/var/www/mercado-digital-releases/dashboard-20261124-r1`.

Trabajo remoto: `/opt/mercado-digital/work/productos-20261125`. Se creó desde los fuentes anteriores sin modificarlos. Al inicio, los 253 archivos frontend del proyecto reconstruido local coincidían por SHA-256 con esos fuentes. Al finalizar, los 256 archivos coinciden entre el VPS y `C:\Sistemas\MercadoDigital-Reconstruido\frontend`. Los siete archivos de `frontend/dist` también coinciden con el release publicado; el compilado local anterior se conserva en `artifacts/productos-20261125/dist-local-anterior`.

No se modificaron el proyecto original ni el rescate. Esta intervención no cambia backend, esquema, migraciones, contratos HTTP, credenciales ni reglas comerciales. No hubo escrituras de negocio productivas ni reinicios. Los 144 productos públicos conservaron respuestas e importes idénticos desde el inicio hasta la publicación.

## Cambios y criterio visual

- **Listado y editor separados:** al editar desaparecen filtros, contadores, botón de alta y encabezados del listado. El editor muestra una sola cabecera con el producto y su estado guardado. La vuelta conserva búsqueda, filtros, página, scroll y foco en la fila correspondiente.
- **Acciones:** “Agregar producto” en el listado; “Crear producto” o “Guardar cambios” en el editor. Se retiró “Nuevo / descartar borrador”. La salida es “← Productos”, con la protección existente de borradores. Botón de guardado de ancho contenido en escritorio y ancho disponible en móvil. Barra final en el flujo normal: no tapa campos, errores ni contenido.
- **Tipografía y estilo:** Inter variable autoalojada, SIL Open Font License preservada. Archivo de 352240 bytes, obtenido de `https://rsms.me/inter/font-files/InterVariable.woff2`; licencia del repositorio oficial `rsms/inter`. Se definieron estilos reutilizables para campos, acciones, títulos, estados, paneles y espaciados. Acento verde sobrio, selección lateral tenue y acciones de cuenta discretas. La fuente se aplica al contenedor del dashboard; no se reemplazó la familia global del storefront público.
- **Distribución:** información, precio/envío, imágenes y publicación. Dos columnas cuando el espacio real del editor alcanza 900 px; una columna por debajo, mediante consultas de contenedor. El orden del DOM coincide con el recorrido visual en móvil y permite llegar al precio antes de las imágenes. No se añadió un asistente de pasos.
- **Imágenes:** miniaturas, portada, cambios de portada, eliminación en borrador, selección acumulativa de archivos, vista previa local y estado de imagen que no carga. El enlace queda como opción secundaria. Las URLs temporales se liberan al retirar archivos, guardar y desmontar. Una selección inválida conserva la selección válida anterior y ofrece cerrar el aviso.
- **Precio:** “Precio de venta”, moneda y envío agrupados; resultado ARS destacado y validado por backend. Para ARS con envío incluido se evita un desglose redundante. USD o envío adicional muestran los componentes relevantes. Se mantiene el ingreso decimal exacto existente: coma o punto decimal, sin agrupadores, sin reformatear mientras se escribe ni recalcular dinero con Number.
- **Publicación:** estado persistido en la cabecera y efecto propuesto al guardar en su sección. Actividad, preparación del precio y publicación efectiva se presentan por separado. Promoción y Novedad quedan como etiquetas secundarias. Stock sigue sin condicionar publicación o contacto.

El encabezado y la navegación compartidos reciben los ajustes de tipografía y jerarquía. No se rediseñó el contenido de las otras cinco secciones. El gestor/editor de catálogo es compartido con la preparación anterior; se conservó su funcionamiento y las advertencias de preparación. Las pantallas públicas y las operaciones históricas no fueron rediseñadas.

## Archivos

Nueve modificados:

```text
frontend/src/index.css
frontend/src/storefront/dashboard/CatalogDashboard.tsx
frontend/src/storefront/components/CatalogoProductManager.tsx
frontend/src/storefront/components/CatalogoProductEditor.tsx
frontend/tests/catalogo-ficha.test.mjs
frontend/tests/catalogo-transicion.test.mjs
frontend/tests/catalogo-transicion-preview.mjs
frontend/tests/catalogo-transicion-browser-check.mjs
frontend/tests/dashboard-browser-check.mjs
```

Tres nuevos:

```text
frontend/src/storefront/dashboard/productos.css
frontend/public/fonts/inter-latin.woff2
frontend/public/fonts/Inter-LICENSE.txt
```

No se agregaron dependencias ni se modificaron `package.json` o el lockfile. Los estilos se cargan desde el CSS principal y se limitan por clases del dashboard/editor. Los arneses sirven la fuente local y comprueban las etiquetas actuales. Se corrigió también el cierre del arnés CDP anterior: esperar la respuesta de Browser.close podía dejarlo detenido después de completar todas sus aserciones. Ahora envía el cierre y libera conexiones/procesos en finally.

## Validación ejecutada

Herramientas privadas ya existentes en el VPS: Node 24.15.0, Playwright 1.56.1 y Chromium, bajo `/opt/mercado-digital/tools`. Dependencias instaladas únicamente en el nuevo directorio de trabajo, sin modificar paquetes del sistema ni servicios.

| Comprobación | Resultado |
| --- | --- |
| `npm run build` | TypeScript y Vite correctos. |
| ESLint sobre los tres componentes TSX modificados | Sin errores ni advertencias. |
| `npm run test:catalogo` | 31/31. |
| `npm run test:catalogo-transicion` | 44/44. |
| `npm run test:dashboard-browser` | 35 vistas: seis secciones y editor en 360, 390, 768, 1366 y 1920 px; 15 grupos de casos; cero excepciones JS. |
| `npm run test:catalogo-transicion-browser` | 17 casos correctos; cero excepciones JS, solicitudes externas u operaciones nuevas de pedidos. Proceso finalizado correctamente. |
| Estáticos HTTPS | Los siete archivos publicados coinciden con sus hashes; incluye fuente y licencia. |
| Catálogo público real | 144 productos con las mismas respuestas y precios antes y después; idénticos también respecto del inicio de la tarea. |
| Selector público real | Responsable destacado y Ayelen Fernandez presentes en 390 y 1366 px; destino de Ayelén correcto, interceptado sin mensajes. |
| Acceso real sin sesión | `/mi-tienda` redirige a ingreso; API privada responde 403. |
| Aislamiento de tiendas | Mega conserva el piloto; Cleostore, Infonel y Navarro permanecen fuera. Mercado Pago sigue deshabilitado en esas respuestas de MercadoDigital. |

El navegador verificó además:

- Listado → página 2 → editor → volver conservando búsqueda, filtro activo, página, posición y foco.
- Un solo título y ausencia de acciones del listado en el editor.
- Inter cargada desde el servidor local del arnés; controles y documento sin desbordamiento horizontal.
- Recorrido de teclado y foco visible; reflow equivalente a 200 % a 683×384 y zoom CSS de 200 % a 1366×768. No se afirma haber operado el control nativo de zoom de un navegador físico.
- ARS y USD, categoría e incluido, resultado ARS y envío de token de revisión.
- Portada y retiro de imágenes existentes sin escritura independiente; enlace roto con explicación; previews de archivos y revocación de object URLs; envío multipart conjunto de ficha, precio y archivos.
- Archivo demasiado grande rechazado, selección posterior válida, errores de API y reintento.
- Borrador protegido al volver, cambiar de sección y usar Atrás; conflicto de guardado conserva el contenido.
- Configuración de dólar/envío y apariencia siguen preservando contratos y campos ocultos existentes.

Se inspeccionaron capturas del listado y editor en escritorio y móvil y se corrigió el orden móvil antes de la publicación final. Las escrituras de esas pruebas usaron exclusivamente transporte/API y sesiones sintéticas en loopback; no se modificaron artículos reales para probar.

## Publicación y conservación de servicios

Respaldo privado: `/opt/mercado-digital/backups/productos-20261125/frontend.tgz`, SHA-256:

```text
64ea02b1478bb5ede30a83f230067cbf81ede1a8ed4f53a2e997ab0211ce7be5
```

Se verificaron hash y lectura del tar. La publicación validó pruebas, manifiesto de fuentes, release anterior, respaldo, catálogo y servicios antes del cambio; luego cambió el enlace atómicamente y comprobó los estáticos y el catálogo. El script incluye reversión automática ante un fallo de esas comprobaciones. No fue necesaria.

Backend MercadoDigital: PID **25165**, SHA-256 sin cambios:

```text
6e8366e8d71e8e9cb44b95d89911e1def8c82b709dddb46aee35a4fcf261a2d5
```

El hash del entorno de MercadoDigital permaneció idéntico. Vidra mantuvo estado, PID, entorno, hashes de los 23 archivos registrados y comprobación HTTPS, según su verificador de solo lectura. No se tocaron Mercado Pago de Vidra, Nginx, Docker, MySQL, servicios compartidos ni credenciales. La comprobación pública interceptó POST de métricas para no escribir visitas de prueba.

Reversión preparada, **no ejecutada**:

```sh
python3 /opt/mercado-digital/work/productos-20261125/revertir-frontend.py
```

Verifica que siga activa `productos-20261125-r1` y restablece el enlace a `dashboard-20261124-r1`. No restaura datos ni reinicia servicios. Si hubo una publicación posterior, exige revisar el estado y se detiene.

## Evidencia y reproducción

Evidencia local: `artifacts/productos-20261125/`. Remota: `/opt/mercado-digital/work/productos-20261125/`.

- `evidencia/navegador-resultados.json`, `editor-{ancho}.png`, `productos-390.png`, `productos-1366.png` y capturas de las demás secciones.
- `evidencia/editor-reflow-200.png` y `editor-css-zoom-200.png`.
- `evidencia/publico-resultados.json` y capturas del selector público.
- `transicion-browser/verificacion-navegador.json` y solicitudes/capturas de regresión.
- `build.log`, `lint.log`, `catalogo.log`, `transicion.log`, `browser.log`, `transicion-browser.log`, `publico.log`.
- `fuentes-iniciales.json`, `fuentes-validadas.json`, `release-manifest.json`, `publicacion-resultados.json`.

Los archivos `fallo.*` conservan diagnósticos de iteraciones anteriores del arnés. Los resultados vigentes son los JSON y logs finales indicados. Los scripts de transformación en artifacts reflejan etapas de la implementación y no deben ejecutarse nuevamente sobre los fuentes finales.

Para repetir compilación, lint y pruebas del dashboard en el entorno privado:

```sh
sh /opt/mercado-digital/work/productos-20261125/verificar.sh
sh /opt/mercado-digital/work/productos-20261125/regresion.sh
```

Estos scripts validan y no publican. `publicar.py` pertenece a esta operación ya completada y bloquea sobrescribir la release existente.

## Límites y pendientes

1. **No se inició sesión con una cuenta real del propietario en producción.** Los componentes privados se comprobaron con sesión y API sintéticas; la comprobación pública real cubrió estáticos, catálogo, contactos y protección sin sesión. Falta la revisión del propietario autenticado.
2. Guardados, cargas de imágenes y conflictos del navegador se probaron con API simulada. No equivalen a una nueva prueba de integración Java/MySQL o de almacenamiento productivo. El backend no cambió y no se repitió su suite.
3. El contrato actual agrega archivos nuevos después de las imágenes por URL ya existentes. Si hay imágenes existentes, una foto recién subida puede elegirse como portada **después de guardar**. La interfaz lo explica; no se inventó un orden de medios que el backend no admite. Elegir portada existente o entre archivos nuevos cuando no hay imágenes existentes sí está disponible antes de guardar.
4. Se mantiene el formato monetario exacto previo; no se admiten agrupadores ambiguos como `1.200` para interpretar miles ni se reformatea al escribir. El resultado público sí se muestra con formato argentino.
5. Las pruebas móviles y de ampliación son emuladas en Chromium. No se afirma certificación en dispositivos físicos, Safari/iOS, teclado virtual ni una cuenta real de WhatsApp. No se enviaron mensajes.
6. Vite mantiene la advertencia previa de bundle superior a 500 kB. ESLint limpio corresponde al alcance indicado, no a todo el repositorio. No se efectuó una refactorización general ni una actualización de dependencias.
7. El respaldo se verificó por lectura y hash; no se ejecutó una reversión real ni se restauró una base, porque esta publicación solo cambia archivos de frontend.

Para revisar: abrir `/mi-tienda?tab=productos`, recargar con Ctrl+F5 si fuera necesario, buscar un artículo y entrar en Editar. En móvil el precio aparece después de la información y antes de las imágenes. Volver mediante “← Productos” recupera el contexto del listado. Las demás secciones quedan disponibles para sus siguientes revisiones específicas.
