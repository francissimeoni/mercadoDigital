# Playbook - Admin storefront proveedor v1

## Objetivo

Operar storefronts de proveedor desde admin backend.

## Endpoints

Base:

- `/admin/proveedores/{proveedorId}/storefront`

Operaciones:

1. `GET` estado/config actual.
2. `PUT` alta o edición (upsert).

## Payload mínimo recomendado (PUT)

```json
{
  "dominioPublico": "tienda-proveedor.demo.com",
  "nombreComercial": "Tienda Proveedor Demo",
  "publicada": true
}
```

## Payload completo (opcional)

Además del mínimo, se pueden enviar:

- `logoUrl`, `faviconUrl`
- `colorPrimario`, `colorSecundario`
- `heroTitulo`, `heroSubtitulo`, `heroImagenUrl`
- `emailComercial`, `telefonoComercial`, `direccionComercial`
- `footerTexto`
- `smtpHost`, `smtpPort`, `smtpUsername`, `smtpPassword`, `smtpAuth`, `smtpStartTls`

## Reglas operativas

1. `dominioPublico` debe ser único entre storefronts.
2. El storefront proveedor queda con:
   - `tipoStorefront = PROVEEDOR`
   - `revendedor = null`
   - `proveedor = proveedorId`
3. Si `publicada = true`, el resolve público por host ya lo expone en `/public/storefront/resolve`.

## Validación rápida post-configuración

1. Resolver storefront:

`GET /public/storefront/resolve?host=tienda-proveedor.demo.com`

Esperado:

- `tipoStorefront = PROVEEDOR`
- `proveedorId` cargado.

2. Catálogo proveedor:

`GET /public/catalogo/proveedor?proveedorId=<proveedorId>`

Esperado:

- sólo productos propios del proveedor owner.

## Troubleshooting

1. Error de dominio duplicado:
   - revisar colisión en `dominio_publico`.
2. Storefront no resuelve:
   - confirmar `publicada=true`,
   - confirmar proveedor activo,
   - confirmar dominio exacto.
