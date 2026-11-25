CREATE TABLE revendedor_storefront (
  id CHAR(36) PRIMARY KEY,
  revendedor_id CHAR(36) NOT NULL,
  nombre_comercial VARCHAR(180) NOT NULL,
  logo_url VARCHAR(500) NULL,
  favicon_url VARCHAR(500) NULL,
  color_primario VARCHAR(30) NULL,
  color_secundario VARCHAR(30) NULL,
  hero_titulo VARCHAR(180) NULL,
  hero_subtitulo VARCHAR(255) NULL,
  hero_imagen_url VARCHAR(500) NULL,
  email_comercial VARCHAR(180) NULL,
  telefono_comercial VARCHAR(80) NULL,
  direccion_comercial VARCHAR(255) NULL,
  footer_texto VARCHAR(500) NULL,
  publicada BIT(1) NOT NULL DEFAULT b'0',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_storefront_revendedor UNIQUE (revendedor_id),
  CONSTRAINT fk_storefront_revendedor FOREIGN KEY (revendedor_id) REFERENCES revendedores(id)
);

CREATE TABLE cliente_revendedor (
  id CHAR(36) PRIMARY KEY,
  usuario_id CHAR(36) NOT NULL,
  revendedor_id CHAR(36) NOT NULL,
  fecha_alta DATETIME(6) NOT NULL,
  activo BIT(1) NOT NULL DEFAULT b'1',
  origen_alta VARCHAR(80) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_cliente_revendedor_usuario_rev UNIQUE (usuario_id, revendedor_id),
  CONSTRAINT fk_cliente_revendedor_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  CONSTRAINT fk_cliente_revendedor_revendedor FOREIGN KEY (revendedor_id) REFERENCES revendedores(id)
);

CREATE INDEX idx_cliente_revendedor_revendedor ON cliente_revendedor (revendedor_id);
