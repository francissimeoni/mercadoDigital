ALTER TABLE pedidos
ADD COLUMN tracking_token VARCHAR(36);

CREATE UNIQUE INDEX ux_pedidos_tracking_token ON pedidos (tracking_token);
