CREATE TABLE categorias_producto (
  id CHAR(36) PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  slug VARCHAR(140) NOT NULL UNIQUE,
  activa BIT(1) NOT NULL DEFAULT b'1',
  sugerida BIT(1) NOT NULL DEFAULT b'0',
  sugerida_por_proveedor_id CHAR(36) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_categoria_producto_sugerida_por_proveedor
    FOREIGN KEY (sugerida_por_proveedor_id) REFERENCES proveedores(id)
);

ALTER TABLE productos
  ADD COLUMN categoria_producto_id CHAR(36) NULL AFTER categoria_paquete,
  ADD CONSTRAINT fk_productos_categoria_producto
    FOREIGN KEY (categoria_producto_id) REFERENCES categorias_producto(id);

CREATE INDEX idx_productos_categoria_producto_id ON productos(categoria_producto_id);
