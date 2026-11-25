# Acta de Migraciones V1-V4

## Alcance
- Se aplico plan B: reset local + migraciones limpias por etapas.
- Se elimino la migracion previa `V1__init.sql`.
- Se definieron nuevas migraciones:
  - `V1__minimal.sql`
  - `V2__core_marketplace.sql`
  - `V3__pedidos_pagos_psp.sql`
  - `V4__envios_seguro_rma.sql`

## Tablas por migracion

## V1__minimal.sql
- `auditoria_eventos`
- `usuarios`
- `roles`
- `usuarios_roles`

## V2__core_marketplace.sql
- `proveedores`
- `revendedores`
- `asesores_venta`
- `productos`
- `proveedor_stock`
- `revendedor_markups`

## V3__pedidos_pagos_psp.sql
- `pedidos`
- `pedido_items`
- `pagos`
- `psp_webhooks`
- `splits_pedido_item`

## V4__envios_seguro_rma.sql
- `transportistas`
- `tarifas_transportista`
- `envios`
- `rmas`

## Cambios en entidades (JPA)
- `BaseEntity`:
  - UUID con `@JdbcTypeCode(SqlTypes.CHAR)` para mapear `CHAR(36)`.
  - `created_at` y `updated_at`.
- `AuditoriaEvento`:
  - alineada a: `tipo_evento`, `referencia_id`, `payload_json`, `fecha_evento`.
- `Usuario`:
  - se quito `password_hash`.
  - se agrego `tipo_usuario`, `activo`, y relacion `usuarios_roles`.
- `Rol` (nueva).
- `Proveedor`:
  - `nombre`, `activo`.
- `Revendedor`:
  - `nombre`, `dominio`, `activo`.
- `AsesorVenta` (nueva).
- `Producto`:
  - `precio_costo`, `moneda`, `categoria_paquete`, `activo`.
- `ProveedorStock` (nueva).
- `RevendedorMarkup`:
  - `markup_porcentaje`, `markup_fijo`, `activo`.
- `Pedido`:
  - `revendedor_id`, `cliente_id`, `estado_pedido`.
  - `total_bruto`, `total_psp`, `total_envio`, `total_seguro`, `total_final`.
- `PedidoItem`:
  - `proveedor_id`, `precio_costo`, `markup_aplicado`, `precio_item_final`, `tiene_seguro`, `monto_seguro`.
- `Pago`:
  - `estado_pago`, `metodo`, `monto`, `psp_referencia`.
- `PspWebhook` (nueva):
  - `id_evento_psp` unique.
- `SplitPedidoItem` (nueva).
- `Transportista` (nueva).
- `TarifaTransportista` (nueva).
- `Envio`:
  - `transportista_id`, `categoria_paquete`, `tracking`, `estado_envio`, `direccion_envio`, `costo_envio`.
- `RMA`:
  - `estado_rma`, `tiene_seguro`, `responsable_probable`, `creado_por_usuario_id`.

## Cambios en controladores/DTOs/servicios
- DTO publico de productos:
  - `ProductoResponse` ya no expone `proveedorId` ni `proveedorNombre`.
  - ahora devuelve `precioCosto`, `moneda`, `categoriaPaquete`, `activo`.
- `CrearPedidoRequest`:
  - usa `clienteId` y `revendedorId`.
- `PedidoResponse` y `PedidoItemResponse`:
  - alineados a los nuevos campos de negocio en pedidos/items.
- `PedidoService`:
  - adaptado a nuevo modelo de `Pedido`, `PedidoItem`, `Pago`, `Envio`.
  - sin split funcional real (solo estructura base, segun restriccion).
- `PedidoMapper` y `ProductoMapper`:
  - actualizados al nuevo contrato de DTO.

## Repositorios agregados
- `RolRepository`
- `AsesorVentaRepository`
- `ProveedorStockRepository`
- `PspWebhookRepository`
- `SplitPedidoItemRepository`
- `TransportistaRepository`
- `TarifaTransportistaRepository`

## Comandos para reset y arranque local
```powershell
docker compose down -v
docker compose up -d
./mvnw -q -DskipTests compile
./mvnw spring-boot:run
```

## Checklist de verificacion
- [ ] Contenedor MySQL levantado y accesible en `127.0.0.1:3307`.
- [ ] Base `mercado_digital` limpia (sin `flyway_schema_history` previa).
- [ ] Flyway ejecuta `V1` a `V4` sin errores.
- [ ] Hibernate `ddl-auto=validate` no reporta mismatch de esquema.
- [ ] Endpoints base responden:
  - [ ] `GET /productos`
  - [ ] `GET /productos/{id}`
  - [ ] `POST /pedido`
  - [ ] `GET /pedido/{id}`
- [ ] `ProductoResponse` no incluye datos de proveedor.
- [ ] Proyecto compila con `./mvnw -q -DskipTests compile`.

## Validacion de compilacion
- Estado actual: compila correctamente con:
  - `./mvnw -q -DskipTests compile`
