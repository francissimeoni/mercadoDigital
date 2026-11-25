# Playbook - Validacion comercial publica PSP

Fecha: 2026-04-03

## Objetivo

Cerrar la validacion comercial real del flujo publico:

1. cliente compra en storefront
2. backend crea pedido publico
3. backend prepara checkout PSP
4. cliente paga en Mercado Pago sandbox
5. Mercado Pago vuelve al storefront
6. Mercado Pago llama al webhook publico
7. backend confirma pago y crea envio
8. tracking publico refleja el estado real

## Precondiciones

- backend levantado en `http://localhost:8080`
- frontend levantado en `http://localhost:5173`
- credenciales sandbox reales cargadas:
  - `MERCADOPAGO_ACCESS_TOKEN`
- tunel publico HTTPS disponible para backend:
  - `ngrok`
  - `cloudflared`
  - equivalente

## Configuracion recomendada

- `MERCADOPAGO_BASE_URL=https://api.mercadopago.com`
- `MERCADOPAGO_RETURN_URL=http://localhost:5173/pago/retorno`
- `MERCADOPAGO_WEBHOOK_URL=https://<url-publica>/webhooks/psp/mercadopago`
- `MERCADOPAGO_CURRENCY=ARS`

## Senales de arranque nuevas

Al iniciar backend ahora queda un diagnostico en logs para Mercado Pago:

- si faltan variables criticas, lo informa explicitamente
- si `MERCADOPAGO_WEBHOOK_URL` sigue en `localhost`, avisa que Mercado Pago no podra entrar desde fuera
- si `MERCADOPAGO_RETURN_URL` sigue en `localhost`, lo marca como valido solo para prueba local en ese navegador

Esto no cambia negocio; solo hace mas visible por que una validacion real puede quedar bloqueada.

Nota local importante:

- no se fuerza `auto_return=approved` en la preferencia porque Mercado Pago rechazo esa combinacion con `localhost`
- el retorno sigue definido por `back_urls`

## Secuencia manual sugerida

1. abrir tunel publico hacia `8080`
2. exportar `MERCADOPAGO_WEBHOOK_URL` con la URL HTTPS publica
3. reiniciar backend
4. revisar logs de arranque y confirmar que no falten `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_RETURN_URL` ni `MERCADOPAGO_WEBHOOK_URL`
5. crear pedido publico desde storefront
6. preparar pago con `POST /public/pedido/{pedidoId}/pago`
7. confirmar que la respuesta trae `checkoutUrl`
8. completar pago sandbox en Mercado Pago
9. verificar retorno a `/pago/retorno`
10. verificar en backend recepcion de webhook
11. verificar en tracking publico:
  - `estadoPedido = PAGADO`
  - `estadoPago = CONFIRMADO`
12. verificar envio creado en `PENDIENTE_PREPARACION`

## Evidencia minima a guardar

- URL publica usada en esa corrida
- captura o log del `checkoutUrl`
- evidencia de retorno a `/pago/retorno`
- evidencia de entrada a `/webhooks/psp/mercadopago`
- evidencia de `pedido = PAGADO`
- evidencia de `pago = CONFIRMADO`
- evidencia de tracking publico actualizado

## Si falla

- si falta `checkoutUrl`: revisar credenciales `MERCADOPAGO_*`
- si vuelve del PSP pero tracking sigue `PENDIENTE`: revisar si webhook publico entro realmente
- si webhook no entra: revisar tunel, HTTPS publico y valor real de `MERCADOPAGO_WEBHOOK_URL`
- si entra webhook pero no confirma: revisar monto, `referenciaPedido` y `referenciaPago`
