-- El alta de catálogo no requiere inventar precio, stock ni paquete del checkout anterior.
ALTER TABLE revendedor_productos
    MODIFY precio_venta DECIMAL(19,2) NULL,
    MODIFY categoria_paquete VARCHAR(30) NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN catalogo_alta_token VARCHAR(36) NULL,
    ADD COLUMN catalogo_alta_hash VARCHAR(64) NULL,
    ADD CONSTRAINT uk_producto_catalogo_alta UNIQUE (revendedor_id, catalogo_alta_token);
