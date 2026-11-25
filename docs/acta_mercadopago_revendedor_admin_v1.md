# Acta: Mercado Pago por revendedor desde Admin v1

Fecha: 2026-04-03

## Decision

Se deja implementada la base operativa para administrar Mercado Pago por revendedor/storefront desde Admin, manteniendo fallback global para desarrollo local y para no bloquear la validacion actual del flujo publico.

## Alcance cerrado en esta etapa

- entidad persistente `RevendedorMercadoPagoAccount`
- migracion `V16__revendedor_mercadopago_account.sql`
- endpoint admin `PUT /admin/revendedores/{id}/mercadopago`
- endpoint admin `GET /admin/revendedores/{id}/mercadopago/oauth-link`
- callback publico `GET /public/mercadopago/oauth/callback`
- respuesta admin con estado derivado:
  - `PENDIENTE`
  - `VINCULADA`
  - `VENCIDA`
  - variantes con `FALLBACK`
- resolucion de token por revendedor en checkout publico
- resolucion por `mercadoPagoUserId` en webhook para consultar el pago con el token correcto
- soporte frontend admin para visualizar y cargar:
  - `mercadoPagoUserId`
  - `publicKey`
  - `accessToken`
  - `refreshToken`
  - `tokenExpiresAt`
  - `liveMode`
  - `scope`
- generacion de link OAuth para compartir al revendedor
- intercambio backend de `authorization_code` por credenciales del vendedor
- guardado automatico de la vinculacion al volver del callback

## Criterio operativo

- credenciales globales de la app marketplace siguen yendo en configuracion backend
- credenciales del vendedor/revendedor quedan por entidad comercial en base
- la carga manual desde Admin sigue disponible como fallback operativo
- mientras el revendedor no tenga cuenta lista, el sistema puede seguir cobrando con `MERCADOPAGO_ACCESS_TOKEN` global si existe

## Lo que no se cierra todavia

- renovacion automatica de `refresh_token`
- cifrado de tokens en reposo
- webhook real publico en dominio publicado
- configuracion productiva de la app marketplace y split real end-to-end
- definicion e implementacion del reparto real de fondos entre:
  - Mercado Digital
  - revendedor
  - proveedor
- decision explicita de waterfall economico:
  - quien cobra
  - que porcentaje retiene cada actor
  - como se tratan PSP fee, logistica, seguro, cancelaciones y RMA

## Conclusion

Con esta base, el proyecto ya puede seguir desarrollando y validando Checkout Pro sin depender de una sola cuenta fija. El siguiente salto ya queda atado a despliegue publico, webhook accesible, configuracion productiva de OAuth y definicion del split economico real.
