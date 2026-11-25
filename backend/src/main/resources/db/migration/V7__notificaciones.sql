ALTER TABLE usuarios
    ADD COLUMN password_hash VARCHAR(255) NULL AFTER email;

CREATE TABLE notificaciones (
    id CHAR(36) PRIMARY KEY,
    usuario_id CHAR(36) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    leida BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_notificaciones_usuario_created ON notificaciones (usuario_id, created_at DESC);
