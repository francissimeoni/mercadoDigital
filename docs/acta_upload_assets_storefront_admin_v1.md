# Acta - Upload Assets Storefront Admin v1

## Objetivo

Permitir cargar imagenes de storefront desde el admin sin depender de URLs externas ni rutas locales del equipo.

## Alcance implementado

- upload real desde admin para:
  - logo
  - hero
  - favicon
- almacenamiento local en backend
- exposicion publica de esos archivos bajo ruta publica
- autocompletado de `logoUrl`, `heroImagenUrl` y `faviconUrl` en el formulario admin

## Decisiones

- los assets se guardan en backend como archivos locales
- el backend devuelve URL publica lista para ser persistida en la storefront
- se mantienen tambien los campos URL manuales para casos futuros

## Resultado

- el onboarding de tienda ya no depende de pegar URLs de imagen externas
- el admin puede completar branding visual real desde la propia plataforma

## Verificacion

- `./mvnw.cmd -q -DskipTests compile` en backend: OK
- `./mvnw.cmd -q "-Dtest=AdminRevendedorControllerTest,SecurityAccessTest" test` en backend: OK
- `npm run build` en frontend: OK
