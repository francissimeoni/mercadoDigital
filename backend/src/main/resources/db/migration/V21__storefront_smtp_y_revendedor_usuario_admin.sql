ALTER TABLE revendedor_storefront
    ADD COLUMN smtp_host VARCHAR(180) NULL AFTER footer_texto,
    ADD COLUMN smtp_port INT NULL AFTER smtp_host,
    ADD COLUMN smtp_username VARCHAR(180) NULL AFTER smtp_port,
    ADD COLUMN smtp_password VARCHAR(255) NULL AFTER smtp_username,
    ADD COLUMN smtp_auth BIT(1) NOT NULL DEFAULT b'1' AFTER smtp_password,
    ADD COLUMN smtp_starttls BIT(1) NOT NULL DEFAULT b'1' AFTER smtp_auth;
