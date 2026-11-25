# Limpieza de textos de Mi tienda — 25/11/2026

Publicado en las cinco tiendas mediante la release `limpieza-panel-20261125`.

Se revisaron Productos y su editor, Precios y envíos, Asesores y contacto, Difusión, Mi página y Métricas. Se eliminaron introducciones repetidas, explicaciones de implementación, aclaraciones técnicas de difusión, el bloque de revisión de textos comerciales y descripciones redundantes. Se conservaron etiquetas, valores, estados, errores, confirmaciones de impacto, protección de borradores y límites breves de formato/archivos. No se modificaron textos comerciales guardados por las tiendas.

Diez componentes modificados. Los demás fuentes coinciden por SHA-256 con el cierre de la extensión del catálogo. Fuentes y evidencia: `artifacts/limpieza-panel-20261125` y `/opt/mercado-digital/work/limpieza-panel-20261125`.

Validación: compilación TypeScript/Vite local y VPS; lint sin problemas en los componentes y dashboard revisados; 35 vistas y 18 recorridos de navegador con API/cuentas sintéticas, sin errores JavaScript. Inspección visual de Precios desktop y Mi página mobile. Los recorridos verifican edición, imágenes, borradores, revisión de dólar/envíos, asesores, difusión, métricas y selector WhatsApp. No se usaron cuentas reales para editar producción.

El lint global mantiene problemas preexistentes en otras áreas (24 errores y 18 advertencias en la ejecución local); no se declara que todo el repositorio esté libre de ellos. El primer build detectó un condicional JSX vacío al retirar un párrafo; se corrigió antes de compilar y validar la versión publicada. Vite mantiene la advertencia de tamaño de chunks.

Publicación atómica del enlace `/var/www/mercado-digital-web`, conservando assets anteriores para pestañas abiertas. Se verificaron por SHA-256 los archivos JS/CSS publicados en los cinco dominios. Backend, configuración, datos y servicios sin cambios; ningún reinicio. Vidra verificado sin alteraciones.

Release anterior preservada: `/var/www/mercado-digital-releases/storefront-composicion-20261125-r2`. La reversión de esta limpieza consiste únicamente en cambiar atómicamente el enlace web a esa release, previa comprobación de que la activa siga siendo `limpieza-panel-20261125`; no requiere revertir la migración de tiendas ni sus datos.
