-- V43-V45 ya existen en produccion. Se usa una version nueva e idempotente para no
-- introducir archivos con checksums distintos para versiones ya registradas por Flyway.

CREATE TABLE IF NOT EXISTS revendedor_productos (
    id CHAR(36) NOT NULL,
    revendedor_id CHAR(36) NOT NULL,
    categoria_producto_id CHAR(36) NULL,
    sku VARCHAR(80) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion TEXT NULL,
    precio_venta DECIMAL(19,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'ARS',
    stock INT NOT NULL DEFAULT 0,
    categoria_paquete VARCHAR(30) NOT NULL,
    activo BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    es_promocion TINYINT(1) NOT NULL DEFAULT 0,
    es_novedad TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_revendedor_productos_revendedor_sku (revendedor_id, sku),
    CONSTRAINT fk_revendedor_productos_revendedor
        FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
    CONSTRAINT fk_revendedor_productos_categoria
        FOREIGN KEY (categoria_producto_id) REFERENCES categorias_producto(id)
);

CREATE TABLE IF NOT EXISTS revendedor_producto_media (
    id CHAR(36) NOT NULL,
    revendedor_producto_id CHAR(36) NOT NULL,
    tipo_media VARCHAR(20) NOT NULL,
    url VARCHAR(500) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    es_principal BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_revendedor_producto_media_producto (revendedor_producto_id),
    CONSTRAINT fk_revendedor_producto_media_producto
        FOREIGN KEY (revendedor_producto_id) REFERENCES revendedor_productos(id)
        ON DELETE CASCADE
);

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'revendedor_storefront'
      AND column_name = 'sincronizacion_pausada'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE revendedor_storefront ADD COLUMN sincronizacion_pausada BIT(1) NOT NULL DEFAULT b''0''',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'revendedor_storefront'
      AND column_name = 'sincronizacion_pausada_por'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE revendedor_storefront ADD COLUMN sincronizacion_pausada_por VARCHAR(180) NULL',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'revendedor_storefront'
      AND column_name = 'sincronizacion_pausada_at'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE revendedor_storefront ADD COLUMN sincronizacion_pausada_at DATETIME(6) NULL',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pedido_items'
      AND column_name = 'revendedor_producto_id'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE pedido_items ADD COLUMN revendedor_producto_id CHAR(36) NULL',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

ALTER TABLE pedido_items
    MODIFY COLUMN producto_id CHAR(36) NULL,
    MODIFY COLUMN proveedor_id CHAR(36) NULL;

SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE constraint_schema = DATABASE()
      AND table_name = 'pedido_items'
      AND column_name = 'revendedor_producto_id'
      AND referenced_table_name = 'revendedor_productos'
);
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE pedido_items ADD CONSTRAINT fk_pedido_item_revendedor_producto FOREIGN KEY (revendedor_producto_id) REFERENCES revendedor_productos(id)',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
