# Composición del storefront — 25/11/2026

## Alcance

Revisión visual compartida por las tiendas, solicitada después de la primera publicación. Se conservan sus imágenes, colores, productos, precios, asesores y modos de funcionamiento. No hay cambios de backend, datos, migraciones, pagos ni configuración de Vidra.

## Cambios

- Cabecera: logo contenido de 176 px en desktop y 150 px en mobile; búsqueda visible. Con el logo de Mega, la cabecera mide 67 px en desktop y 107 px en mobile.
- Portada: una sola imagen completa, sin dividirla con una columna de texto. Se utiliza `object-fit: contain`, fondo difuminado de la misma imagen y transición inferior a blanco fuera del contenido principal del banner. El fondo nunca reemplaza ni recorta la imagen principal.
- Carrusel: controles manuales, indicador de posición, gesto horizontal y ampliación accesible. Sin avance automático. Títulos/subtítulos propios de cada diapositiva permanecen visibles sin superponer controles. Si falta la imagen, se utiliza el texto configurado; una imagen fallida tiene una alternativa textual.
- Promociones y novedades: vuelven como tiras comerciales independientes. Sólo incluyen productos con las marcas correspondientes. Cada tira muestra hasta 12 productos; “Ver todas” abre la colección completa mediante un filtro combinable con búsqueda, categoría y orden. Se conservan el foco y la posición al volver desde un producto. No se fabrican descuentos ni urgencia.
- Categorías: navegación visual con una fotografía de un producto de cada categoría; desaparecen las cápsulas de la portada. Las fotografías se obtienen de los productos de la propia tienda. El filtro de categoría sigue disponible en los resultados.
- La búsqueda, los filtros y las listas compartidas muestran resultados directamente, sin repetir la portada y las tiras.
- Ficha: proporción aproximada 58/42, galería de altura limitada, miniaturas laterales en desktop y horizontales en mobile. Imagen completa y ampliable. Título más moderado, categoría discreta, precio y acción agrupados. Descripción debajo.
- Entrega: fila con icono y detalle desplegable mediante mouse, tacto o teclado. Se conserva literalmente el texto comercial configurado.
- Contacto: se mantienen la validación del precio, el selector de asesores y WhatsApp. No se agregan pedidos, stock obligatorio ni pagos. Mobile conserva una única acción fija de compra; la aclaración explica que se elegirá un asesor.

## Ajuste adicional de cotización

La cotización se presenta siempre con dos decimales al cargar y guardar la configuración, en la ayuda del editor de producto y en la vista previa del precio. Se admiten coma o punto al ingresar un valor nuevo y hasta dos decimales; una entrada con mayor precisión se rechaza antes de solicitar la vista previa. El formato se normaliza al salir del campo. El redondeo de presentación utiliza aritmética decimal exacta.

Si una cotización histórica contiene más decimales significativos, se conserva su valor completo al guardar otros datos sin editar la cotización. No se modifica la precisión de la base de datos ni se recalculan precios por un simple cambio de presentación.

## Límites de contenido

La solución funciona con imágenes horizontales, cuadradas y verticales, pero una imagen vertical seguirá dejando más espacio lateral y un banner con letra pequeña seguirá siendo difícil de leer en un teléfono. La ampliación conserva acceso al original. Esta entrega no introduce campos nuevos de banner específico para mobile ni enlaces de campaña: el contrato actual de portadas contiene imagen, título y subtítulo, sin destino de categoría. Las categorías visuales sí filtran sus productos.

No se alteraron nombres en mayúsculas/emojis ni se retocaron fotografías de productos. La calidad y composición del material comercial siguen influyendo en el resultado. No se inventaron características técnicas ni información institucional.

## Validación y publicación

El cierre y los resultados finales se registran en `artifacts/storefront-composicion-20261125/`. Las pruebas de navegador usan el build real con respuestas públicas reproducidas y estados sintéticos, bloqueando escrituras. La verificación pública posterior también bloquea eventos y navegación externa de WhatsApp; no envía mensajes.

Trabajo remoto: `/opt/mercado-digital/work/storefront-composicion-20261125`.

Release publicada y verificada: `/var/www/mercado-digital-releases/storefront-composicion-20261125-r2`.

Release anterior conservada: `/var/www/mercado-digital-releases/storefront-composicion-20261125-r1`, con la composición visual previa al ajuste de cotización. La versión anterior a toda esta pasada también permanece en `/var/www/mercado-digital-releases/storefront-20261125-r1`. Se verifican hashes antes del cambio. La publicación usa un reemplazo atómico del enlace y conserva los chunks anteriores para pestañas abiertas. Ante fallo de verificación pública se revierte automáticamente el enlace.

Reversión preparada, no ejecutada:

```sh
python3 /opt/mercado-digital/work/storefront-composicion-20261125/revertir.py
```

La reversión verifica que esta sea la versión activa y no reinicia servicios ni restaura bases de datos. No debe usarse después de una publicación posterior sin revisar el estado.

Accesibilidad automática no equivale a certificación WCAG. Quedan fuera de esta comprobación los dispositivos físicos, Safari y lectores de pantalla reales.

## Resultado final verificado

- TypeScript/Vite: compilación correcta. ESLint de los componentes modificados: sin observaciones.
- 81 pruebas unitarias de catálogo, transición y navegación: aprobadas, incluidas cotización con dos decimales y preservación de precisión existente.
- Navegador sobre build real: 32 vistas/estados y 13 grupos de flujos; cero errores JavaScript y cero infracciones detectadas por axe en esas capturas.
- Verificación HTTPS posterior: 8 capturas, búsqueda, regreso a resultados, selector y enlace de Ayelén interceptado. Cero mensajes enviados. Los 144 productos públicos mantienen precios y datos; configuración de las cuatro tiendas comparadas sin cambios.
- 232 fuentes/configuraciones frontend verificados por SHA-256. 11 archivos estáticos anteriores retenidos para pestañas abiertas.
- JAR y entorno de MercadoDigital sin cambios. Ambos servicios mantienen su PID y estado. Vidra conserva sus hashes, configuración y respuesta HTTPS. No se reiniciaron servicios.
- La simulación táctil inicialmente falló por faltar el identificador obligatorio del evento de prueba; se corrigió el test y se repitió la comprobación completa. No fue un fallo del producto.
- La primera publicación visual se revirtió automáticamente al detectar una diferencia de precio contra una copia histórica (Samsung A56: 710000.00 histórico y 702571.50 actual). El despliegue no escribió datos. Se corrigió el control para tomar una lectura pública actual inmediatamente antes del cambio y contrastarla después; la nueva publicación pasó.
- La revisión r1 con la composición visual quedó publicada y luego se preparó r2 para incorporar el pedido adicional de cotización. Las 32 vistas de storefront corresponden a la revisión visual; r2 agrega la regresión del dashboard y repite la comprobación pública HTTPS.
- Dashboard sintético: 35 vistas y 18 grupos de flujos sin errores JavaScript. Se verifica 1200.00 al abrir cotización y 1300.00 después de guardar, conservando el contrato decimal del backend.
- Se revisaron visualmente capturas reales de catálogo y ficha en desktop/mobile. Las imágenes y nombres comerciales son los existentes, no material inventado para una maqueta.
