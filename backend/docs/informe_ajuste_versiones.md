# Informe de Ajuste de Versiones (Flyway + Modelo)

## 1) Estado actual de Flyway

### Migraciones presentes
- `V1__init.sql`

### Qué crea actualmente cada migración
- `V1__init.sql` crea solo la tabla:
  - `auditoria_eventos`
    - `id BIGINT AUTO_INCREMENT PRIMARY KEY`
    - `tipo_evento VARCHAR(50) NOT NULL`
    - `referencia VARCHAR(100) NULL`
    - `payload_json JSON NULL`
    - `fecha_evento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`

### Observación clave
- El modelo JPA actual define múltiples entidades (`usuarios`, `productos`, `pedidos`, etc.) que no existen en Flyway.
- Con `spring.jpa.hibernate.ddl-auto=validate`, esto rompe en arranque.
- Además, ya hubo desalineación por checksum de `V1` (archivo modificado después de aplicado).

## 2) Diferencias entre el modelo actual (SQL + entidades) y docs/especificacion.md

## Diferencias estructurales
- SQL actual (Flyway) solo tiene `auditoria_eventos`.
- Entidades actuales incluyen: `Usuario`, `Proveedor`, `Revendedor`, `Producto`, `RevendedorMarkup`, `Pedido`, `PedidoItem`, `Pago`, `Envio`, `RMA`, `AuditoriaEvento`.
- Especificación funcional exige además conceptos no modelados explícitamente:
  - PSP obligatorio como canal único.
  - Split por ítem/producto.
  - Idempotencia de webhooks PSP (`id_evento_psp` UNIQUE).
  - Reglas de envío por transportista y categoría de paquete.
  - Seguro de transporte opcional por producto.
  - Roles de negocio (`ADMIN_MD`, `PROVEEDOR`, `REVENDEDOR`, `ASESOR_VENTA`, `CLIENTE`).
  - Trazabilidad/auditoría de pagos, splits, envíos y RMA.

## Diferencias de dominio puntual
- `AuditoriaEvento` entidad vs SQL:
  - Entidad: `id UUID`, `referencia_id UUID`, `created_at`, `updated_at`.
  - SQL: `id BIGINT`, `referencia` string, sin `created_at`/`updated_at`.
- `ProductoResponse` expone `proveedorId` y `proveedorNombre`, mientras `convenciones.md` indica no exponer proveedor al cliente final/revendedor en dropshipping privado.
- Flujo de pago en servicios:
  - Actual: `PedidoService` crea `Pago` básico (`estado`, `metodo`, `monto`), sin entidad/evento de PSP, sin idempotencia.
  - Especificación: PSP obligatorio, split por ítem, captura inmediata con posibilidad futura de diferida.
- Envíos:
  - Actual: `Envio` básico (`estado`, `direccion`, `trackingCode`).
  - Especificación: categorías de paquete + transportistas + reglas de costo.
- Seguro de transporte:
  - No modelado actualmente.
- Asesores:
  - No modelado actualmente.

## 3) Qué está “de más” y qué falta

### De más o no acordado explícitamente (respecto a docs actuales)
- `AuthController` + `TokenService` de token Base64 simple (mock técnico, no modelo de negocio acordado).
- Estados de enums actuales pueden no coincidir con catálogos finales acordados (no documentados aún en especificación).
- Exposición de datos de proveedor en DTO público de producto (potencialmente en conflicto con convención de no exposición).

### Falta (según docs/especificación + convenciones)
- Flyway completo del dominio (hoy casi inexistente).
- Modelo explícito para PSP:
  - transacciones PSP
  - webhooks PSP idempotentes (`id_evento_psp` UNIQUE)
  - split por ítem/proveedor/revendedor/MD.
- Modelo explícito de reglas de envío:
  - transportistas y categorías de paquete.
- Modelo explícito de seguro de transporte por producto/pedido ítem.
- Modelo de roles y, si aplica, relación usuario-rol.
- Auditoría específica para pagos/splits/envíos/RMA.

## 4) Recomendación A vs B

## Recomendación: **B) Reset de DB y migraciones limpias**

### Motivo
- La brecha entre SQL real y entidades es muy grande (no es solo un ajuste menor).
- Ya existe antecedente de conflicto de checksum en `V1`.
- Mantener compatibilidad incremental sobre una base casi vacía aporta complejidad sin beneficio real.
- Un esquema limpio por etapas deja Flyway como única verdad y reduce deuda técnica.

## 5) Plan exacto si se aplica B

### Migraciones propuestas

1. `V1__minimal.sql`
- Tablas:
  - `auditoria_eventos` (alineada a convención de timestamps y estructura acordada)
  - `usuarios`
  - `roles`
  - `usuarios_roles` (si se decide relación N:M)

2. `V2__core_marketplace.sql`
- Tablas:
  - `proveedores`
  - `revendedores`
  - `productos`
  - `revendedor_markups` (si se mantiene estrategia de markup por producto)

3. `V3__pedidos_pagos_psp.sql`
- Tablas:
  - `pedidos`
  - `pedido_items`
  - `pagos`
  - `psp_transacciones`
  - `psp_webhooks` (con `id_evento_psp` UNIQUE para idempotencia)
  - `splits_pedido_item` (distribución por ítem)

4. `V4__envios_seguro_rma.sql`
- Tablas:
  - `envios`
  - `transportistas`
  - `tarifas_transportista` (por categoría de paquete)
  - `seguros_transporte`
  - `rmas`
  - `rma_eventos` (opcional, para trazabilidad fina)

### Cambios en entidades/servicios/controladores

### Entidades
- Alinear `AuditoriaEvento` con esquema definitivo de Flyway.
- Incorporar entidades faltantes del negocio documentado:
  - `Rol` (y puente con `Usuario` si aplica)
  - `PspTransaccion`
  - `PspWebhook`
  - `SplitPedidoItem`
  - `Transportista`
  - `TarifaTransportista`
  - `SeguroTransporte`
- Revisar `ProductoResponse` para no exponer proveedor en endpoints de cara a cliente/revendedor.

### Servicios
- `PedidoService`:
  - calcular precio final incluyendo costo PSP según fórmula documentada;
  - persistir splits por ítem;
  - preparar soporte captura inmediata con diseño extensible a captura diferida;
  - registrar auditoría de eventos críticos.
- Servicios de webhook PSP con idempotencia por `id_evento_psp`.
- Servicios de envío con selección de tarifa por transportista/categoría.
- Servicios RMA con reglas diferenciadas con/sin seguro.

### Controladores
- Mantener los actuales base (`ProductoController`, `PedidoController`) pero:
  - ajustar respuestas para no filtrar datos de proveedor cuando no corresponda.
- Incorporar controlador de webhooks PSP (cuando se implemente flujo PSP).
- Endpoints de envíos/RMA según etapas posteriores.

## 6) Checklist de riesgo y mitigación

- Riesgo: pérdida de datos al reset.
  - Mitigación: backup SQL previo + script de restauración + ventana controlada.

- Riesgo: checksum/estado Flyway inconsistente.
  - Mitigación: reset limpio en entorno local, recrear `flyway_schema_history` desde cero, prohibir edición de migraciones ya aplicadas.

- Riesgo: entidades no alineadas con tipos SQL (UUID/timestamps).
  - Mitigación: definir política única (`CHAR(36)` o `BINARY(16)`) y validar mapeos Hibernate antes de merge.

- Riesgo: regressions en API existente.
  - Mitigación: tests de integración por endpoint crítico (`/productos`, `/pedido`) + fixtures Flyway.

- Riesgo: exposición de datos sensibles/no deseados (proveedor).
  - Mitigación: revisar DTOs públicos y separar DTO interno vs externo.

- Riesgo: lógica de pagos incompleta (sin idempotencia/split).
  - Mitigación: bloquear salida productiva sin tablas/eventos PSP y constraint UNIQUE de `id_evento_psp`.

- Riesgo: deriva futura entre documentación y código.
  - Mitigación: DoD de PR obligatoria: cambio de dominio requiere actualización de `docs/especificacion.md` + nueva migración Flyway.

## Cierre
- El proyecto compila, pero no está alineado funcionalmente con la especificación cerrada.
- Para este contexto, la opción más segura y ordenada es **B (reset + migraciones limpias por etapas)**.
