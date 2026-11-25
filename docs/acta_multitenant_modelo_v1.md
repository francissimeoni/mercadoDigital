# Acta - Multitenant Modelo v1

Fecha: 2026-03-11

## Objetivo de la etapa

Implementar la etapa MT1 de multitenant v1 incorporando soporte de modelo y persistencia para:

- tienda white-label por revendedor
- clientes asociados comercialmente a un revendedor

Sin romper el comportamiento actual del backend ni los flujos ya existentes.

## Tablas y entidades agregadas

### `revendedor_storefront`

Nueva tabla para representar la presencia publica white-label del revendedor.

Entidad agregada:
- `RevendedorStorefront`

Campos incorporados:
- `id`
- `revendedor`
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

### `cliente_revendedor`

Nueva tabla para modelar la relacion explicita entre `Usuario` y `Revendedor`.

Entidad agregada:
- `ClienteRevendedor`

Campos incorporados:
- `id`
- `usuario`
- `revendedor`
- `fechaAlta`
- `activo`
- `origenAlta`

## Relaciones incorporadas

- `Revendedor 1 -> 0..1 RevendedorStorefront`
- `Usuario 1 -> * ClienteRevendedor`
- `Revendedor 1 -> * ClienteRevendedor`

Adicionalmente se agregaron las asociaciones JPA inversas minimas:

- `Revendedor.storefront`
- `Revendedor.clientes`
- `Usuario.clientesRevendedor`

## Decisiones tomadas

- Se mantuvo `Revendedor` como tenant comercial base, segun el acta de arquitectura.
- Se separo `RevendedorStorefront` de `Revendedor` para no contaminar la entidad operativa con datos visuales y comerciales publicos.
- Se agrego `ClienteRevendedor` como relacion explicita en lugar de alterar el modelo de `Pedido` o crear una entidad `Cliente` separada.
- Se incorporo constraint unico en `cliente_revendedor (usuario_id, revendedor_id)` para evitar duplicar la misma relacion comercial.
- Se incorporo constraint unico en `revendedor_storefront (revendedor_id)` para sostener el modelo 1 -> 0..1.

## Compatibilidad con el modelo actual

Se mantuvieron sin cambios funcionales:

- `Usuario`
- `Revendedor`
- `Pedido`
- `PedidoItem`
- `Catalogo`
- `Pricing`
- `Pagos`
- `Envios`
- `RMA`
- seguridad y seed actual

No se agregaron endpoints nuevos.
No se modifico logica de negocio existente.
No se altero el flujo actual de pedidos ni catalogo.

## Persistencia y compilacion

Se agrego la migracion:

- `backend/src/main/resources/db/migration/V9__multitenant_modelo_v1.sql`

Se agregaron:

- entidad `RevendedorStorefront`
- entidad `ClienteRevendedor`
- `RevendedorStorefrontRepository`
- `ClienteRevendedorRepository`

Se agrego test minimo no invasivo de mapping/relaciones:

- `MultitenantModeloMappingTest`

## Pendiente para la siguiente etapa

Queda pendiente para etapas siguientes del roadmap:

- resolucion de storefront por dominio
- endpoints publicos `/public/**`
- catalogo publico por tienda
- endurecimiento de seguridad para superficies publicas
- administracion de tiendas
- contexto tenant en frontend
- aislamiento operativo de clientes por revendedor durante la compra

## Resultado

La base de datos y el modelo JPA ya soportan:

- una tienda white-label por revendedor
- clientes por revendedor

Manteniendo compatibilidad con la arquitectura y el comportamiento actual del sistema.
