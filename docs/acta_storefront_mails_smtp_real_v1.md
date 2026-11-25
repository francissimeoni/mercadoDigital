# Acta - storefront mails SMTP real v1

Fecha: 2026-04-07
Estado: implementado

## Objetivo

Permitir pruebas reales de mails transaccionales desde storefront sin abandonar el fallback local por logs.

## Cambios realizados

1. el backend ahora soporta dos modos de transporte para mails post-compra:
   - `log`
   - `smtp`
2. se agrego `spring-boot-starter-mail`
3. si `app.post-compra-email.transport=smtp`, el backend envia mails reales via SMTP
4. si no, mantiene el comportamiento anterior de solo loguear el mail preparado
5. el `emailComercial` del storefront ahora se reutiliza como:
   - contacto visible
   - `reply-to`
   - remitente preferido del mail transaccional
6. el nombre comercial de la tienda sigue usandose como nombre remitente

## Configuracion esperada

Variables sugeridas:

- `POST_COMPRA_EMAIL_TRANSPORT=smtp`
- `MAIL_HOST=...`
- `MAIL_PORT=587`
- `MAIL_USERNAME=...`
- `MAIL_PASSWORD=...`
- `MAIL_SMTP_AUTH=true`
- `MAIL_SMTP_STARTTLS_ENABLE=true`
- `POST_COMPRA_EMAIL_FROM_EMAIL=...`
- `POST_COMPRA_EMAIL_FROM_NAME=...`
- `POST_COMPRA_EMAIL_BASE_PUBLIC_URL=...`

## Nota operativa

Para una prueba real simple, conviene:

- usar en storefront el mismo email que la cuenta SMTP puede enviar
- o usar un alias/remitente permitido por tu proveedor SMTP

## Validacion ejecutada

- `backend`: `.\mvnw.cmd test`
- `frontend`: `npm run build`

Ambos en verde.
