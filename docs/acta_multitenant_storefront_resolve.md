# Acta - Multitenant Storefront Resolve

Fecha: 2026-03-11

## Objetivo

Implementar la etapa MT2 de multitenant v1 para que el backend pueda resolver que tienda white-label corresponde a un host dado, sin mezclar aun catalogo publico ni flujo de compra.

## Endpoint agregado

Se agrego el endpoint publico:

- `GET /public/storefront/resolve?host=...`

Controller agregado:

- `StorefrontController`

Service agregado:

- `StorefrontService`

## DTO de respuesta

Se agrego:

- `StorefrontResponse`

Campos incluidos:

- `revendedorId`
- `dominio`
- `nombreComercial`
- `logoUrl`
- `faviconUrl`
- `colorPrimario`
- `colorSecundario`
- `heroTitulo`
- `heroSubtitulo`
- `heroImagenUrl`
- `emailComercial`
- `telefonoComercial`
- `direccionComercial`
- `footerTexto`
- `publicada`

## Reglas de resolucion por dominio

La resolucion usa como base:

- `Revendedor.dominio`
- `RevendedorStorefront` asociado al revendedor

Flujo implementado:

1. normalizar `host`
2. buscar `Revendedor` por dominio
3. validar que el revendedor este activo
4. buscar `RevendedorStorefront` asociado
5. validar que el storefront este publicado
6. devolver `StorefrontResponse`

Normalizacion aplicada al host:

- trim
- lowercase
- remocion de `http://` o `https://` si vienen por error
- remocion de path
- remocion de puerto
- remocion de punto final

## Comportamiento ante no disponible

Se adopto una politica conservadora para superficie publica.

El endpoint devuelve `404 Not Found` en todos estos casos:

- no existe revendedor para ese host
- el revendedor existe pero esta inactivo
- el revendedor existe pero no tiene storefront
- el storefront existe pero no esta publicado

Motivo de la decision:

- no filtrar estado interno de tiendas
- no exponer si el dominio existe pero esta deshabilitado
- mantener un contrato simple y seguro para el futuro frontend publico

El mensaje usado es:

- `Storefront no disponible para host: ...`

## Comportamiento ante request invalido

Si `host` llega vacio o en blanco, el backend devuelve `400 Bad Request`.

## Impacto sobre seguridad

Se habilito explicitamente como publico:

- `/public/**`

No se agregaron otros endpoints publicos.
No se modifico la seguridad de catalogo, pedidos, logistica, proveedor, RMA ni notificaciones.

## Tests agregados

Se agregaron tests minimos para:

- resolucion exitosa
- dominio inexistente
- storefront no publicado
- revendedor inactivo
- acceso publico sin token
- request invalido sin host util

Archivos:

- `backend/src/test/java/ar/com/mercadodigital/backend/service/StorefrontServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/StorefrontControllerTest.java`
- ajuste en `SecurityAccessTest`

## Pendiente para la siguiente etapa

Queda pendiente para MT3 y etapas posteriores:

- `GET /public/catalogo`
- `GET /public/catalogo/{id}`
- reutilizacion de logica de catalogo para superficie publica
- frontend publico white-label
- contexto tenant en frontend
- admin de tiendas
- endurecimiento global de seguridad para toda la superficie publica

## Resultado

El backend ya puede resolver la tienda white-label correspondiente a un dominio/host dado y devolver su configuracion publica minima de forma segura y consistente con la arquitectura multitenant definida.
