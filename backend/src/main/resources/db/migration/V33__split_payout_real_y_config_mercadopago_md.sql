CREATE TABLE IF NOT EXISTS configuracion_mercadopago_md (
    id CHAR(36) NOT NULL,
    clave VARCHAR(50) NOT NULL,
    mercado_pago_user_id VARCHAR(80) NULL,
    public_key VARCHAR(150) NULL,
    access_token VARCHAR(255) NULL,
    refresh_token VARCHAR(255) NULL,
    token_expires_at DATETIME(6) NULL,
    live_mode TINYINT(1) NOT NULL DEFAULT 0,
    scope VARCHAR(120) NULL,
    payout_email VARCHAR(180) NULL,
    linked_at DATETIME(6) NULL,
    last_refresh_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_configuracion_mercadopago_md_clave (clave),
    UNIQUE KEY uk_configuracion_mercadopago_md_user_id (mercado_pago_user_id)
);

CREATE TABLE IF NOT EXISTS split_payout_ejecuciones (
    id CHAR(36) NOT NULL,
    pedido_id CHAR(36) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    payout_id VARCHAR(80) NULL,
    idempotency_key VARCHAR(80) NULL,
    error_ultimo VARCHAR(500) NULL,
    payload_json JSON NULL,
    ultimo_intento_at DATETIME(6) NULL,
    completado_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_split_payout_ejecuciones_pedido (pedido_id)
);
