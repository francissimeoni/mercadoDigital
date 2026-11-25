# Acta MT5 - Administracion de revendedores y tiendas

## Objetivo

Implementar la etapa MT5 de multitenant v1 para que `ADMIN_MD` pueda gestionar revendedores y su storefront desde backend, sin depender de carga manual en base y sin tocar la superficie publica ni los flujos operativos existentes.

## Endpoints admin agregados

- `GET /admin/revendedores`
- `GET /admin/revendedores/{id}`
- `POST /admin/revendedores`
- `PUT /admin/revendedores/{id}`
- `PUT /admin/revendedores/{id}/storefront`

## DTOs request/response creados

Requests:

- `AdminRevendedorRequest`
- `AdminStorefrontRequest`

Responses:

- `AdminRevendedorResponse`
- `AdminStorefrontResponse`

## Validaciones adoptadas

- `nombre`, `dominio` y `activo` son obligatorios para crear/editar revendedor.
- `nombreComercial` y `publicada` son obligatorios para alta/edicion de storefront.
- `dominio` se normaliza:
  - trim
  - lowercase
  - remocion de `http://` o `https://` si vienen por error
  - remocion de path
  - remocion de puerto
  - remocion de punto final
- `dominio` debe tener formato de host valido y al menos un punto.
- si ya existe otro revendedor con el mismo dominio normalizado, se responde `409 Conflict`.
- no se permite publicar storefront para un revendedor inactivo.
- no se permite desactivar un revendedor si su storefront actual esta publicada.

## Politica de creacion y actualizacion de storefront

- Crear revendedor **no** crea storefront automaticamente.
- El storefront puede quedar ausente inicialmente.
- `PUT /admin/revendedores/{id}/storefront` funciona como **create-or-update**:
  - si no existe storefront, la crea
  - si existe, la actualiza

Esta decision evita endpoints extra y mantiene una API admin simple.

## Campos devueltos en detalle admin

`AdminRevendedorResponse` devuelve:

- `id`
- `nombre`
- `dominio`
- `activo`
- `createdAt`
- `updatedAt`
- `storefront`

`storefront`, cuando existe, devuelve:

- `id`
- `nombreComercial`
- `logoUrl`
- `faviconUrl`
- `colorPrimario`
- `colorSecundario`
- `heroTitulo`
- `heroSubtitulo`
- `heroImagenUrl`
- `emailComercial`
- `telefonoComercial`
- `direccionComercial`
- `footerTexto`
- `publicada`
- `createdAt`
- `updatedAt`

## Impacto sobre seguridad

- Se agrego proteccion explicita para `/admin/**` con rol `ADMIN_MD`.
- Se mantiene la politica `deny-by-default` definida en MT4.
- No se abrieron endpoints publicos nuevos.

## Compatibilidad

- No se modifico catalogo, pricing, pedidos, pagos, envios ni RMA.
- No se alteraron endpoints publicos multitenant.
- Se reutilizaron `Revendedor` y `RevendedorStorefront` sin cambios de modelo ni migraciones adicionales.

## Tests agregados o ajustados

- `AdminRevendedorServiceTest`
- `AdminRevendedorControllerTest`
- ajuste en `SecurityAccessTest` para `/admin/revendedores`

## Pendiente para la siguiente etapa

- frontend admin de tiendas
- contexto tenant en frontend
- onboarding de clientes por revendedor
- carrito y pedido publico
- automatizacion operativa de dominios
