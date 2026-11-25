ALTER TABLE revendedor_storefront
    ADD COLUMN transferencia_habilitada BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN transferencia_alias VARCHAR(180) NULL,
    ADD COLUMN transferencia_titular VARCHAR(180) NULL;

ALTER TABLE pagos
    ADD COLUMN transferencia_token VARCHAR(80) NULL,
    ADD COLUMN transferencia_alias_snapshot VARCHAR(180) NULL,
    ADD COLUMN transferencia_solicitada_at DATETIME(6) NULL,
    ADD COLUMN comprobante_archivo VARCHAR(255) NULL,
    ADD COLUMN comprobante_nombre_original VARCHAR(255) NULL,
    ADD COLUMN comprobante_content_type VARCHAR(120) NULL,
    ADD COLUMN comprobante_cargado_at DATETIME(6) NULL,
    ADD UNIQUE KEY uk_pagos_transferencia_token (transferencia_token);
