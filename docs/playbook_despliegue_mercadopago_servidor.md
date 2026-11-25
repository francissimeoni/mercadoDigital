# Playbook - Despliegue de Mercado Pago en servidor

Fecha: 2026-04-06

## Objetivo

Dejar una guia corta y operativa para cerrar Mercado Pago cuando la plataforma se publique en servidor.

Este playbook no bloquea desarrollo local.

## Cuando usar este documento

Usarlo recien cuando exista:

- backend publicado
- frontend publicado
- SSL activo
- dominio o subdominio definitivo

## Topologia recomendada

Se recomienda:

- portal madre: `https://www.mercado-digital.com.ar`
- backend API: `https://api.mercado-digital.com.ar`

No hace falta comprar otro dominio.

`api.mercado-digital.com.ar` es un subdominio del dominio actual.

## Variables a cargar en servidor

### Frontend

- `VITE_API_BASE_URL=https://api.mercado-digital.com.ar`

### Backend

- `MERCADOPAGO_BASE_URL=https://api.mercadopago.com`
- `MERCADOPAGO_CLIENT_ID=<client_id_app_mp>`
- `MERCADOPAGO_CLIENT_SECRET=<client_secret_app_mp>`
- `MERCADOPAGO_REDIRECT_URI=https://api.mercado-digital.com.ar/public/mercadopago/oauth/callback`
- `MERCADOPAGO_ADMIN_REDIRECT_URL=https://www.mercado-digital.com.ar/admin/storefronts`
- `MERCADOPAGO_WEBHOOK_URL=https://api.mercado-digital.com.ar/webhooks/psp/mercadopago`
- `MERCADOPAGO_RETURN_URL=https://<storefront-real>/pago/retorno`
- `MERCADOPAGO_ACCESS_TOKEN=<token_global_test_o_fallback>`
- `MERCADOPAGO_PUBLIC_KEY=<public_key_global_test_o_fallback>`
- `MERCADOPAGO_CURRENCY=ARS`

## Aclaracion sobre `MERCADOPAGO_RETURN_URL`

No debe volver al portal madre.

Debe volver al storefront real usado para la compra publica.

Ejemplo:

- `https://www.ventasnavarro.com/pago/retorno`

## Pre-check tecnico de servidor

Antes de probar OAuth o Checkout Pro:

1. crear DNS de `api.mercado-digital.com.ar`
2. publicar backend en ese host
3. emitir SSL valido
4. configurar frontend con `VITE_API_BASE_URL`
5. verificar CORS si portal madre y API quedan en hosts distintos
6. cargar variables `MERCADOPAGO_*`
7. reiniciar backend y frontend

## Pre-check en Mercado Pago

Antes de probar OAuth real:

1. entrar a la app de Mercado Pago
2. registrar la redirect URI exacta:
   `https://api.mercado-digital.com.ar/public/mercadopago/oauth/callback`
3. confirmar uso de credenciales sandbox o productivas segun etapa

## Validacion recomendada post deploy

### OAuth revendedor

1. entrar al admin MD
2. abrir storefronts / revendedores
3. generar link OAuth
4. completar autorizacion de Mercado Pago
5. verificar vuelta a:
   `https://www.mercado-digital.com.ar/admin/storefronts`
6. confirmar que el revendedor queda vinculado

### Checkout Pro sandbox end-to-end

1. entrar al storefront publicado del revendedor
2. crear pedido publico
3. preparar pago
4. confirmar que backend devuelve `checkoutUrl`
5. pagar con tarjeta sandbox
6. verificar retorno a `/pago/retorno`
7. verificar entrada del webhook real
8. verificar:
   - `pedido = PAGADO`
   - `pago = CONFIRMADO`
   - `envio = PENDIENTE_PREPARACION`

## Senales esperadas en logs

Al iniciar backend no deberian quedar warnings por:

- `MERCADOPAGO_WEBHOOK_URL` en localhost
- `MERCADOPAGO_RETURN_URL` en localhost
- `MERCADOPAGO_REDIRECT_URI` en localhost
- `MERCADOPAGO_ADMIN_REDIRECT_URL` en localhost

## Referencias relacionadas

- `docs/acta_cierre_mercadopago_servidor_urls_y_variables_v1.md`
- `docs/acta_migracion_psp_mercadopago_checkout_pro_v1.md`
- `docs/acta_mercadopago_revendedor_admin_v1.md`
- `docs/playbook_validacion_comercial_publica_psp.md`
