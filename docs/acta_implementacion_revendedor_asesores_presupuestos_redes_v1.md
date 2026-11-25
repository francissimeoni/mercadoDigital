# Acta - implementacion revendedor asesores + presupuestos + redes v1

Fecha: 2026-04-27

## Objetivo

Cerrar el siguiente remate funcional del panel `REVENDEDOR`:

- gestion de asesores de venta ligados al revendedor/storefront
- separacion explicita de `Red comercial` en:
  - `Presupuestos`
  - `Redes sociales`
- atribucion de asesor en links comerciales y en checkout publico

## Alcance implementado

### 1) Backend de asesores para revendedor

Se habilitaron endpoints reales:

- `GET /revendedores/{revendedorId}/asesores`
- `POST /revendedores/{revendedorId}/asesores`
- `PUT /revendedores/{revendedorId}/asesores/{asesorId}`

Contrato base:

- nombre (obligatorio)
- fotoUrl (opcional)
- activo (opcional)

Reglas:

- el asesor siempre pertenece al revendedor del path
- actualizacion acotada al mismo revendedor
- alta por defecto en estado activo si no se informa lo contrario

### 2) Red comercial renovada (frontend)

La vista `Red comercial` ahora queda estructurada por bloques reales:

- bloque de asesores:
  - alta de asesor
  - seleccion de asesor activo para campaña
  - activacion/desactivacion
- bloque de operacion comercial:
  - tab `Presupuestos`
  - tab `Redes sociales`

### 3) Seccion Presupuestos

- seleccion de uno o varios productos
- generacion de link compartible de catalogo con `?lista=...`
- incorporacion opcional de asesor en link:
  - `asesorId`
  - `asesor` (nombre visible)
- accion de compartir lista por:
  - `navigator.share`
  - fallback `wa.me` sin API oficial/paga

### 4) Seccion Redes sociales

- compartir producto puntual en formato comercial:
  - nombre
  - precio final
  - entrega estimada
  - link de compra
- envio por:
  - `navigator.share`
  - fallback `wa.me`

### 5) Atribucion de asesor en storefront/checkout

Se completo el hilo tecnico para no perder contexto comercial:

- catalogo publico preserva params de campaña al limpiar filtros:
  - `lista`
  - `asesor`
  - `asesorId`
  - `storefrontHost`
- links de card y detalle conservan params de campaña
- checkout publico ahora toma `asesorId` (UUID valido) y lo envia como `asesorVentaId` al crear pedido

## Archivos principales

Backend:

- `backend/src/main/java/ar/com/mercadodigital/backend/controller/RevendedorController.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/service/RevendedorAsesorService.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/repository/AsesorVentaRepository.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/dto/request/RevendedorAsesorRequest.java`
- `backend/src/main/java/ar/com/mercadodigital/backend/dto/response/RevendedorAsesorResponse.java`

Frontend:

- `frontend/src/pages/RevendedorRedComercialPage.tsx`
- `frontend/src/api/revendedorApi.ts`
- `frontend/src/hooks/useRevendedorAsesores.ts`
- `frontend/src/types/revendedor.ts`
- `frontend/src/storefront/pages/StorefrontCatalogPage.tsx`
- `frontend/src/storefront/components/StorefrontProductCard.tsx`
- `frontend/src/storefront/pages/StorefrontProductDetailPage.tsx`
- `frontend/src/storefront/pages/StorefrontCheckoutPage.tsx`

## Validacion tecnica ejecutada

- backend: `./mvnw.cmd -DskipTests compile` OK
- frontend: `npm run build` OK

## Pendiente siguiente recomendado

- persistir presupuesto como entidad propia con vencimiento/version
- endurecer atribucion comercial con control adicional server-side sobre token/slug de campaña
- exponer resumen de ventas/comisiones por asesor en `Facturas y movimientos`

