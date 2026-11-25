ALTER TABLE splits_pedido_item
  ADD COLUMN estado_liquidacion_proveedor VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE',
  ADD COLUMN liberado_proveedor_at DATETIME(6) NULL,
  ADD COLUMN motivo_liberacion_manual VARCHAR(255) NULL;
