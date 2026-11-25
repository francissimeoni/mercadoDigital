# Acta - Admin Revendedores y Storefronts Operativos v1

## Fecha

2026-04-02

## Objetivo

Pasar el modulo admin de revendedores/storefronts desde una superficie de lectura a una superficie operativa real, alineada con el backend existente.

## Alcance implementado

- alta real de revendedor desde frontend admin
- edicion real de nombre, dominio y estado activo del revendedor
- configuracion real de storefront:
  - nombre comercial
  - hero titulo
  - hero subtitulo
  - logo URL
  - hero imagen URL
  - colores
  - email comercial
  - telefono comercial
  - direccion comercial
  - footer
  - estado publicada / borrador
- preview visual basica dentro del panel admin

## Backend involucrado

El backend ya disponia de:

- `POST /admin/revendedores`
- `PUT /admin/revendedores/{id}`
- `PUT /admin/revendedores/{id}/storefront`

No fue necesario cambiar contratos backend para esta v1.

## Frontend involucrado

Se agregaron:

- requests y tipos admin para create/update/upsert
- hooks de mutacion con invalidacion de cache
- nueva version operativa de `AdminStorefrontsPage`

## Resultado

El admin ya no depende solo de seed manual para dar de alta o editar tiendas.

Queda disponible una base real para:

- registrar una tienda white-label
- cambiar su dominio
- completar branding
- publicarla o dejarla en borrador

## Pendiente

- validacion manual completa del flujo desde admin hasta reflejo en storefront publico
- posible mejora futura de carga de imagen/logo sin depender de URL manual
- posible separacion futura entre gestion de revendedor y branding storefront si el modulo crece
