# Acta - Multitenant Catalogo Publico

Fecha: 2026-03-11

## Objetivo

Implementar la etapa MT3 de multitenant v1 para exponer catalogo publico por tienda white-label, reutilizando la logica real ya existente del backend y sin duplicar negocio.

## Endpoints agregados

Se agregaron los endpoints publicos:

- `GET /public/catalogo?revendedorId={uuid}`
- `GET /public/catalogo/{productoCanonicoId}?revendedorId={uuid}`

Controller agregado:

- `PublicCatalogoController`

Service agregado:

- `PublicCatalogoService`

## DTOs publicos definidos

Se agregaron DTOs publicos explicitos para no acoplar directamente el contrato externo a DTOs internos:

- `PublicCatalogoItemResponse`
- `PublicProductoDetalleResponse`

### Campos reales expuestos en listado publico

- `productoCanonicoId`
- `nombrePublico`
- `mejorPrecioFinalEstimado`
- `demoraEstimadaDias`
- `stockDisponible`
- `cantidadOfertas`
- `imagenPrincipal`

### Campos reales expuestos en detalle publico

- `productoCanonicoId`
- `nombrePublico`
- `descripcion`
- `mejorPrecioFinalEstimado`
- `demoraEstimadaDias`
- `stockDisponible`
- `imagenPrincipal`

## Datos explicitamente no expuestos

La superficie publica NO expone:

- proveedor
- proveedorId
- productoId interno de abastecimiento
- costo
- precioCosto
- split
- markup interno
- ids de stock
- ids operativos internos
- datos logisticos internos

## Reglas de visibilidad del catalogo publico

El catalogo publico reutiliza la logica real del backend:

- `CatalogoService`
- seleccion de mejor oferta existente
- reglas actuales de stock visible
- media principal / primera imagen

No se duplico pricing ni seleccion de oferta en otra capa.

La capa publica implementada fue minima:

- valida que el `revendedorId` corresponda a una tienda publica disponible
- delega en `CatalogoService`
- mapea la respuesta a DTOs publicos seguros

## Decision sobre reutilizacion de logica existente

Se decidio reutilizar `CatalogoService` directamente a traves de un wrapper liviano `PublicCatalogoService`.

Motivo:

- evita duplicar reglas de catalogo, stock visible, mejor oferta e imagen principal
- mantiene cambios chicos y coherentes
- reduce riesgo de divergencia entre superficie autenticada y superficie publica

No se hizo un refactor grande ni una extraccion nueva de capa compartida porque para esta etapa no era necesario.

## Politica de respuesta publica

Se mantiene una politica conservadora consistente con MT2.

### Se responde `404 Not Found` cuando:

- el revendedor no existe
- el revendedor esta inactivo
- el revendedor no tiene storefront
- el storefront no esta publicado
- el producto canonico no existe
- el producto no tiene oferta visible para ese revendedor

### Se responde `200 OK` con lista vacia cuando:

- el revendedor esta disponible publicamente
- pero hoy no tiene productos visibles en el catalogo

Esto evita filtrar informacion interna innecesaria y mantiene una semantica natural para el listado.

## Impacto sobre seguridad

No se abrieron endpoints adicionales fuera de:

- `/public/catalogo`
- `/public/catalogo/{productoCanonicoId}`

La seguridad sigue permitiendo `/public/**` como superficie publica.
No se modifico la proteccion del catalogo autenticado del revendedor ni de otros modulos.

## Tests agregados

Se agregaron tests minimos para:

- listado publico exitoso
- detalle publico exitoso
- revendedor no disponible
- producto no visible
- acceso sin token
- validacion de parametros

Archivos:

- `backend/src/test/java/ar/com/mercadodigital/backend/service/PublicCatalogoServiceTest.java`
- `backend/src/test/java/ar/com/mercadodigital/backend/controller/PublicCatalogoControllerTest.java`
- ajuste en `SecurityAccessTest`

## Pendiente para la siguiente etapa

Queda pendiente para etapas siguientes:

- carrito publico
- pedido publico
- login/registro de cliente final
- frontend publico
- contexto tenant en frontend
- endurecimiento general de seguridad para toda la superficie publica

## Resultado

El backend ya expone catalogo publico por revendedor de forma segura, reutilizando la logica real existente y sin filtrar datos internos de abastecimiento ni economicos sensibles.
