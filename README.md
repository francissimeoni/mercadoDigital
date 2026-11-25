# MercadoDigital

Fuentes reconstruidos y actualizados del catálogo autónomo para tiendas de revendedores. Estado publicado al 25 de noviembre de 2026.

## Funcionalidad actual

- Catálogo público con búsqueda, categorías, promociones y novedades.
- Productos en ARS o USD; cotización y categorías de envío independientes por tienda. El precio público se expresa en pesos argentinos.
- Contacto y compra coordinados por WhatsApp mediante selección de responsable o asesor. El stock no condiciona la publicación.
- Panel Mi tienda: Productos, Precios y envíos, Asesores y contacto, Difusión, Mi página y Métricas.
- Presentación institucional con galería de imagen principal y miniaturas, adaptada a desktop y móvil.
- Mercado Pago retirado del flujo actual de MercadoDigital. Los datos históricos se conservan.

## Estructura

- `backend/`: Java 17, Spring Boot, Maven y MySQL 8. Migraciones Flyway hasta V58.
- `frontend/`: React, TypeScript y Vite. Dependencias fijadas en package-lock.json.
- `backend/scripts/`: herramientas explícitas para preparación y extensión del catálogo; no ejecutarlas automáticamente contra producción.
- `INFORME_*.md`, `PASO_*.md` y `docs/`: recuperación, decisiones, pruebas y publicaciones. Algunos informes mencionan evidencia privada que deliberadamente no está en Git.

## Desarrollo y compilación

Backend, desde `backend/`:

```sh
./mvnw test
./mvnw package
```

En Windows usar `mvnw.cmd`. Para ejecutar la aplicación, configurar una base local aislada y las variables de entorno; Flyway aplica migraciones al iniciar. `docker-compose.yml` contiene credenciales de demostración exclusivamente locales. Nunca usar esos valores ni el secreto JWT de desarrollo en producción.

Frontend, desde `frontend/` (Node.js 24 usado para las compilaciones verificadas):

```sh
npm ci
npm run build
npm run dev
```

Las variables `VITE_*` son públicas y se incorporan al bundle. No colocar secretos en ellas. Para una publicación con proxy del mismo origen, la API utiliza `/api` por defecto. Los archivos `.env.*.example` son referencias que deben adaptarse al entorno.

Pruebas del frontend:

```sh
npm run test:catalogo
npm run test:catalogo-transicion
npm run test:dashboard-mejoras
```

Las pruebas de navegador requieren Playwright y, según el script, fixtures o evidencia local adicional. No todos los scripts de auditoría son pruebas autónomas. El lint global conserva incidencias históricas documentadas; las áreas modificadas en las últimas publicaciones pasaron lint dirigido.

## Publicación y conservación

Este repositorio se inicia con una instantánea limpia de los fuentes actuales. No incluye el historial local de rescate porque contenía credenciales y notas operativas, ni incluye bases de datos, respaldos, archivos subidos por clientes, compilados o dependencias.

Subir a GitHub no despliega ni ejecuta migraciones. La release pública de referencia es `galeria-institucional-20261125`; el backend permanece en el compilado verificado en los informes. Vidra es una aplicación separada y no forma parte de este proyecto.
