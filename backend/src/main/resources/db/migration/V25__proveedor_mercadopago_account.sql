CREATE TABLE proveedor_mercadopago_accounts (
  id CHAR(36) PRIMARY KEY,
  proveedor_id CHAR(36) NOT NULL,
  mercado_pago_user_id VARCHAR(80) NULL,
  public_key VARCHAR(150) NULL,
  access_token VARCHAR(255) NULL,
  refresh_token VARCHAR(255) NULL,
  token_expires_at DATETIME(6) NULL,
  live_mode BIT(1) NOT NULL DEFAULT b'0',
  scope VARCHAR(120) NULL,
  linked_at DATETIME(6) NULL,
  last_refresh_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_proveedor_mercadopago_prov UNIQUE (proveedor_id),
  CONSTRAINT uq_proveedor_mercadopago_user UNIQUE (mercado_pago_user_id),
  CONSTRAINT fk_proveedor_mercadopago_prov FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);
