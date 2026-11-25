CREATE TABLE rma_mensajes (
    id CHAR(36) PRIMARY KEY,
    rma_id CHAR(36) NOT NULL,
    autor_tipo VARCHAR(40) NOT NULL,
    autor_nombre VARCHAR(180) NULL,
    mensaje TEXT NOT NULL,
    interno BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_rma_mensajes_rma FOREIGN KEY (rma_id) REFERENCES rmas(id)
);

CREATE INDEX idx_rma_mensajes_rma_created ON rma_mensajes (rma_id, created_at DESC);
