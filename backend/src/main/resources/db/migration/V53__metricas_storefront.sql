CREATE TABLE storefront_eventos (
    id CHAR(36) NOT NULL,
    revendedor_id CHAR(36) NOT NULL,
    tipo_evento VARCHAR(30) NOT NULL,
    visitante_hash CHAR(64) NOT NULL,
    producto_id CHAR(36) NULL,
    producto_nombre VARCHAR(180) NULL,
    rubro_nombre VARCHAR(180) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_storefront_eventos_tienda_fecha (revendedor_id, created_at),
    KEY idx_storefront_eventos_tienda_tipo_fecha (revendedor_id, tipo_evento, created_at),
    KEY idx_storefront_eventos_visitante (revendedor_id, visitante_hash, created_at),
    CONSTRAINT fk_storefront_eventos_revendedor
        FOREIGN KEY (revendedor_id) REFERENCES revendedores(id)
);
