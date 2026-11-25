CREATE TABLE IF NOT EXISTS configuracion_psp (
    id CHAR(36) NOT NULL,
    clave VARCHAR(50) NOT NULL,
    psp_fee_porcentaje DECIMAL(8,4) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_configuracion_psp_clave (clave)
);
