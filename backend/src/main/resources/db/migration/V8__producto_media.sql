CREATE TABLE producto_media (
    id CHAR(36) PRIMARY KEY,
    producto_id CHAR(36) NOT NULL,
    tipo_media VARCHAR(20) NOT NULL,
    url VARCHAR(500) NOT NULL,
    orden INT NOT NULL,
    es_principal BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_producto_media_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE INDEX idx_producto_media_producto ON producto_media (producto_id);
