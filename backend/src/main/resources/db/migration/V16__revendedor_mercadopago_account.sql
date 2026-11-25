CREATE TABLE revendedor_mercadopago_accounts (
  id CHAR(36) PRIMARY KEY,
  revendedor_id CHAR(36) NOT NULL,
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
  CONSTRAINT uq_revendedor_mercadopago_rev UNIQUE (revendedor_id),
  CONSTRAINT uq_revendedor_mercadopago_user UNIQUE (mercado_pago_user_id),
  CONSTRAINT fk_revendedor_mercadopago_rev FOREIGN KEY (revendedor_id) REFERENCES revendedores(id)
);
