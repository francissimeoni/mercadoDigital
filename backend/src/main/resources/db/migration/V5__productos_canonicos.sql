CREATE TABLE productos_canonicos (
  id CHAR(36) PRIMARY KEY,
  marca VARCHAR(80) NOT NULL,
  modelo VARCHAR(120) NOT NULL,
  nombre_publico VARCHAR(200) NOT NULL,
  atributos_json JSON NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

ALTER TABLE productos
  ADD COLUMN producto_canonico_id CHAR(36) NULL;

ALTER TABLE productos
  ADD CONSTRAINT fk_producto_canonico
  FOREIGN KEY (producto_canonico_id) REFERENCES productos_canonicos(id);

CREATE INDEX idx_productos_canonico_id ON productos(producto_canonico_id);
