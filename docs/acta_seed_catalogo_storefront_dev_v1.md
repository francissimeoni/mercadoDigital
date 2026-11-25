# Acta - Seed catalogo storefront dev v1

Fecha: 2026-03-17

## Objetivo

Ampliar el seed de desarrollo para simular un catalogo real end-to-end del storefront sin alterar reglas de negocio ni hardcodear datos en frontend.

## Cambios realizados

Se actualizo `LocalDemoDataSeeder` para dejar operativo un escenario de desarrollo con:

- 1 proveedor demo
- 1 revendedor demo con storefront funcional
- 30 productos canonicos
- 30 productos/ofertas del proveedor
- stock positivo por producto
- markup activo por revendedor para todos los productos
- media principal por producto

## Modelo real respetado

La oferta del marketplace sigue resolviendose con la estructura real existente:

- `ProductoCanonico`
- `Producto`
- `ProveedorStock`
- `RevendedorMarkup`
- `ProductoMedia`

No se agregaron tablas, endpoints ni reglas nuevas.

## Criterio de carga

El seed queda idempotente:

- conserva los IDs historicos del primer producto demo
- genera IDs deterministas para los demas productos usando `UUID.nameUUIDFromBytes`
- no duplica datos al reiniciar con `app.seed-demo.enabled=true`

## Impacto funcional esperado

Con perfil local y seed activo, el storefront demo debe tener:

- catalogo publico con volumen realista
- detalle publico funcional para multiples productos
- checkout publico funcional sobre productos visibles

El flujo actual de:

- catalogo
- detalle
- checkout publico
- inicio PSP

queda preservado.

## Validacion

Se valido compilacion backend:

- `./mvnw -q -DskipTests compile`

## Archivos modificados

- `backend/src/main/java/ar/com/mercadodigital/backend/bootstrap/LocalDemoDataSeeder.java`

## Conclusion

El entorno local ahora representa mejor un marketplace real con catalogo amplio y visible en storefront, sin romper la logica actual ni mover verdad de negocio fuera del backend.
