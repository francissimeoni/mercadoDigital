ALTER TABLE revendedor_storefront
  ADD COLUMN tipo_storefront VARCHAR(30) NOT NULL DEFAULT 'REVENDEDOR',
  ADD COLUMN dominio_publico VARCHAR(180) NULL,
  ADD COLUMN proveedor_id CHAR(36) NULL;

UPDATE revendedor_storefront rs
JOIN revendedores r ON r.id = rs.revendedor_id
SET rs.dominio_publico = r.dominio
WHERE rs.dominio_publico IS NULL;

ALTER TABLE revendedor_storefront
  ADD CONSTRAINT uq_storefront_dominio_publico UNIQUE (dominio_publico),
  ADD CONSTRAINT uq_storefront_proveedor UNIQUE (proveedor_id),
  ADD CONSTRAINT fk_storefront_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id);
