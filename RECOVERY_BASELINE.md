# Línea base de recuperación — 2026-08-28

## Artefactos funcionales recuperados

- Frontend observado en producción a las 14:33:
  - `index-BfbuwFfn.js`
  - SHA-256: `e2b917534b46e2429949dbf895c7685e28239098f9afcc3909aa8e27657a3edc`
  - Tamaño: `1.436.067` bytes
  - `index-BM7bJezy.css`
  - SHA-256: `ca9ac36f300b1e4f8a0d7bd808c6517439f2c0902bca22291a2ecd9fb8a455a4`
  - Tamaño: `90.835` bytes
- Los archivos se recuperaron de la respuesta HTTP conservada por Chromium y se
  verificaron en un servidor local aislado.
- La copia desplegable se conserva fuera del árbol fuente en
  `recovery/storefront-1433`.

## Estado del backend

- El JAR reemplazado durante el primer despliegue del día no tenía un backup
  recuperable. El archivo `/opt/mercado-digital/backups/app.jar.` ya correspondía
  a la base incompleta: devuelve 111 artículos para INFONEL.
- La base de datos productiva conserva las migraciones 34–45 y los datos de las
  funciones perdidas.
- La restauración de backend se reconstruye contra ese esquema y debe superar
  estas invariantes antes de publicarse:
  - INFONEL, sincronización pausada: 30 productos propios activos con stock.
  - Mega Reconquista, sincronización pausada: 20.
  - Navarro Ventas, sincronización pausada: 126.
  - Al reactivar sincronización, se agregan productos de proveedores sin ocultar
    los productos propios.

## Regla de despliegue

No desplegar `frontend/dist` generado desde `frontend/` hasta reconciliar el
fuente con el bundle recuperado. Para una restauración se usa únicamente el
paquete verificado de `recovery/storefront-1433`.
