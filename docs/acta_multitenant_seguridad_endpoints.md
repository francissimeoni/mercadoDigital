# Acta MT4 - Endurecimiento de seguridad de endpoints

## Objetivo

Implementar la etapa MT4 de multitenant v1 en backend para dejar explícita la superficie pública permitida y cerrar aperturas accidentales en endpoints operativos o sensibles.

## Política final de seguridad

La configuración quedó bajo una política explícita y conservadora:

- Público sin token:
  - `POST /auth/login`
  - `/public/**`
- Público técnico preexistente:
  - `POST /webhooks/psp`
- Protegido por rol o autenticación:
  - `/catalogo/**` -> `REVENDEDOR`
  - `/pedido/**` -> `REVENDEDOR`
  - `/revendedores/**` -> `REVENDEDOR`
  - `/proveedores/**` -> `PROVEEDOR`
  - `POST /envios/*/listo-para-retiro` -> `PROVEEDOR`
  - `/logistica/**` -> `LOGISTICA_MD`
  - `/envios/**` -> `LOGISTICA_MD`, excepto la transición de proveedor ya explicitada
  - `/rma/**` -> `ADMIN_MD`
  - `/notificaciones/**` -> autenticado
  - `/productos/**` -> `ADMIN_MD`
  - `/productos-canonicos/**` -> `ADMIN_MD`
  - `/media/**` -> `ADMIN_MD`

La política final de `anyRequest()` quedó en `denyAll()`.

## Endpoints públicos explícitos

Superficie pública de usuario en esta etapa:

- `POST /auth/login`
- `GET /public/storefront/resolve`
- `GET /public/catalogo`
- `GET /public/catalogo/{productoCanonicoId}`

Excepción técnica no orientada a usuario final:

- `POST /webhooks/psp`

## Endpoints potencialmente abiertos que fueron cerrados

Se cerraron explícitamente endpoints que antes podían caer en el `permitAll()` final:

- `GET /productos`
- `GET /productos/{id}`
- `GET /productos-canonicos/{id}/ofertas`
- `POST /productos/{id}/media`
- `GET /productos/{id}/media`
- `DELETE /media/{id}`

La decisión fue restringirlos a `ADMIN_MD` porque:

- no forman parte de la superficie pública multitenant
- no son usados hoy por el frontend operativo
- exponen datos internos de producto, oferta o media fuera del contrato público controlado

## Impacto sobre compatibilidad

- No se modificó lógica de negocio.
- No se alteraron contratos funcionales de los endpoints públicos multitenant.
- No se rompió el catálogo autenticado del revendedor ni los módulos operativos existentes.
- Se endureció seguridad en endpoints que no tenían cobertura explícita y podían quedar abiertos por default.

## Tests ajustados

Se ampliaron tests de acceso para validar:

- públicos permitidos sin token
- catálogo autenticado de revendedor
- notificaciones autenticadas
- proveedor y transición `listo-para-retiro`
- logística y envíos
- RMA admin
- cierre de `/productos/**`, `/productos-canonicos/**` y `/media/**`

## Pendiente para siguiente etapa

- Mantener esta política como base para futuras superficies públicas de carrito o pedido cliente final
- Revisar si en etapas posteriores conviene separar endpoints de backoffice de endpoints operativos con prefijos más explícitos
- Incorporar seguridad específica para nuevas superficies públicas sin relajar el `deny-by-default`
