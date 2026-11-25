# Acta - Admin storefront proveedor v1

Fecha: 2026-05-07

## Objetivo

Habilitar alta/edición de storefront de proveedor desde backend admin, reutilizando el modelo de storefront existente y preservando la separación:

- storefront `REVENDEDOR` (flujo clásico)
- storefront `PROVEEDOR` (catálogo propio)

## Cambios implementados

### 1) Endpoints admin de proveedor

Se agregan en `AdminProveedorController`:

- `GET /admin/proveedores/{id}/storefront`
- `PUT /admin/proveedores/{id}/storefront`

Request:

- reutiliza `AdminStorefrontRequest`
- incorpora `dominioPublico` para el storefront proveedor

### 2) Servicio de negocio nuevo

`AdminProveedorStorefrontService`:

- valida proveedor existente,
- normaliza y valida `dominioPublico`,
- evita colisión de dominio entre storefronts,
- upsert de `RevendedorStorefront` en modo `PROVEEDOR`,
- persiste branding/comercial/smtp/publicada.

### 3) DTOs storefront admin extendidos

`AdminStorefrontResponse` ahora incluye:

- `tipoStorefront`
- `dominioPublico`

`AdminStorefrontRequest` incorpora:

- `dominioPublico` (necesario para storefront proveedor).

### 4) Compatibilidad revendedor

`AdminRevendedorService` mantiene comportamiento de revendedor y fija:

- `tipoStorefront = REVENDEDOR`
- `dominioPublico = revendedor.dominio`

## Validación

- `./mvnw -q -DskipTests compile`
- `./mvnw -q "-Dtest=AdminProveedorControllerTest,StorefrontControllerTest,PublicCatalogoControllerTest" test`

En verde.

## Alcance pendiente

Queda para la siguiente iteración:

- integración frontend admin de esta nueva sección,
- checkout público proveedor end-to-end.
