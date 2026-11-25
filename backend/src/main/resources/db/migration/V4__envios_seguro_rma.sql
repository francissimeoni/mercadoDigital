CREATE TABLE transportistas (
  id CHAR(36) PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL UNIQUE,
  activo BIT(1) NOT NULL DEFAULT b'1'
);

CREATE TABLE tarifas_transportista (
  id CHAR(36) PRIMARY KEY,
  transportista_id CHAR(36) NOT NULL,
  categoria_paquete VARCHAR(30) NOT NULL,
  zona VARCHAR(60) NOT NULL,
  precio DECIMAL(19,2) NOT NULL,
  demora_estimada_dias INT NOT NULL,
  CONSTRAINT uq_tarifa_transportista UNIQUE (transportista_id, categoria_paquete, zona),
  CONSTRAINT fk_tarifa_transportista FOREIGN KEY (transportista_id) REFERENCES transportistas(id)
);

CREATE TABLE envios (
  id CHAR(36) PRIMARY KEY,
  pedido_id CHAR(36) NOT NULL UNIQUE,
  transportista_id CHAR(36) NULL,
  categoria_paquete VARCHAR(30) NULL,
  tracking VARCHAR(120) NULL,
  estado_envio VARCHAR(40) NOT NULL,
  direccion_envio VARCHAR(255) NOT NULL,
  costo_envio DECIMAL(19,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_envio_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
  CONSTRAINT fk_envio_transportista FOREIGN KEY (transportista_id) REFERENCES transportistas(id)
);

CREATE TABLE rmas (
  id CHAR(36) PRIMARY KEY,
  pedido_item_id CHAR(36) NOT NULL,
  estado_rma VARCHAR(40) NOT NULL,
  motivo VARCHAR(255) NOT NULL,
  tiene_seguro BIT(1) NOT NULL DEFAULT b'0',
  responsable_probable VARCHAR(60) NULL,
  creado_por_usuario_id CHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_rma_item FOREIGN KEY (pedido_item_id) REFERENCES pedido_items(id),
  CONSTRAINT fk_rma_usuario FOREIGN KEY (creado_por_usuario_id) REFERENCES usuarios(id)
);
