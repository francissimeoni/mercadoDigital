CREATE TABLE pedidos (
  id CHAR(36) PRIMARY KEY,
  revendedor_id CHAR(36) NOT NULL,
  cliente_id CHAR(36) NOT NULL,
  estado_pedido VARCHAR(40) NOT NULL,
  total_bruto DECIMAL(19,2) NOT NULL,
  total_psp DECIMAL(19,2) NOT NULL,
  total_envio DECIMAL(19,2) NOT NULL,
  total_seguro DECIMAL(19,2) NOT NULL,
  total_final DECIMAL(19,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_pedido_revendedor FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id)
);

CREATE TABLE pedido_items (
  id CHAR(36) PRIMARY KEY,
  pedido_id CHAR(36) NOT NULL,
  producto_id CHAR(36) NOT NULL,
  proveedor_id CHAR(36) NOT NULL,
  cantidad INT NOT NULL,
  precio_costo DECIMAL(19,2) NOT NULL,
  markup_aplicado DECIMAL(19,2) NOT NULL,
  precio_item_final DECIMAL(19,2) NOT NULL,
  tiene_seguro BIT(1) NOT NULL DEFAULT b'0',
  monto_seguro DECIMAL(19,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
  CONSTRAINT fk_item_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
  CONSTRAINT fk_item_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

CREATE TABLE pagos (
  id CHAR(36) PRIMARY KEY,
  pedido_id CHAR(36) NOT NULL UNIQUE,
  estado_pago VARCHAR(40) NOT NULL,
  metodo VARCHAR(60) NOT NULL,
  monto DECIMAL(19,2) NOT NULL,
  psp_referencia VARCHAR(150) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_pago_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
);

CREATE TABLE psp_webhooks (
  id CHAR(36) PRIMARY KEY,
  id_evento_psp VARCHAR(120) NOT NULL UNIQUE,
  tipo_evento VARCHAR(80) NOT NULL,
  payload_json JSON NULL,
  recibido_en DATETIME(6) NOT NULL,
  procesado_ok BIT(1) NOT NULL DEFAULT b'0',
  created_at DATETIME(6) NOT NULL
);

CREATE TABLE splits_pedido_item (
  id CHAR(36) PRIMARY KEY,
  pedido_item_id CHAR(36) NOT NULL UNIQUE,
  monto_proveedor DECIMAL(19,2) NOT NULL,
  monto_revendedor DECIMAL(19,2) NOT NULL,
  monto_md DECIMAL(19,2) NOT NULL,
  monto_envio DECIMAL(19,2) NOT NULL,
  monto_seguro DECIMAL(19,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_split_item FOREIGN KEY (pedido_item_id) REFERENCES pedido_items(id)
);
