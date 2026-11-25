# Acta - Validacion Local Storefront Desde Admin v1

## Objetivo

Permitir validar de forma mas directa el flujo admin -> storefront publico en entorno local, sin depender de cambiar `VITE_STOREFRONT_HOST` cada vez.

## Decision

- el tenant local acepta override de host por query param `storefrontHost`
- ese override se persiste en `sessionStorage` para mantener la simulacion durante la navegacion local
- el admin de storefronts expone un acceso directo para abrir la tienda publica simulada del dominio configurado

## Alcance implementado

- `TenantContext` ahora resuelve tenant local en este orden:
  - query param
  - session override
  - `VITE_STOREFRONT_HOST`
- `AdminStorefrontsPage` agrega:
  - boton `Abrir storefront simulado`
  - URL local visible para validacion

## Resultado

- desde admin ya se puede guardar dominio y branding, y abrir la tienda publica correspondiente sin editar Vite manualmente
- mejora la validacion E2E del onboarding de tiendas white-label

## Verificacion

- `npm run build` en frontend: OK
