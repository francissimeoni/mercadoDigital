ALTER TABLE proveedores
  ADD COLUMN usuario_id CHAR(36) NULL,
  ADD CONSTRAINT fk_proveedor_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  ADD CONSTRAINT uq_proveedor_usuario UNIQUE (usuario_id);
