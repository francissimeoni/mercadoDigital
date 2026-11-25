CREATE TABLE IF NOT EXISTS configuracion_correo_md (
    id CHAR(36) NOT NULL,
    clave VARCHAR(50) NOT NULL,
    from_email VARCHAR(180) NOT NULL,
    from_name VARCHAR(180) NOT NULL,
    reply_to VARCHAR(180) NULL,
    smtp_host VARCHAR(180) NULL,
    smtp_port INT NULL,
    smtp_username VARCHAR(180) NULL,
    smtp_password VARCHAR(255) NULL,
    smtp_auth TINYINT(1) NOT NULL DEFAULT 1,
    smtp_starttls TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_configuracion_correo_md_clave (clave)
);
