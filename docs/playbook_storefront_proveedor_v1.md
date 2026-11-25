# Playbook - Storefront proveedor v1 (base)

## Objetivo

Operar y validar la base de storefront de proveedor sin romper storefront de revendedor.

## Precondiciones

1. Backend desplegado con migración `V31__storefront_tipo_y_proveedor.sql`.
2. Proveedor existente y activo.
3. Productos activos del proveedor con stock.

## 1) Verificar migración

Chequeo recomendado en DB:

- columna `tipo_storefront` en `revendedor_storefront`
- columna `dominio_publico`
- columna `proveedor_id`
- constraints:
  - `uq_storefront_dominio_publico`
  - `uq_storefront_proveedor`
  - `fk_storefront_proveedor`

## 2) Crear storefront proveedor (operación manual inicial)

En esta etapa base, el alta se hace manual por SQL.

Ejemplo:

```sql
INSERT INTO revendedor_storefront (
  id,
  tipo_storefront,
  dominio_publico,
  proveedor_id,
  nombre_comercial,
  publicada,
  created_at,
  updated_at
) VALUES (
  UUID(),
  'PROVEEDOR',
  'tienda-proveedor.demo.com',
  'PROVEEDOR_UUID_AQUI',
  'Tienda Proveedor Demo',
  b'1',
  NOW(6),
  NOW(6)
);
```

Notas:

- `revendedor_id` debe quedar `NULL` en storefront tipo proveedor.
- `dominio_publico` debe ser único.

## 3) Validar resolve público por host

Request:

`GET /public/storefront/resolve?host=tienda-proveedor.demo.com`

Esperado:

- `tipoStorefront = PROVEEDOR`
- `proveedorId` informado
- `revendedorId = null`
- `publicada = true`

## 4) Validar catálogo proveedor

Request:

`GET /public/catalogo/proveedor?proveedorId=<ID>`

Esperado:

- sólo productos activos del proveedor owner,
- sin mezcla de ofertas de otros proveedores,
- cada item con `productoId`.

Detalle:

`GET /public/catalogo/proveedor/{productoId}?proveedorId=<ID>`

Esperado:

- producto del proveedor owner,
- error 404 si el producto pertenece a otro proveedor o no tiene stock.

## 5) Regression mínima revendedor

1. `GET /public/storefront/resolve?host=<dominio_revendedor>`
2. `GET /public/catalogo?revendedorId=<ID>`
3. `GET /public/catalogo/{productoCanonicoId}?revendedorId=<ID>`

Esperado:

- flujo existente sin cambios funcionales.

## Riesgos operativos conocidos

1. Aún no está implementado checkout proveedor end-to-end.
2. Aún no existe panel admin para alta/edición de storefront proveedor (manual SQL).
3. Pricing proveedor hoy usa base operativa; falta capa de markup comercial propia configurable.

## Próxima etapa sugerida

1. Endpoint admin para alta/edición storefront proveedor.
2. Checkout público proveedor (`productoId` directo, sin canonización).
3. Regla explícita de markup comercial para proveedor en storefront propio.

