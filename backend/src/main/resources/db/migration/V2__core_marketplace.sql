CREATE TABLE proveedores (
  id CHAR(36) PRIMARY KEY,
  nombre VARCHAR(180) NOT NULL,
  activo BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE TABLE revendedores (
  id CHAR(36) PRIMARY KEY,
  nombre VARCHAR(180) NOT NULL,
  dominio VARCHAR(180) NULL,
  activo BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE TABLE asesores_venta (
  id CHAR(36) PRIMARY KEY,
  revendedor_id CHAR(36) NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  foto_url VARCHAR(255) NULL,
  activo BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_asesor_revendedor FOREIGN KEY (revendedor_id) REFERENCES revendedores(id)
);

CREATE TABLE productos (
  id CHAR(36) PRIMARY KEY,
  proveedor_id CHAR(36) NOT NULL,
  sku VARCHAR(80) NOT NULL UNIQUE,
  nombre VARCHAR(180) NOT NULL,
  descripcion TEXT NULL,
  precio_costo DECIMAL(19,2) NOT NULL,
  moneda VARCHAR(10) NOT NULL,
  categoria_paquete VARCHAR(30) NOT NULL,
  activo BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

CREATE TABLE proveedor_stock (
  id CHAR(36) PRIMARY KEY,
  proveedor_id CHAR(36) NOT NULL,
  producto_id CHAR(36) NOT NULL,
  stock INT NOT NULL,
  precio_costo_actual DECIMAL(19,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_stock_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
  CONSTRAINT fk_stock_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE revendedor_markups (
  id CHAR(36) PRIMARY KEY,
  revendedor_id CHAR(36) NOT NULL,
  producto_id CHAR(36) NOT NULL,
  markup_porcentaje DECIMAL(5,2) NOT NULL,
  markup_fijo DECIMAL(19,2) NOT NULL,
  activo BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_markup_revendedor FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
  CONSTRAINT fk_markup_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
  CONSTRAINT uq_markup_revendedor_producto UNIQUE (revendedor_id, producto_id)
);
