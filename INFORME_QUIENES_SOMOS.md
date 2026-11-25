# Presentación institucional y textos públicos — 25/11/2026

Se adaptó Quiénes somos a la tipografía, colores y espaciado del catálogo. Presentación con historia/fotos reales y contacto lateral en desktop; una columna en móvil. Cuando faltan historia y fotos, cabecera compacta y contactos en fila en desktop. Se omiten secciones sin datos. Las galerías y redes sociales conservan los datos propios de cada tienda.

Se retiraron Cómo comprar, la presentación genérica automática, las instrucciones automáticas del footer, la explicación de filtros de selecciones compartidas y la indicación repetida bajo Comprar por WhatsApp. El selector conserva una indicación breve de que abre WhatsApp y su descripción accesible. No se alteraron textos comerciales guardados, precios, contactos ni datos.

Fuentes y evidencia en `artifacts/quienes-somos-20261125` y `/opt/mercado-digital/work/quienes-somos-20261125`. Se modificaron seis archivos. Compilación TypeScript/Vite, lint dirigido, recorrido de catálogo/ficha/presentación con comprobaciones de accesibilidad y pruebas públicas en los cinco dominios. Resultados detallados en los JSON de evidencia; no se enviaron mensajes ni se modificaron datos durante las comprobaciones.

Release prevista: `/var/www/mercado-digital-releases/quienes-somos-20261125`. Anterior: `/var/www/mercado-digital-releases/limpieza-panel-20261125`. Publicación mediante cambio atómico del enlace web y conservación de chunks previos. El script verifica SHA-256 de fuentes y archivos publicados, backend/configuración y estado de MercadoDigital/Vidra. No requiere reinicios ni migraciones. Reversión: volver atómicamente a la release anterior, comprobando primero la release activa.

Limitaciones: el contenido institucional depende de lo cargado por cada tienda. No se inventaron historias, fotos, garantías ni cobertura. Vite conserva su advertencia de tamaño de bundles.

Cierre: publicada y verificada. La versión final pasó 32 vistas y 13 recorridos, sin errores JavaScript ni infracciones detectadas por el chequeo automático de accesibilidad. Se verificaron catálogo y presentación en los cinco dominios, con diez capturas de presentación desktop/mobile. Backend y Vidra sin cambios. Los campos capturasPanel/recorridosPanel del resultado reutilizan el nombre del publicador anterior: en esta entrega corresponden al storefront público.
