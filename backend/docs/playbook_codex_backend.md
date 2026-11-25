# Playbook Codex - Backend (Mercado Digital)

## Prompt 1 - Revisión scaffold
Revisá el proyecto y alineá entidades/migraciones con docs/especificacion.md y docs/convenciones.md. NO agregues features nuevas. Entregar: lista de diferencias + plan de ajustes.

## Prompt 2 - Modelo mínimo MVP (V2 migración)
Crear migración Flyway V2 con tablas mínimas: usuarios, proveedores, revendedores, asesores_venta, productos, proveedor_stock, revendedor_markup, pedidos, pedido_items, auditoria_eventos. Ajustar entidades JPA en consecuencia.

## Prompt 3 - State machine (enums + transiciones)
Implementar enums y transiciones válidas para pedido/pago/envío/rma. Agregar validaciones de cambio de estado en servicios.

## Prompt 4 - Catálogo productos (sin exponer proveedor)
Endpoints: GET /productos (filtra por revendedor y reglas), GET /productos/{id}. Incluir precio final calculado y envío estimado.

## Prompt 5 - Crear pedido (carrito multi-proveedor)
POST /pedidos: crea pedido con items, calcula split por item (a nivel “lógico”, sin PSP aún), registra auditoría.

## Prompt 6 - Seguro de transporte por producto
Agregar a pedido_items: tiene_seguro, monto_seguro. Agregar lógica de cálculo 2% por producto y excepciones.

## Prompt 7 - Envíos (categorías de paquete)
Agregar modelo: categoria_paquete, transportista, tracking, estado_envio. Endpoint para actualizar tracking.

## Prompt 8 - RMA (flujo completo)
Crear endpoints para solicitar RMA, aprobar RMA, registrar recepción depósito, inspección y resolución. Aplicar reglas del seguro.

## Prompt 9 - Reporte mensual
Generar resumen mensual de intervenciones (envíos + rma + consultas) por revendedor. Exportar JSON/CSV (por ahora).

## Prompt 10 - Auditoría e idempotencia base
Implementar servicio AuditoriaService y tabla auditoria_eventos robusta. Base para webhooks PSP con idempotencia.