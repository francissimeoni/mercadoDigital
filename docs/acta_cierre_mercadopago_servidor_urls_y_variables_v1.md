# Acta - Cierre Mercado Pago en servidor: URLs y variables v1

Fecha: 2026-04-06

## Objetivo

Dejar cerrado el criterio operativo para configurar Mercado Pago en servidor sin mezclar:

- portal madre MD
- storefront publico del revendedor
- callback OAuth backend
- webhook PSP backend

## Decision de superficies

Se mantiene la separacion funcional ya asentada:

- `https://www.mercado-digital.com.ar` = portal madre MD
- `https://<dominio-revendedor>` = storefront publico del revendedor

Consecuencia:

- `MERCADOPAGO_ADMIN_REDIRECT_URL` debe volver al portal madre
- `MERCADOPAGO_RETURN_URL` debe volver al storefront/checkout publicado
- `MERCADOPAGO_REDIRECT_URI` y `MERCADOPAGO_WEBHOOK_URL` deben apuntar al backend publico real

## Variables requeridas

- `MERCADOPAGO_CLIENT_ID`
- `MERCADOPAGO_CLIENT_SECRET`
- `MERCADOPAGO_REDIRECT_URI`
- `MERCADOPAGO_ADMIN_REDIRECT_URL`
- `MERCADOPAGO_WEBHOOK_URL`
- `MERCADOPAGO_RETURN_URL`
- `MERCADOPAGO_ACCESS_TOKEN`
- `MERCADOPAGO_PUBLIC_KEY`

## Valores ya definidos

Queda validado en documentacion y frontend:

- `MERCADOPAGO_ADMIN_REDIRECT_URL=https://www.mercado-digital.com.ar/admin/storefronts`

## URLs publicas a cerrar segun tipo de deploy

### Opcion A - mismo dominio con reverse proxy

Si el backend se publica detras del portal madre, por ejemplo bajo `/api`:

- `VITE_API_BASE_URL=https://www.mercado-digital.com.ar/api`
- `MERCADOPAGO_REDIRECT_URI=https://www.mercado-digital.com.ar/api/public/mercadopago/oauth/callback`
- `MERCADOPAGO_WEBHOOK_URL=https://www.mercado-digital.com.ar/api/webhooks/psp/mercadopago`

### Opcion B - backend en subdominio propio

Si el backend se publica con host dedicado:

- `VITE_API_BASE_URL=https://api.mercado-digital.com.ar`
- `MERCADOPAGO_REDIRECT_URI=https://api.mercado-digital.com.ar/public/mercadopago/oauth/callback`
- `MERCADOPAGO_WEBHOOK_URL=https://api.mercado-digital.com.ar/webhooks/psp/mercadopago`

## Return URL de Checkout Pro

`MERCADOPAGO_RETURN_URL` no debe volver al portal madre sino a la superficie publica donde termina la compra.

Ejemplo esperado:

- `MERCADOPAGO_RETURN_URL=https://www.ventasnavarro.com/pago/retorno`

Si la validacion inicial se hace sobre una tienda publicada distinta, reemplazar ese dominio por el storefront real usado en la prueba.

## Notas operativas

- OAuth del revendedor usa `MERCADOPAGO_REDIRECT_URI` para el callback backend y luego redirige visualmente a `MERCADOPAGO_ADMIN_REDIRECT_URL`
- Checkout Pro usa `MERCADOPAGO_RETURN_URL` para volver a la UI y `MERCADOPAGO_WEBHOOK_URL` para la confirmacion real
- sin webhook publico real, el tracking puede volver pero el pedido seguira pendiente
- el frontend ya dejo de depender de `http://localhost:8080` hardcodeado y ahora acepta `VITE_API_BASE_URL`

## Pendiente externo al repo

Para cerrar la configuracion productiva falta confirmar cual de estas dos topologias existe realmente en servidor:

1. mismo dominio con reverse proxy `/api`
2. subdominio API dedicado

Con ese dato se cargan los valores finales en servidor y queda lista la prueba OAuth real + Checkout Pro sandbox end-to-end.
