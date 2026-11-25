CREATE TABLE rma_adjuntos (
    id CHAR(36) PRIMARY KEY,
    rma_id CHAR(36) NOT NULL,
    rma_mensaje_id CHAR(36) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    archivo LONGBLOB NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_rma_adjuntos_rma FOREIGN KEY (rma_id) REFERENCES rmas(id),
    CONSTRAINT fk_rma_adjuntos_mensaje FOREIGN KEY (rma_mensaje_id) REFERENCES rma_mensajes(id)
);

CREATE INDEX idx_rma_adjuntos_rma ON rma_adjuntos (rma_id, created_at DESC);
CREATE INDEX idx_rma_adjuntos_mensaje ON rma_adjuntos (rma_mensaje_id, created_at ASC);
