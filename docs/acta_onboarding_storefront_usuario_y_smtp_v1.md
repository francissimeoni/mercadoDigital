# Acta - onboarding storefront con usuario revendedor y SMTP v1

Fecha: 2026-04-07
Estado: implementado

## Objetivo

Permitir que una tienda nueva pueda quedar operativa sin pasos manuales por base:

- usuario/login real del revendedor
- configuracion SMTP propia por storefront

## Cambios realizados

1. `AdminRevendedorRequest` ahora soporta:
   - `usuarioEmail`
   - `usuarioPassword`
2. el admin crea o actualiza automaticamente el `Usuario` asociado al revendedor:
   - `tipoUsuario = REVENDEDOR`
   - rol `REVENDEDOR`
   - password hasheada
3. `AdminStorefrontRequest` ahora soporta SMTP propio por tienda:
   - `smtpHost`
   - `smtpPort`
   - `smtpUsername`
   - `smtpPassword`
   - `smtpAuth`
   - `smtpStartTls`
4. el backend preserva la password existente si se edita la tienda sin reingresarla
5. los mails post-compra ahora pueden salir con SMTP propio del storefront cuando exista
6. el admin frontend ya expone:
   - usuario del revendedor
   - password inicial o nueva password
   - seccion SMTP storefront

## Validacion ejecutada

- `backend`: `.\mvnw.cmd test`
- `frontend`: `npm run build`

Ambos en verde.
