# Galería institucional y contacto — 25/11/2026

Se reemplazó la imagen inicial y la grilla de fotos sueltas por una galería única: imagen principal de altura estable y proporción conservada, miniaturas seleccionables, anterior/siguiente, flechas de teclado y gesto horizontal. Sin reproducción automática. Título, comentario y contador acompañan a la imagen seleccionada. Una foto no muestra flechas/miniaturas; ninguna foto omite la galería. La selección vuelve al inicio al cambiar las imágenes de la tienda.

El contacto deja de ser una caja lateral. Se integra debajo de la presentación y galería, con separadores sutiles, iconos y tres columnas en desktop, una columna en móvil. Los datos, enlaces, fotos, orden y textos comerciales originales se conservan.

Fuentes: componente nuevo StorefrontAboutGallery, integración en StorefrontHomePage y estilos públicos. Manifest de 233 fuentes. Evidencia y scripts en `artifacts/galeria-institucional-20261125` y `/opt/mercado-digital/work/galeria-institucional-20261125`.

Validación: compilación TypeScript/Vite local y VPS; lint dirigido. Prueba con datos públicos reales de Infonel usando el frontend candidato aislado: 320, 390, 768 y 1366 px; miniaturas, siguiente, teclado, gesto táctil simulado, foto seleccionada, ausencia de overflow y contacto debajo. También cero/una foto. No hubo escrituras ni mensajes. El primer ensayo táctil falló porque faltaba identifier en el evento sintético; se corrigió el ensayo y pasó completo. El gesto físico en teléfonos no se certificó.

Release: `/var/www/mercado-digital-releases/galeria-institucional-20261125`. Anterior preservada: `/var/www/mercado-digital-releases/quienes-somos-20261125`. Publicación atómica con retención de assets previos y comprobación de hashes en cinco dominios. Backend, base de datos y Vidra fuera del cambio, sin reinicios. Reversión: cambiar atómicamente el enlace web a la release anterior previa comprobación de la activa.

Los resultados de regresión pública y publicación se conservan en navegador-resultados.json, galeria-resultados.json, resultado.json y publico-resultados.json. Vite mantiene la advertencia preexistente de tamaño de chunks.

Cierre: publicado y verificado en cinco dominios. Regresión de 32 vistas y 13 recorridos sin errores JavaScript ni infracciones detectadas por el chequeo automático. Las pruebas específicas de galería pasaron en cuatro tamaños. Los campos capturasPanel y recorridosPanel del publicador corresponden aquí al storefront. Backend, configuración y Vidra verificados sin cambios.
