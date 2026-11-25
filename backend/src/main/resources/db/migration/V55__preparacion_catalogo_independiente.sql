-- Configuración privada, sin backfill ni modificación del precio/stock legado.
CREATE TABLE catalogo_configuraciones (
    id CHAR(36) NOT NULL PRIMARY KEY,
    revendedor_id CHAR(36) NOT NULL,
    cotizacion_usd DECIMAL(19,6) NULL,
    condiciones_entrega VARCHAR(2000) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_catalogo_config_revendedor UNIQUE (revendedor_id),
    CONSTRAINT fk_catalogo_config_revendedor FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
    CONSTRAINT ck_catalogo_cotizacion CHECK (cotizacion_usd IS NULL OR cotizacion_usd > 0)
);
CREATE TABLE catalogo_categorias_envio (
    id CHAR(36) NOT NULL PRIMARY KEY,
    revendedor_id CHAR(36) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    costo_ars DECIMAL(19,2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_catalogo_envio_nombre UNIQUE (revendedor_id, nombre),
    CONSTRAINT fk_catalogo_envio_revendedor FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
    CONSTRAINT ck_catalogo_envio_costo CHECK (costo_ars >= 0)
);
CREATE TABLE catalogo_productos_configuracion (
    id CHAR(36) NOT NULL PRIMARY KEY,
    producto_id CHAR(36) NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    precio_original DECIMAL(19,2) NOT NULL,
    incluye_envio BIT(1) NOT NULL,
    categoria_envio_id CHAR(36) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_catalogo_producto UNIQUE (producto_id),
    CONSTRAINT fk_catalogo_producto FOREIGN KEY (producto_id) REFERENCES revendedor_productos(id),
    CONSTRAINT fk_catalogo_producto_envio FOREIGN KEY (categoria_envio_id) REFERENCES catalogo_categorias_envio(id),
    CONSTRAINT ck_catalogo_producto_moneda CHECK (moneda IN ('ARS', 'USD')),
    CONSTRAINT ck_catalogo_producto_precio CHECK (precio_original > 0),
    CONSTRAINT ck_catalogo_producto_envio CHECK (incluye_envio = b'1' OR categoria_envio_id IS NOT NULL)
);
ALTER TABLE asesores_venta ADD COLUMN whatsapp_comercial VARCHAR(16) NULL;
